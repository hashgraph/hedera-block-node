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

import com.google.protobuf.Descriptors;
import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.consumer.LiveStreamObserver;
import com.hedera.block.server.consumer.LiveStreamObserverImpl;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.producer.ProducerBlockStreamObserver;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;

import java.time.Clock;

import static com.hedera.block.server.Constants.*;

/**
 * This class implements the GrpcService interface and provides the functionality for the BlockStreamService.
 * It sets up the bidirectional streaming methods for the service and handles the routing for these methods.
 * It also initializes the StreamMediator, BlockStorage, and BlockCache upon creation.
 *
 * <p>The class provides two main methods, streamSink and streamSource, which handle the client and server streaming
 * respectively. These methods return custom StreamObservers which are used to observe and respond to the streams.
 */
public class BlockStreamService implements GrpcService {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final long timeoutThresholdMillis;
    private final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator;

    /**
     * Constructor for the BlockStreamService class.
     *
     * @param timeoutThresholdMillis the timeout threshold in milliseconds
     * @param streamMediator the stream mediator
     */
    public BlockStreamService(final long timeoutThresholdMillis,
                              final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator) {

        this.timeoutThresholdMillis = timeoutThresholdMillis;
        this.streamMediator = streamMediator;
    }

    /**
     * Returns the FileDescriptor for the BlockStreamServiceGrpcProto.
     *
     * @return the FileDescriptor for the BlockStreamServiceGrpcProto
     */
    @Override
    public Descriptors.FileDescriptor proto() {
        return BlockStreamServiceGrpcProto.getDescriptor();
    }

    /**
     * Returns the service name for the BlockStreamService.  This service name corresponds to the service name in
     * the proto file.
     *
     * @return the service name corresponding to the service name in the proto file
     */
    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * Updates the routing for the BlockStreamService.  It sets up the bidirectional streaming methods for the service.
     *
     * @param routing the routing for the BlockStreamService
     */
    @Override
    public void update(final Routing routing) {
        routing.bidi(CLIENT_STREAMING_METHOD_NAME, this::streamSink);
        routing.bidi(SERVER_STREAMING_METHOD_NAME, this::streamSource);
    }

    /**
     * The streamSink method is called by Helidon each time a producer initiates a bidirectional stream.
     *
     * @param responseStreamObserver Helidon provides a StreamObserver to handle responses back to the producer.
     *
     * @return a custom StreamObserver to handle streaming blocks from the producer to all subscribed consumer
     *     via the streamMediator as well as sending responses back to the producer.
     */
    private StreamObserver<BlockStreamServiceGrpcProto.Block> streamSink(
            final StreamObserver<BlockStreamServiceGrpcProto.BlockResponse> responseStreamObserver) {
        LOGGER.log(System.Logger.Level.DEBUG, "Executing bidirectional streamSink method");

        return new ProducerBlockStreamObserver(streamMediator, responseStreamObserver);
    }

    /**
     * The streamSource method is called by Helidon each time a consumer initiates a bidirectional stream.
     *
     * @param responseStreamObserver Helidon provides a StreamObserver to handle responses from the consumer
     *     back to the server.
     *
     * @return a custom StreamObserver to handle streaming blocks from the producer to the consumer as well
     *     as handling responses from the consumer.
     */
    private StreamObserver<BlockStreamServiceGrpcProto.BlockResponse> streamSource(final StreamObserver<BlockStreamServiceGrpcProto.Block> responseStreamObserver) {
        LOGGER.log(System.Logger.Level.DEBUG, "Executing bidirectional streamSource method");

        // Return a custom StreamObserver to handle streaming blocks from the producer.
        final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamObserver = new LiveStreamObserverImpl(
                timeoutThresholdMillis,
                Clock.systemDefaultZone(),
                Clock.systemDefaultZone(),
                streamMediator,
                responseStreamObserver);

        // Subscribe the observer to the mediator
        streamMediator.subscribe(streamObserver);

        return streamObserver;
    }
}


