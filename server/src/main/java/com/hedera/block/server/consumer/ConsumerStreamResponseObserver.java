// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Gauge.CurrentBlockNumberOutbound;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.LivenessCalculator;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.OneOf;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipeline;
import edu.umd.cs.findbugs.annotations.NonNull;
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
class ConsumerStreamResponseObserver implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final MetricsService metricsService;
    private final Pipeline<? super SubscribeStreamResponseUnparsed> subscribeStreamResponseObserver;
    private BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> prevSubscriptionHandler;

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
     * @param subscribeStreamResponseObserver the observer to use to send responses to the consumer
     * @param blockNodeContext contains the context with metrics and configuration for the
     *     application
     */
    public ConsumerStreamResponseObserver(
            @NonNull final InstantSource producerLivenessClock,
            @NonNull final Pipeline<? super SubscribeStreamResponseUnparsed> subscribeStreamResponseObserver,
            @NonNull final BlockNodeContext blockNodeContext) {

        this.livenessCalculator = new LivenessCalculator(
                producerLivenessClock,
                blockNodeContext
                        .configuration()
                        .getConfigData(ConsumerConfig.class)
                        .timeoutThresholdMillis());

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
            @NonNull final ObjectEvent<SubscribeStreamResponseUnparsed> event, final long l, final boolean b)
            throws ParseException {

        // Only send the response if the consumer has not cancelled
        // or closed the stream.
        if (isResponsePermitted.get()) {
            if (isTimeoutExpired()) {
                unsubscribe();

                // Notify the Helidon observer that we've
                // stopped processing the stream
                subscribeStreamResponseObserver.onComplete();
                LOGGER.log(DEBUG, "Producer liveness timeout. Unsubscribed ConsumerBlockItemObserver.");
            } else {
                // Refresh the producer liveness and pass the BlockItem to the downstream observer.
                livenessCalculator.refresh();

                final SubscribeStreamResponseUnparsed subscribeStreamResponse = event.get();
                final ResponseSender responseSender = getResponseSender(subscribeStreamResponse);
                responseSender.send(subscribeStreamResponse);
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
                unsubscribe();
                yield statusResponseSender;
            }
            case BLOCK_ITEMS -> blockItemsResponseSender;
                // An unknown response type here is a protocol violation
                // and should shut down the server.
            default -> throw new IllegalArgumentException("Unknown response type: " + responseType.kind());
        };
    }

    private interface ResponseSender {
        void send(@NonNull final SubscribeStreamResponseUnparsed subscribeStreamResponse) throws ParseException;
    }

    private final class BlockItemsResponseSender implements ResponseSender {
        private boolean streamStarted = false;

        public void send(@NonNull final SubscribeStreamResponseUnparsed subscribeStreamResponse) throws ParseException {

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
                if (firstBlockItem.hasBlockHeader()) {
                    long blockNumber = BlockHeader.PROTOBUF
                            .parse(Objects.requireNonNull(firstBlockItem.blockHeader()))
                            .number();
                    LOGGER.log(DEBUG, "{0} sending block: {1}", Thread.currentThread(), blockNumber);
                    metricsService.get(CurrentBlockNumberOutbound).set(blockNumber);
                }

                // Increment the number of block items consumed
                metricsService
                        .get(BlockNodeMetricTypes.Counter.LiveBlockItemsConsumed)
                        .add(blockItems.size());

                // Send the response down through Helidon
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

    @Override
    public void unsubscribe() {
        prevSubscriptionHandler.unsubscribe();
    }

    public void setPrevSubscriptionHandler(
            @NonNull
                    final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> prevSubscriptionHandler) {
        this.prevSubscriptionHandler = Objects.requireNonNull(prevSubscriptionHandler);
    }
}
