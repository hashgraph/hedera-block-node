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

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.LivenessCalculator;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.OneOf;
import com.hedera.pbj.runtime.grpc.Pipeline;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.UncheckedIOException;
import java.time.InstantSource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The ConsumerStreamResponseObserver class is the primary integration point between the LMAX Disruptor
 * and an instance of a downstream consumer (represented by subscribeStreamResponseObserver provided
 * by Helidon). The ConsumerBlockItemObserver implements the BlockNodeEventHandler interface so the
 * Disruptor can invoke the onEvent() method when a new SubscribeStreamResponse is available.
 */
public class ConsumerStreamResponseObserver
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final MetricsService metricsService;
    private final Pipeline<? super SubscribeStreamResponseUnparsed> subscribeStreamResponseObserver;
    private final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;

    private final AtomicBoolean isResponsePermitted = new AtomicBoolean(true);
    private final ResponseSender statusResponseSender = new StatusResponseSender();
    private final ResponseSender blockItemsResponseSender = new BlockItemsResponseSender();

    private static final String PROTOCOL_VIOLATION_MESSAGE =
            "Protocol Violation. %s is OneOf type %s but %s is null.\n%s";

    private final LivenessCalculator livenessCalculator;

    /**
     * Constructor for the ConsumerBlockItemObserver class. It is responsible for observing the
     * SubscribeStreamResponse events from the Disruptor and passing them to the downstream consumer
     * via the subscribeStreamResponseObserver.
     *
     * @param producerLivenessClock the clock to use to determine the producer liveness
     * @param subscriptionHandler the subscription handler to use to manage the subscription
     *     lifecycle
     * @param subscribeStreamResponseObserver the observer to use to send responses to the consumer
     * @param blockNodeContext contains the context with metrics and configuration for the
     *     application
     */
    public ConsumerStreamResponseObserver(
            @NonNull final InstantSource producerLivenessClock,
            @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
            @NonNull final Pipeline<? super SubscribeStreamResponseUnparsed> subscribeStreamResponseObserver,
            @NonNull final BlockNodeContext blockNodeContext) {

        this.livenessCalculator = new LivenessCalculator(
                producerLivenessClock,
                blockNodeContext
                        .configuration()
                        .getConfigData(ConsumerConfig.class)
                        .timeoutThresholdMillis());

        this.subscriptionHandler = subscriptionHandler;
        this.metricsService = blockNodeContext.metricsService();
        this.subscribeStreamResponseObserver = subscribeStreamResponseObserver;
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
            @NonNull final ObjectEvent<SubscribeStreamResponseUnparsed> event, final long l, final boolean b) {

        // Only send the response if the consumer has not cancelled
        // or closed the stream.
        if (isResponsePermitted.get()) {
            if (isTimeoutExpired()) {
                stopProcessing();

                // Notify the Helidon observer that we've
                // stopped processing the stream
                subscribeStreamResponseObserver.onComplete();
                LOGGER.log(DEBUG, "Producer liveness timeout. Unsubscribed ConsumerBlockItemObserver.");
            } else {
                // Refresh the producer liveness and pass the BlockItem to the downstream observer.
                livenessCalculator.refresh();

                final SubscribeStreamResponseUnparsed subscribeStreamResponse = event.get();
                final ResponseSender responseSender = getResponseSender(subscribeStreamResponse);
                try {
                    responseSender.send(subscribeStreamResponse);
                } catch (IllegalArgumentException e) {
                    // Bubble protocol exceptions up to a higher
                    // layer where we can broadcast a uniform
                    // message to terminate all connections
                    // and shut the server down.
                    throw e;
                } catch (UncheckedIOException e) {
                    // UncheckedIOException at this layer will almost
                    // always be wrapped SocketExceptions from individual
                    // clients disconnecting from the server streaming
                    // service. This should be happening all the time.
                    stopProcessing();
                    LOGGER.log(
                            DEBUG,
                            "UncheckedIOException caught from Pipeline instance. Unsubscribed ConsumerBlockItemObserver instance");
                } catch (RuntimeException e) {
                    stopProcessing();
                    LOGGER.log(
                            ERROR,
                            "RuntimeException caught from Pipeline instance. Unsubscribed ConsumerBlockItemObserver instance.");
                    LOGGER.log(ERROR, e.getMessage());
                }
            }
        }
    }

    @Override
    public boolean isTimeoutExpired() {
        return livenessCalculator.isTimeoutExpired();
    }

    @NonNull
    private ResponseSender getResponseSender(@NonNull final SubscribeStreamResponseUnparsed subscribeStreamResponse) {

        final OneOf<SubscribeStreamResponseUnparsed.ResponseOneOfType> responseType =
                subscribeStreamResponse.response();
        return switch (responseType.kind()) {
            case STATUS -> {
                // Per the spec, status messages signal
                // the end of processing. Unsubscribe this
                // observer and send a message back to the
                // client
                stopProcessing();
                yield statusResponseSender;
            }
            case BLOCK_ITEMS -> blockItemsResponseSender;
                // An unknown response type here is a protocol violation
                // and should shut down the server.
            default -> throw new IllegalArgumentException("Unknown response type: " + responseType.kind());
        };
    }

    private interface ResponseSender {
        void send(@NonNull final SubscribeStreamResponseUnparsed subscribeStreamResponse);
    }

    private final class BlockItemsResponseSender implements ResponseSender {
        private boolean streamStarted = false;

        public void send(@NonNull final SubscribeStreamResponseUnparsed subscribeStreamResponse) {

            if (subscribeStreamResponse.blockItems() == null) {
                final String message = PROTOCOL_VIOLATION_MESSAGE.formatted(
                        "SubscribeStreamResponse", "BLOCK_ITEMS", "block_items", subscribeStreamResponse);
                LOGGER.log(ERROR, message);
                throw new IllegalArgumentException(message);
            }

            final List<BlockItemUnparsed> blockItems =
                    Objects.requireNonNull(subscribeStreamResponse.blockItems()).blockItems();

            // Only start sending BlockItems after we've reached
            // the beginning of a block.
            final BlockItemUnparsed firstBlockItem = blockItems.getFirst();
            if (!streamStarted && firstBlockItem.hasBlockHeader()) {
                streamStarted = true;
            }

            if (streamStarted) {
                metricsService
                        .get(BlockNodeMetricTypes.Counter.LiveBlockItemsConsumed)
                        .add(blockItems.size());
                subscribeStreamResponseObserver.onNext(subscribeStreamResponse);
            }
        }
    }

    // TODO: Implement another StatusResponseSender that will unsubscribe the observer once the
    // status code is fixed.
    private final class StatusResponseSender implements ResponseSender {
        public void send(@NonNull final SubscribeStreamResponseUnparsed subscribeStreamResponse) {
            LOGGER.log(DEBUG, "Sending SubscribeStreamResponse downstream: " + subscribeStreamResponse);
            subscribeStreamResponseObserver.onNext(subscribeStreamResponse);
            subscribeStreamResponseObserver.onComplete();
        }
    }

    private void stopProcessing() {
        isResponsePermitted.set(false);
        subscriptionHandler.unsubscribe(this);
    }
}
