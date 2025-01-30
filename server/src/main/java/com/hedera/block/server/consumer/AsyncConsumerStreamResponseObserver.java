// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import com.hedera.block.server.consumer.Functions.Task;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * The AsyncConsumerStreamResponseObserver class is responsible for decoupling the
 * RingBuffer worker threads from the consumer processing required to send each
 * response to a downstream consumer.
 */
class AsyncConsumerStreamResponseObserver
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> {

    private final ExecutorService executorService;
    private final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;
    private final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> nextBlockNodeEventHandler;

    /**
     * Constructor for the AsyncConsumerStreamResponseObserver class.
     *
     * @param executorService the executor service to use for asynchronous processing
     * @param subscriptionHandler the handler for managing subscriptions
     * @param nextBlockNodeEventHandler the next block node event handler in the chain
     */
    public AsyncConsumerStreamResponseObserver(
            @NonNull final ExecutorService executorService,
            @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
            @NonNull
                    final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>
                            nextBlockNodeEventHandler) {

        this.executorService = Objects.requireNonNull(executorService);
        this.subscriptionHandler = Objects.requireNonNull(subscriptionHandler);
        this.nextBlockNodeEventHandler = Objects.requireNonNull(nextBlockNodeEventHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(
            @NonNull final ObjectEvent<SubscribeStreamResponseUnparsed> event, final long l, final boolean b) {

        try {
            executorService
                    .submit(new Task(event, l, b, subscriptionHandler, this, nextBlockNodeEventHandler))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            }

            throw new RuntimeException("Unexpected exception occurred", cause);
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
