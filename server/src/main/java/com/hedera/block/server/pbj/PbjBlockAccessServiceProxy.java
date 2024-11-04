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

package com.hedera.block.server.pbj;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SingleBlocksNotFound;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SingleBlocksRetrieved;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.stream.Block;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.grpc.Pipelines;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Flow;
import javax.inject.Inject;

/**
 * PbjBlockAccessServiceProxy is the runtime binding between the PBJ Helidon Plugin and the
 * Block Node. The Helidon Plugin routes inbound requests to this class based on the methods
 * and service names in PbjBlockAccessService. Service implementations are instantiated via
 * the open method thereby bridging the client requests into the Block Node application.
 */
public class PbjBlockAccessServiceProxy implements PbjBlockAccessService {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final ServiceStatus serviceStatus;
    private final BlockReader<Block> blockReader;
    private final MetricsService metricsService;

    /**
     * Creates a new PbjBlockAccessServiceProxy instance.
     *
     * @param serviceStatus the service status
     * @param blockReader the block reader
     * @param blockNodeContext the block node context
     */
    @Inject
    public PbjBlockAccessServiceProxy(
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final BlockReader<Block> blockReader,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.serviceStatus = serviceStatus;
        this.blockReader = blockReader;
        this.metricsService = blockNodeContext.metricsService();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Pipeline<? super Bytes> open(
            final @NonNull Method method,
            final @NonNull RequestOptions options,
            final @NonNull Flow.Subscriber<? super Bytes> replies) {

        try {
            final var m = (BlockAccessMethod) method;
            return switch (m) {
                case singleBlock -> Pipelines.<SingleBlockRequest, SingleBlockResponse>unary()
                        .mapRequest(bytes -> parseSingleBlockRequest(bytes, options))
                        .method(this::singleBlock)
                        .mapResponse(reply -> createSingleBlockResponse(reply, options))
                        .respondTo(replies)
                        .build();
            };
        } catch (Exception e) {
            replies.onError(e);
            return Pipelines.noop();
        }
    }

    SingleBlockResponse singleBlock(SingleBlockRequest singleBlockRequest) {

        LOGGER.log(DEBUG, "Executing Unary singleBlock gRPC method");

        if (serviceStatus.isRunning()) {
            final long blockNumber = singleBlockRequest.blockNumber();
            try {
                final Optional<Block> blockOpt = blockReader.read(blockNumber);
                if (blockOpt.isPresent()) {
                    LOGGER.log(DEBUG, "Successfully returning block number: {0}", blockNumber);
                    metricsService.get(SingleBlocksRetrieved).increment();

                    return SingleBlockResponse.newBuilder()
                            .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                            .block(blockOpt.get())
                            .build();
                } else {
                    LOGGER.log(DEBUG, "Block number {0} not found", blockNumber);
                    metricsService.get(SingleBlocksNotFound).increment();

                    return SingleBlockResponse.newBuilder()
                            .status(SingleBlockResponseCode.READ_BLOCK_NOT_FOUND)
                            .build();
                }
            } catch (IOException e) {
                LOGGER.log(ERROR, "Error reading block number: {0}", blockNumber);

                return SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();
            } catch (ParseException e) {
                LOGGER.log(ERROR, "Error parsing block number: {0}", blockNumber);

                return SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();
            }
        } else {
            LOGGER.log(ERROR, "Unary singleBlock gRPC method is not currently running");

            return SingleBlockResponse.newBuilder()
                    .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                    .build();
        }
    }

    @NonNull
    private SingleBlockRequest parseSingleBlockRequest(
            @NonNull final Bytes message, @NonNull final RequestOptions options) throws ParseException {
        // Copying bytes to avoid using references passed from Helidon
        return SingleBlockRequest.PROTOBUF.parse(Bytes.wrap(message.toByteArray()));
    }

    @NonNull
    private Bytes createSingleBlockResponse(
            @NonNull final SingleBlockResponse reply, @NonNull final RequestOptions options) {
        return SingleBlockResponse.PROTOBUF.toBytes(reply);
    }
}
