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

package com.hedera.block.server;

import static com.hedera.block.server.Constants.CLIENT_STREAMING_METHOD_NAME;
import static com.hedera.block.server.Constants.SERVER_STREAMING_METHOD_NAME;
import static com.hedera.block.server.Constants.SERVICE_NAME;
import static com.hedera.block.server.Constants.SINGLE_BLOCK_METHOD_NAME;
import static com.hedera.block.server.Translator.toProtocSingleBlockResponse;
import static com.hedera.block.server.Translator.toProtocSubscribeStreamResponse;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerStreamResponseObserver;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.producer.ProducerBlockItemObserver;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.protoc.BlockService;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

/**
 * The BlockStreamService class defines the gRPC service for the block stream service. It provides
 * the implementation for the bidirectional streaming, server streaming, and unary methods defined
 * in the proto file.
 */
public class BlockStreamService implements GrpcService {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> streamMediator;
    private final ServiceStatus serviceStatus;
    private final BlockReader<Block> blockReader;
    private final BlockNodeContext blockNodeContext;

    /**
     * Constructor for the BlockStreamService class. It initializes the BlockStreamService with the
     * given parameters.
     *
     * @param streamMediator the stream mediator to proxy block items from the producer to the
     *     subscribers and manage the subscription lifecycle for subscribers
     * @param blockReader the block reader to fetch blocks from storage for unary singleBlock
     *     service calls
     * @param serviceStatus the service status provides methods to check service availability and to
     *     stop the service and web server in the event of an unrecoverable exception
     */
    BlockStreamService(
            @NonNull
                    final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>>
                            streamMediator,
            @NonNull final BlockReader<Block> blockReader,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.streamMediator = streamMediator;
        this.blockReader = blockReader;
        this.serviceStatus = serviceStatus;
        this.blockNodeContext = blockNodeContext;
    }

    /**
     * Returns the proto descriptor for the BlockStreamService. This descriptor corresponds to the
     * proto file for the BlockStreamService.
     *
     * @return the proto descriptor for the BlockStreamService
     */
    @NonNull
    @Override
    public Descriptors.FileDescriptor proto() {
        return BlockService.getDescriptor();
    }

    /**
     * Returns the service name for the BlockStreamService. This service name corresponds to the
     * service name in the proto file.
     *
     * @return the service name corresponding to the service name in the proto file
     */
    @NonNull
    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * Updates the routing definitions for the BlockStreamService. It establishes the bidirectional
     * streaming method for publishBlockStream, server streaming method for subscribeBlockStream and
     * a unary method for singleBlock.
     *
     * @param routing the routing for the BlockStreamService
     */
    @Override
    public void update(@NonNull final Routing routing) {
        routing.bidi(CLIENT_STREAMING_METHOD_NAME, this::protocPublishBlockStream);
        routing.serverStream(SERVER_STREAMING_METHOD_NAME, this::protocSubscribeBlockStream);
        routing.unary(SINGLE_BLOCK_METHOD_NAME, this::protocSingleBlock);
    }

    StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest> protocPublishBlockStream(
            @NonNull
                    final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
                            publishStreamResponseObserver) {
        LOGGER.log(DEBUG, "Executing bidirectional publishBlockStream gRPC method");

        return new ProducerBlockItemObserver(
                streamMediator, publishStreamResponseObserver, serviceStatus);
    }

    void protocSubscribeBlockStream(
            @NonNull
                    final com.hedera.hapi.block.protoc.SubscribeStreamRequest
                            subscribeStreamRequest,
            @NonNull
                    final StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
                            subscribeStreamResponseObserver) {
        LOGGER.log(DEBUG, "Executing Server Streaming subscribeBlockStream gRPC method");

        // Return a custom StreamObserver to handle streaming blocks from the producer.
        if (serviceStatus.isRunning()) {
            final var streamObserver =
                    new ConsumerStreamResponseObserver(
                            blockNodeContext,
                            Clock.systemDefaultZone(),
                            streamMediator,
                            subscribeStreamResponseObserver);

            streamMediator.subscribe(streamObserver);
        } else {
            LOGGER.log(
                    ERROR,
                    "Server Streaming subscribeBlockStream gRPC Service is not currently running");

            subscribeStreamResponseObserver.onNext(buildSubscribeStreamNotAvailableResponse());
        }
    }

    void protocSingleBlock(
            @NonNull final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest,
            @NonNull
                    final StreamObserver<com.hedera.hapi.block.protoc.SingleBlockResponse>
                            singleBlockResponseStreamObserver) {
        LOGGER.log(DEBUG, "Executing Unary singleBlock gRPC method");

        try {
            final SingleBlockRequest pbjSingleBlockRequest =
                    toPbjSingleBlockRequest(singleBlockRequest);
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
                            toProtocSingleBlockResponse(blockOpt.get()));

                    final MetricsService metricsService = blockNodeContext.metricsService();
                    metricsService.singleBlocksRetrieved.increment();
                } else {
                    LOGGER.log(DEBUG, "Block number {0} not found", blockNumber);
                    singleBlockResponseStreamObserver.onNext(buildSingleBlockNotFoundResponse());
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

    // TODO: Fix this error type once it's been standardized in `hedera-protobufs`
    //  this should not be success
    @NonNull
    static com.hedera.hapi.block.protoc.SubscribeStreamResponse
            buildSubscribeStreamNotAvailableResponse() {
        final SubscribeStreamResponse response =
                SubscribeStreamResponse.newBuilder()
                        .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                        .build();

        return toProtocSubscribeStreamResponse(response);
    }

    @NonNull
    static com.hedera.hapi.block.protoc.SingleBlockResponse buildSingleBlockNotAvailableResponse() {
        final SingleBlockResponse response =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();

        return toProtocSingleBlockResponse(response);
    }

    @NonNull
    static com.hedera.hapi.block.protoc.SingleBlockResponse buildSingleBlockNotFoundResponse()
            throws InvalidProtocolBufferException {
        final SingleBlockResponse response =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_FOUND)
                        .build();

        return toProtocSingleBlockResponse(response);
    }

    @NonNull
    private static com.hedera.hapi.block.SingleBlockRequest toPbjSingleBlockRequest(
            @NonNull final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest)
            throws ParseException {

        return SingleBlockRequest.PROTOBUF.parse(Bytes.wrap(singleBlockRequest.toByteArray()));
    }
}
