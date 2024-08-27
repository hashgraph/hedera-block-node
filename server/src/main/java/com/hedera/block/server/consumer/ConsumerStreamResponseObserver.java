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

package com.hedera.block.server.consumer;

import static com.hedera.block.server.Translator.fromPbj;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.CounterMetrics;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.OneOf;
import com.lmax.disruptor.EventHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.time.InstantSource;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The ConsumerBlockItemObserver class is the primary integration point between the LMAX Disruptor
 * and an instance of a downstream consumer (represented by subscribeStreamResponseObserver provided
 * by Helidon). The ConsumerBlockItemObserver implements the EventHandler interface so the Disruptor
 * can invoke the onEvent() method when a new SubscribeStreamResponse is available.
 */
public class ConsumerStreamResponseObserver
        implements EventHandler<ObjectEvent<SubscribeStreamResponse>> {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final MetricsService metricsService;
    private final StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
            subscribeStreamResponseObserver;
    private final SubscriptionHandler<ObjectEvent<SubscribeStreamResponse>> subscriptionHandler;

    private final long timeoutThresholdMillis;
    private final InstantSource producerLivenessClock;
    private long producerLivenessMillis;

    private final AtomicBoolean isResponsePermitted = new AtomicBoolean(true);
    private final ResponseSender statusResponseSender = new StatusResponseSender();
    private final ResponseSender blockItemResponseSender = new BlockItemResponseSender();

    private static final String PROTOCOL_VIOLATION_MESSAGE =
            "Protocol Violation. %s is OneOf type %s but %s is null.\n%s";

    /**
     * The onCancel handler to execute when the consumer cancels the stream. This handler is
     * protected to facilitate testing.
     */
    protected Runnable onCancel;

    /**
     * The onClose handler to execute when the consumer closes the stream. This handler is protected
     * to facilitate testing.
     */
    protected Runnable onClose;

    /**
     * Constructor for the ConsumerBlockItemObserver class. It is responsible for observing the
     * SubscribeStreamResponse events from the Disruptor and passing them to the downstream consumer
     * via the subscribeStreamResponseObserver.
     *
     * @param context contains the context with metrics and configuration for the application
     * @param producerLivenessClock the clock to use to determine the producer liveness
     * @param subscriptionHandler the subscription handler to use to manage the subscription
     *     lifecycle
     * @param subscribeStreamResponseObserver the observer to use to send responses to the consumer
     */
    public ConsumerStreamResponseObserver(
            @NonNull final BlockNodeContext context,
            @NonNull final InstantSource producerLivenessClock,
            @NonNull
                    final SubscriptionHandler<ObjectEvent<SubscribeStreamResponse>>
                            subscriptionHandler,
            @NonNull
                    final StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
                            subscribeStreamResponseObserver) {

        this.timeoutThresholdMillis =
                context.configuration()
                        .getConfigData(ConsumerConfig.class)
                        .timeoutThresholdMillis();
        this.subscriptionHandler = subscriptionHandler;
        this.metricsService = context.metricsService();

        // The ServerCallStreamObserver can be configured with Runnable handlers to
        // be executed when a downstream consumer closes the connection. The handlers
        // unsubscribe this observer.
        if (subscribeStreamResponseObserver
                instanceof
                ServerCallStreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
                serverCallStreamObserver) {

            onCancel =
                    () -> {
                        // The consumer has cancelled the stream.
                        // Do not allow additional responses to be sent.
                        isResponsePermitted.set(false);
                        subscriptionHandler.unsubscribe(this);
                        LOGGER.log(DEBUG, "Consumer cancelled stream.  Observer unsubscribed.");
                    };
            serverCallStreamObserver.setOnCancelHandler(onCancel);

            onClose =
                    () -> {
                        // The consumer has closed the stream.
                        // Do not allow additional responses to be sent.
                        isResponsePermitted.set(false);
                        subscriptionHandler.unsubscribe(this);
                        LOGGER.log(DEBUG, "Consumer completed stream.  Observer unsubscribed.");
                    };
            serverCallStreamObserver.setOnCloseHandler(onClose);
        }

        this.subscribeStreamResponseObserver = subscribeStreamResponseObserver;
        this.producerLivenessClock = producerLivenessClock;
        this.producerLivenessMillis = producerLivenessClock.millis();
    }

    /**
     * The onEvent method is invoked by the Disruptor when a new SubscribeStreamResponse is
     * available. Before sending the response to the downstream consumer, the method checks the
     * producer liveness and unsubscribes the observer if the producer activity is outside the
     * configured timeout threshold. The method also ensures that the downstream subscriber has not
     * cancelled or closed the stream before sending the response.
     *
     * @param event the ObjectEvent containing the SubscribeStreamResponse
     * @param l the sequence number of the event
     * @param b true if the event is the last in the sequence
     */
    @Override
    public void onEvent(
            @NonNull final ObjectEvent<SubscribeStreamResponse> event,
            final long l,
            final boolean b) {

        // Only send the response if the consumer has not cancelled
        // or closed the stream.
        if (isResponsePermitted.get()) {
            final long currentMillis = producerLivenessClock.millis();
            if (currentMillis - producerLivenessMillis > timeoutThresholdMillis) {
                subscriptionHandler.unsubscribe(this);
                LOGGER.log(
                        DEBUG,
                        "Producer liveness timeout. Unsubscribed ConsumerBlockItemObserver.");
            } else {
                // Refresh the producer liveness and pass the BlockItem to the downstream observer.
                producerLivenessMillis = currentMillis;

                metricsService.getCounter(CounterMetrics.LIVE_BLOCK_ITEMS_CONSUMED).increment();

                final SubscribeStreamResponse subscribeStreamResponse = event.get();
                final ResponseSender responseSender = getResponseSender(subscribeStreamResponse);
                responseSender.send(subscribeStreamResponse);
            }
        }
    }

    @NonNull
    private ResponseSender getResponseSender(
            @NonNull final SubscribeStreamResponse subscribeStreamResponse) {

        final OneOf<SubscribeStreamResponse.ResponseOneOfType> oneOfTypeOneOf =
                subscribeStreamResponse.response();
        return switch (oneOfTypeOneOf.kind()) {
            case STATUS -> statusResponseSender;
            case BLOCK_ITEM -> blockItemResponseSender;
            default -> throw new IllegalArgumentException(
                    "Unknown response type: " + oneOfTypeOneOf.kind());
        };
    }

    private interface ResponseSender {
        void send(@NonNull final SubscribeStreamResponse subscribeStreamResponse);
    }

    private final class BlockItemResponseSender implements ResponseSender {
        private boolean streamStarted = false;

        public void send(@NonNull final SubscribeStreamResponse subscribeStreamResponse) {

            // Only start sending BlockItems after we've reached
            // the beginning of a block.
            final BlockItem blockItem = subscribeStreamResponse.blockItem();
            if (blockItem == null) {
                final String message =
                        PROTOCOL_VIOLATION_MESSAGE.formatted(
                                "SubscribeStreamResponse",
                                "BLOCK_ITEM",
                                "block_item",
                                subscribeStreamResponse);
                LOGGER.log(ERROR, message);
                throw new IllegalArgumentException(message);
            } else {
                if (!streamStarted && blockItem.hasBlockHeader()) {
                    streamStarted = true;
                }

                if (streamStarted) {
                    LOGGER.log(DEBUG, "Sending BlockItem downstream: {0}", blockItem);
                    subscribeStreamResponseObserver.onNext(fromPbj(subscribeStreamResponse));
                }
            }
        }
    }

    private final class StatusResponseSender implements ResponseSender {
        public void send(@NonNull final SubscribeStreamResponse subscribeStreamResponse) {
            LOGGER.log(
                    DEBUG,
                    "Sending SubscribeStreamResponse downstream: {0} ",
                    subscribeStreamResponse);
            subscribeStreamResponseObserver.onNext(fromPbj(subscribeStreamResponse));
        }
    }
}