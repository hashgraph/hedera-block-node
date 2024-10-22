/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.block.server.grpc;

import static com.hedera.block.server.Constants.SERVICE_NAME_BLOCK_ACCESS;
import static com.hedera.block.server.Constants.SINGLE_BLOCK_METHOD_NAME;
import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.Translator.toPbj;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SingleBlocksNotFound;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SingleBlocksRetrieved;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.protoc.BlockService;
import com.hedera.hapi.block.protoc.SingleBlockResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;

/**
 * The BlockAccessService class provides a gRPC service to access blocks.
 *
 * <p>This service provides a unary gRPC method to retrieve a single block by block number.
 */
public class BlockAccessService implements GrpcService {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final ServiceStatus serviceStatus;
    private final BlockReader<Block> blockReader;
    private final MetricsService metricsService;

    /**
     * Constructs a new BlockAccessService instance with the given dependencies.
     *
     * @param serviceStatus used to query the service status
     * @param blockReader used to retrieve blocks
     * @param metricsService used to observe metrics
     */
    @Inject
    public BlockAccessService(
            @NonNull ServiceStatus serviceStatus,
            @NonNull BlockReader<Block> blockReader,
            @NonNull MetricsService metricsService) {
        this.serviceStatus = serviceStatus;
        this.blockReader = blockReader;
        this.metricsService = metricsService;
    }

    @Override
    public Descriptors.FileDescriptor proto() {
        return BlockService.getDescriptor();
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME_BLOCK_ACCESS;
    }

    @Override
    public void update(Routing routing) {
        routing.unary(SINGLE_BLOCK_METHOD_NAME, this::protocSingleBlock);
    }

    void protocSingleBlock(
            @NonNull final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest,
            @NonNull final StreamObserver<SingleBlockResponse> singleBlockResponseStreamObserver) {
        LOGGER.log(DEBUG, "Executing Unary singleBlock gRPC method");

        try {
            final SingleBlockRequest pbjSingleBlockRequest =
                    toPbj(SingleBlockRequest.PROTOBUF, singleBlockRequest.toByteArray());
            singleBlock(pbjSingleBlockRequest, singleBlockResponseStreamObserver);
        } catch (ParseException e) {
            LOGGER.log(ERROR, "Error parsing protoc SingleBlockRequest: {0}", singleBlockRequest);
            singleBlockResponseStreamObserver.onNext(buildSingleBlockNotAvailableResponse());
        }
    }

    private void singleBlock(
            @NonNull final SingleBlockRequest singleBlockRequest,
            @NonNull
                    final StreamObserver<com.hedera.hapi.block.protoc.SingleBlockResponse>
                            singleBlockResponseStreamObserver) {

        LOGGER.log(DEBUG, "Executing Unary singleBlock gRPC method");

        if (serviceStatus.isRunning()) {
            final long blockNumber = singleBlockRequest.blockNumber();
            try {
                final Optional<Block> blockOpt = blockReader.read(blockNumber);
                if (blockOpt.isPresent()) {
                    LOGGER.log(DEBUG, "Successfully returning block number: {0}", blockNumber);
                    singleBlockResponseStreamObserver.onNext(
                            fromPbjSingleBlockSuccessResponse(blockOpt.get()));

                    metricsService.get(SingleBlocksRetrieved).increment();
                } else {
                    LOGGER.log(DEBUG, "Block number {0} not found", blockNumber);
                    singleBlockResponseStreamObserver.onNext(buildSingleBlockNotFoundResponse());
                    metricsService.get(SingleBlocksNotFound).increment();
                }
            } catch (IOException e) {
                LOGGER.log(ERROR, "Error reading block number: {0}", blockNumber);
                singleBlockResponseStreamObserver.onNext(buildSingleBlockNotAvailableResponse());
            } catch (ParseException e) {
                LOGGER.log(ERROR, "Error parsing block number: {0}", blockNumber);
                singleBlockResponseStreamObserver.onNext(buildSingleBlockNotAvailableResponse());
            }
        } else {
            LOGGER.log(ERROR, "Unary singleBlock gRPC method is not currently running");
            singleBlockResponseStreamObserver.onNext(buildSingleBlockNotAvailableResponse());
        }

        // Send the response
        singleBlockResponseStreamObserver.onCompleted();
    }

    @NonNull
    static com.hedera.hapi.block.protoc.SingleBlockResponse buildSingleBlockNotAvailableResponse() {
        final com.hedera.hapi.block.SingleBlockResponse response =
                com.hedera.hapi.block.SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();

        return fromPbj(response);
    }

    @NonNull
    static com.hedera.hapi.block.protoc.SingleBlockResponse buildSingleBlockNotFoundResponse()
            throws InvalidProtocolBufferException {
        final com.hedera.hapi.block.SingleBlockResponse response =
                com.hedera.hapi.block.SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_FOUND)
                        .build();

        return fromPbj(response);
    }

    @NonNull
    static com.hedera.hapi.block.protoc.SingleBlockResponse fromPbjSingleBlockSuccessResponse(
            @NonNull final Block block) {
        final com.hedera.hapi.block.SingleBlockResponse singleBlockResponse =
                com.hedera.hapi.block.SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                        .block(block)
                        .build();

        return fromPbj(singleBlockResponse);
    }
}
