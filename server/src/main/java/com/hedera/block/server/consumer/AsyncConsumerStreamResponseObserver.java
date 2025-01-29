// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.Functions.Task;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

class AsyncConsumerStreamResponseObserver
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> {

    private final ExecutorService executorService;
    private final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;
    private final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> nextBlockNodeEventHandler;
    private final MetricsService metricsService;

    public AsyncConsumerStreamResponseObserver(
            @NonNull final ExecutorService executorService,
            @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
            @NonNull
                    final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> nextBlockNodeEventHandler,
            @NonNull final BlockNodeContext blockNodeContext) {

        this.executorService = Objects.requireNonNull(executorService);
        this.subscriptionHandler = Objects.requireNonNull(subscriptionHandler);
        this.nextBlockNodeEventHandler = Objects.requireNonNull(nextBlockNodeEventHandler);
        this.metricsService = blockNodeContext.metricsService();
    }

    @Override
    public void onEvent(
            @NonNull final ObjectEvent<SubscribeStreamResponseUnparsed> event, final long l, final boolean b) {

        try {
            executorService
                    .submit(new Task(event, l, b, subscriptionHandler, this, nextBlockNodeEventHandler, metricsService))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            }

            throw new RuntimeException("Unexpected exception occurred", cause);
        }
    }

    @Override
    public void unsubscribe() {
        subscriptionHandler.unsubscribe(this);
    }
}
