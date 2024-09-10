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

import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.Translator.toPbj;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItemsReceived;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SuccessfulPubStreamRespSent;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.LivenessCalculator;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.Publisher;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.time.InstantSource;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The ProducerBlockStreamObserver class plugs into Helidon's server-initiated bidirectional gRPC
 * service implementation. Helidon calls methods on this class as networking events occur with the
 * connection to the upstream producer (e.g. block items streamed from the Consensus Node to the
 * server).
 */
public class ProducerBlockItemObserver
        implements StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest>,
                BlockNodeEventHandler<ObjectEvent<PublishStreamResponse>> {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
            publishStreamResponseObserver;
    private final SubscriptionHandler<PublishStreamResponse> subscriptionHandler;
    private final Publisher<BlockItem> publisher;
    private final ServiceStatus serviceStatus;
    private final MetricsService metricsService;

    private final AtomicBoolean isResponsePermitted = new AtomicBoolean(true);

    private final LivenessCalculator livenessCalculator;

    /**
     * The onCancel handler to execute when the producer cancels the stream. This handler is
     * protected to facilitate testing.
     */
    protected Runnable onCancel;

    /**
     * The onClose handler to execute when the producer closes the stream. This handler is protected
     * to facilitate testing.
     */
    protected Runnable onClose;

    /**
     * Constructor for the ProducerBlockStreamObserver class. It is responsible for calling the
     * mediator with blocks as they arrive from the upstream producer. It also sends responses back
     * to the upstream producer via the responseStreamObserver.
     *
     * @param publisher the block item publisher to used to pass block items to consumers as they
     *     arrive from the upstream producer.
     * @param publishStreamResponseObserver the response stream observer to send responses back to
     *     the upstream producer for each block item processed.
     * @param blockNodeContext the block node context used to access context objects for the Block
     *     Node (e.g. - the metrics service).
     */
    public ProducerBlockItemObserver(
            @NonNull final InstantSource producerLivenessClock,
            @NonNull final Publisher<BlockItem> publisher,
            @NonNull final SubscriptionHandler<PublishStreamResponse> subscriptionHandler,
            @NonNull
                    final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
                            publishStreamResponseObserver,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {

        this.livenessCalculator =
                new LivenessCalculator(
                        producerLivenessClock,
                        blockNodeContext
                                .configuration()
                                .getConfigData(ConsumerConfig.class)
                                .timeoutThresholdMillis());

        this.publisher = publisher;
        this.publishStreamResponseObserver = publishStreamResponseObserver;
        this.subscriptionHandler = subscriptionHandler;
        this.metricsService = blockNodeContext.metricsService();
        this.serviceStatus = serviceStatus;

        if (publishStreamResponseObserver
                instanceof
                ServerCallStreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
                serverCallStreamObserver) {

            onCancel =
                    () -> {
                        // The producer has cancelled the stream.
                        // Do not allow additional responses to be sent.
                        isResponsePermitted.set(false);
                        subscriptionHandler.unsubscribe(this);
                        LOGGER.log(DEBUG, "Producer cancelled the stream. Observer unsubscribed.");
                    };
            serverCallStreamObserver.setOnCancelHandler(onCancel);

            onClose =
                    () -> {
                        // The producer has closed the stream.
                        // Do not allow additional responses to be sent.
                        isResponsePermitted.set(false);
                        subscriptionHandler.unsubscribe(this);
                        LOGGER.log(DEBUG, "Producer completed the stream. Observer unsubscribed.");
                    };
            serverCallStreamObserver.setOnCloseHandler(onClose);
        }
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

        try {
            LOGGER.log(DEBUG, "Received PublishStreamRequest from producer");
            final BlockItem blockItem =
                    toPbj(BlockItem.PROTOBUF, publishStreamRequest.getBlockItem().toByteArray());
            LOGGER.log(DEBUG, "Received block item: " + blockItem);

            metricsService.get(LiveBlockItemsReceived).increment();

            // Publish the block to all the subscribers unless
            // there's an issue with the StreamMediator.
            if (serviceStatus.isRunning()) {
                // Refresh the producer liveness
                livenessCalculator.refresh();

                // Publish the block to the mediator
                publisher.publish(blockItem);

            } else {
                LOGGER.log(ERROR, getClass().getName() + " is not accepting BlockItems");

                // Close the upstream connection to the producer(s)
                final var errorResponse = buildErrorStreamResponse();
                publishStreamResponseObserver.onNext(errorResponse);
                LOGGER.log(ERROR, "Error PublishStreamResponse sent to upstream producer");
            }
        } catch (ParseException e) {
            final var errorResponse = buildErrorStreamResponse();
            publishStreamResponseObserver.onNext(errorResponse);
            LOGGER.log(
                    ERROR,
                    "Error parsing inbound block item from a producer: "
                            + publishStreamRequest.getBlockItem(),
                    e);
            serviceStatus.stopWebServer();
        }
    }

    @Override
    public void onEvent(
            ObjectEvent<PublishStreamResponse> event, long sequence, boolean endOfBatch) {

        if (isResponsePermitted.get()) {
            if (isTimeoutExpired()) {
                subscriptionHandler.unsubscribe(this);
                LOGGER.log(
                        DEBUG,
                        "Producer liveness timeout. Unsubscribed ProducerBlockItemObserver.");
            } else {
                LOGGER.log(DEBUG, "Publishing response to upstream producer: " + this);
                publishStreamResponseObserver.onNext(fromPbj(event.get()));
                metricsService.get(SuccessfulPubStreamRespSent).increment();
            }
        }
    }

    @NonNull
    private static com.hedera.hapi.block.protoc.PublishStreamResponse buildErrorStreamResponse() {
        // TODO: Replace this with a real error enum.
        final EndOfStream endOfStream =
                EndOfStream.newBuilder()
                        .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                        .build();
        return fromPbj(PublishStreamResponse.newBuilder().status(endOfStream).build());
    }

    /**
     * Helidon triggers this method when an error occurs on the bidirectional stream to the upstream
     * producer.
     *
     * @param t the error occurred on the stream
     */
    @Override
    public void onError(@NonNull final Throwable t) {
        LOGGER.log(ERROR, "onError method invoked with an exception: ", t);
        publishStreamResponseObserver.onError(t);
    }

    /**
     * Helidon triggers this method when the bidirectional stream to the upstream producer is
     * completed. Unsubscribe all the observers from the mediator.
     */
    @Override
    public void onCompleted() {
        LOGGER.log(DEBUG, "ProducerBlockStreamObserver completed");
        publishStreamResponseObserver.onCompleted();
    }

    @Override
    public boolean isTimeoutExpired() {
        return livenessCalculator.isTimeoutExpired();
    }
}
