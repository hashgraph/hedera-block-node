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
import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.Translator.toPbj;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SingleBlocksNotFound;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SingleBlocksRetrieved;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerStreamResponseObserver;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifiable;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.notifier.NotifierBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.producer.ProducerBlockItemObserver;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.validator.StreamValidatorBuilder;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.protoc.BlockService;
import com.hedera.hapi.block.stream.Block;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/**
 * The BlockStreamService class defines the gRPC service for the block stream service. It provides
 * the implementation for the bidirectional streaming, server streaming, and unary methods defined
 * in the proto file.
 */
public class BlockStreamService implements GrpcService, Notifiable {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final LiveStreamMediator streamMediator;
    private final ServiceStatus serviceStatus;
    private final BlockReader<Block> blockReader;

    private final BlockNodeContext blockNodeContext;
    private final MetricsService metricsService;

    private final NotifierBuilder notifierBuilder;
    private Notifier notifier;
    private final StreamValidatorBuilder streamValidatorBuilder;

    private AtomicBoolean isInitPhase = new AtomicBoolean(true);

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
    @Inject
    BlockStreamService(
            @NonNull final LiveStreamMediator streamMediator,
            @NonNull final BlockReader<Block> blockReader,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final StreamValidatorBuilder streamValidatorBuilder,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.streamMediator = streamMediator;
        this.blockReader = blockReader;
        this.serviceStatus = serviceStatus;
        this.blockNodeContext = blockNodeContext;
        this.metricsService = blockNodeContext.metricsService();

        this.notifierBuilder =
                NotifierBuilder.newBuilder(streamMediator, blockNodeContext, serviceStatus);
        this.streamValidatorBuilder = streamValidatorBuilder;
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

        if (isInitPhase.get()) {
            notifier = notifierBuilder.blockStreamService(this).build();

            final var streamValidator =
                    streamValidatorBuilder
                            .subscriptionHandler(streamMediator)
                            .notifier(notifier)
                            .build();
            streamMediator.subscribe(streamValidator);

            isInitPhase.set(false);
        }

        // Unsubscribe any expired notifiers
        notifier.unsubscribeAllExpired();

        final var producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        Clock.systemDefaultZone(),
                        streamMediator,
                        notifier,
                        publishStreamResponseObserver,
                        blockNodeContext,
                        serviceStatus);

        // Register the producer observer with the notifier to publish responses back to the
        // producer
        notifier.subscribe(producerBlockItemObserver);

        return producerBlockItemObserver;
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
            // Unsubscribe any expired notifiers
            streamMediator.unsubscribeAllExpired();

            final var consumerStreamResponseObserver =
                    new ConsumerStreamResponseObserver(
                            Clock.systemDefaultZone(),
                            streamMediator,
                            subscribeStreamResponseObserver,
                            blockNodeContext);

            streamMediator.subscribe(consumerStreamResponseObserver);
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

    @Override
    public void notifyUnrecoverableError() {
        // prevent additional producer/consumer subscriptions
        // and single block queries. Prepare for shutdown.
        serviceStatus.stopRunning(getClass().getName());
        LOGGER.log(ERROR, "An unrecoverable error occurred. Preparing to stop the service.");
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

        return fromPbj(response);
    }

    @NonNull
    static com.hedera.hapi.block.protoc.SingleBlockResponse buildSingleBlockNotAvailableResponse() {
        final SingleBlockResponse response =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();

        return fromPbj(response);
    }

    @NonNull
    static com.hedera.hapi.block.protoc.SingleBlockResponse buildSingleBlockNotFoundResponse()
            throws InvalidProtocolBufferException {
        final SingleBlockResponse response =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_FOUND)
                        .build();

        return fromPbj(response);
    }

    @NonNull
    static com.hedera.hapi.block.protoc.SingleBlockResponse fromPbjSingleBlockSuccessResponse(
            @NonNull final Block block) {
        final SingleBlockResponse singleBlockResponse =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                        .block(block)
                        .build();

        return fromPbj(singleBlockResponse);
    }
}
