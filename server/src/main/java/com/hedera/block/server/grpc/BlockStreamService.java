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

import static com.hedera.block.server.Constants.CLIENT_STREAMING_METHOD_NAME;
import static com.hedera.block.server.Constants.SERVER_STREAMING_METHOD_NAME;
import static com.hedera.block.server.Constants.SERVICE_NAME_BLOCK_STREAM;
import static com.hedera.block.server.Translator.fromPbj;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.google.protobuf.Descriptors;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerStreamResponseObserver;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.producer.ProducerBlockItemObserver;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.protoc.BlockService;
import com.hedera.hapi.block.stream.Block;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;
import java.time.Clock;
import javax.inject.Inject;

/**
 * The BlockStreamService class defines the gRPC service for the block stream service. It provides
 * the implementation for the bidirectional streaming, server streaming as defined in the proto file.
 */
public class BlockStreamService implements GrpcService {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final LiveStreamMediator streamMediator;
    private final ServiceStatus serviceStatus;
    private final BlockReader<Block> blockReader;

    private final BlockNodeContext blockNodeContext;
    private final MetricsService metricsService;

    private final Notifier notifier;

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
            @NonNull
                    final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>>
                            streamPersistenceHandler,
            @NonNull final Notifier notifier,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.blockReader = blockReader;
        this.serviceStatus = serviceStatus;
        this.notifier = notifier;
        this.blockNodeContext = blockNodeContext;
        this.metricsService = blockNodeContext.metricsService();

        streamMediator.subscribe(streamPersistenceHandler);
        this.streamMediator = streamMediator;
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
        return SERVICE_NAME_BLOCK_STREAM;
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
    }

    StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest> protocPublishBlockStream(
            @NonNull
                    final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
                            publishStreamResponseObserver) {
        LOGGER.log(DEBUG, "Executing bidirectional publishBlockStream gRPC method");

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
}
