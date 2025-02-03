// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.consumer.Functions.ProcessOutboundEvent;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The AsyncConsumerStreamResponseObserver class is responsible for decoupling the
 * RingBuffer worker threads from the consumer processing required to send each
 * response to a downstream consumer.
 */
class AsyncConsumerStreamResponseObserver
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> {

    private static final Logger LOGGER = System.getLogger(ProcessOutboundEvent.class.getName());

    private final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;
    private final CompletionService<Void> completionService;
    private final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> nextBlockNodeEventHandler;

    /**
     * Constructor for the AsyncConsumerStreamResponseObserver class.
     *
     * @param subscriptionHandler the handler for managing subscriptions
     * @param nextBlockNodeEventHandler the next block node event handler in the chain
     */
    // spotless:off
    public AsyncConsumerStreamResponseObserver(
            @NonNull final CompletionService<Void> completionService,
            @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
            @NonNull final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>
                            nextBlockNodeEventHandler) {

        this.completionService = Objects.requireNonNull(completionService);
        this.subscriptionHandler = Objects.requireNonNull(subscriptionHandler);
        this.nextBlockNodeEventHandler = Objects.requireNonNull(nextBlockNodeEventHandler);
    }
    // spotless:on

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(
            @NonNull final ObjectEvent<SubscribeStreamResponseUnparsed> event, final long l, final boolean b) {

        try {
            completionService.submit(new ProcessOutboundEvent(event, l, b, nextBlockNodeEventHandler));

            // Non-blocking check - take() propagates
            // exceptions we rely on upstream to handle
            Future<Void> future = completionService.poll();
            if (future != null) {
                future.get();
            }

        } catch (ExecutionException | InterruptedException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            } else if (cause instanceof UncheckedIOException) {
                // UncheckedIOException at this layer will almost
                // always be wrapped SocketExceptions from individual
                // clients disconnecting from the server streaming
                // service. This should be happening all the time.
                subscriptionHandler.unsubscribe(this);
                LOGGER.log(
                        DEBUG,
                        "UncheckedIOException caught from Pipeline instance. Unsubscribed consumer observer instance");
            } else {
                subscriptionHandler.unsubscribe(this);
                LOGGER.log(ERROR, "Exception caught from Pipeline instance. Unsubscribed consumer observer instance.");
                LOGGER.log(ERROR, e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe() {
        subscriptionHandler.unsubscribe(this);
    }
}
