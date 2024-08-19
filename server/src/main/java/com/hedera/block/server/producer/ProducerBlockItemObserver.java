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

package com.hedera.block.server.producer;

import static com.hedera.block.server.Translator.toPbjBlockItem;
import static com.hedera.block.server.Translator.toProtocPublishStreamResponse;

import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.mediator.Publisher;
import com.hedera.hapi.block.*;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * The ProducerBlockStreamObserver class plugs into Helidon's server-initiated bidirectional gRPC
 * service implementation. Helidon calls methods on this class as networking events occur with the
 * connection to the upstream producer (e.g. block items streamed from the Consensus Node to the
 * server).
 */
public class ProducerBlockItemObserver
        implements StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
            publishStreamResponseObserver;
    private final Publisher<BlockItem> publisher;
    private final AckBuilder ackBuilder;
    private final ServiceStatus serviceStatus;

    /**
     * Constructor for the ProducerBlockStreamObserver class. It is responsible for calling the
     * mediator with blocks as they arrive from the upstream producer. It also sends responses back
     * to the upstream producer via the responseStreamObserver.
     *
     * @param publisher the block item publisher to used to pass block items to consumers as they
     *     arrive from the upstream producer
     * @param publishStreamResponseObserver the response stream observer to send responses back to
     *     the upstream producer for each block item processed
     * @param ackBuilder the item acknowledgement builder to use when sending responses back to the
     *     upstream producer for each block item processed
     * @param serviceStatus the service status used to determine if the downstream service is
     *     accepting block items. In the event of an unrecoverable exception, it will be used to
     *     stop the web server.
     */
    public ProducerBlockItemObserver(
            @NonNull final Publisher<BlockItem> publisher,
            @NonNull
                    final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
                            publishStreamResponseObserver,
            @NonNull final AckBuilder ackBuilder,
            @NonNull final ServiceStatus serviceStatus) {

        this.publisher = publisher;
        this.publishStreamResponseObserver = publishStreamResponseObserver;
        this.ackBuilder = ackBuilder;
        this.serviceStatus = serviceStatus;
    }

    /**
     * Helidon triggers this method when it receives a new PublishStreamRequest from the upstream
     * producer. The method publish the block item data to all subscribers via the Publisher and
     * sends a response back to the upstream producer.
     *
     * @param publishStreamRequest the PublishStreamRequest received from the upstream producer
     */
    @Override
    public void onNext(
            @NonNull final com.hedera.hapi.block.protoc.PublishStreamRequest publishStreamRequest) {

        @NonNull final BlockItem blockItem = toPbjBlockItem(publishStreamRequest.getBlockItem());

        try {
            // Publish the block to all the subscribers unless
            // there's an issue with the StreamMediator.
            if (serviceStatus.isRunning()) {

                // Publish the block to the mediator
                publisher.publish(blockItem);

                try {
                    // Send a successful response
                    publishStreamResponseObserver.onNext(buildSuccessStreamResponse(blockItem));

                } catch (IOException | NoSuchAlgorithmException e) {
                    @NonNull final var errorResponse = buildErrorStreamResponse();
                    publishStreamResponseObserver.onNext(errorResponse);
                    LOGGER.log(System.Logger.Level.ERROR, "Error calculating hash: ", e);
                }

            } else {
                // Close the upstream connection to the producer(s)
                @NonNull final var errorResponse = buildErrorStreamResponse();
                publishStreamResponseObserver.onNext(errorResponse);
                LOGGER.log(System.Logger.Level.DEBUG, "StreamMediator is not accepting BlockItems");
            }
        } catch (IOException io) {
            @NonNull final var errorResponse = buildErrorStreamResponse();
            publishStreamResponseObserver.onNext(errorResponse);
            LOGGER.log(System.Logger.Level.ERROR, "Exception thrown publishing BlockItem: ", io);

            LOGGER.log(System.Logger.Level.ERROR, "Shutting down the web server");
            serviceStatus.stopWebServer();
        }
    }

    @NonNull
    private com.hedera.hapi.block.protoc.PublishStreamResponse buildSuccessStreamResponse(
            @NonNull final BlockItem blockItem) throws IOException, NoSuchAlgorithmException {
        @NonNull final Acknowledgement ack = ackBuilder.buildAck(blockItem);

        return toProtocPublishStreamResponse(
                PublishStreamResponse.newBuilder().acknowledgement(ack).build());
    }

    @NonNull
    private static com.hedera.hapi.block.protoc.PublishStreamResponse buildErrorStreamResponse() {
        // TODO: Replace this with a real error enum.
        @NonNull
        final EndOfStream endOfStream =
                EndOfStream.newBuilder()
                        .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                        .build();
        return toProtocPublishStreamResponse(
                PublishStreamResponse.newBuilder().status(endOfStream).build());
    }

    /**
     * Helidon triggers this method when an error occurs on the bidirectional stream to the upstream
     * producer.
     *
     * @param t the error occurred on the stream
     */
    @Override
    public void onError(@NonNull final Throwable t) {
        LOGGER.log(System.Logger.Level.ERROR, "onError method invoked with an exception: ", t);
        publishStreamResponseObserver.onError(t);
    }

    /**
     * Helidon triggers this method when the bidirectional stream to the upstream producer is
     * completed. Unsubscribe all the observers from the mediator.
     */
    @Override
    public void onCompleted() {
        LOGGER.log(System.Logger.Level.DEBUG, "ProducerBlockStreamObserver completed");
        publishStreamResponseObserver.onCompleted();
    }
}
