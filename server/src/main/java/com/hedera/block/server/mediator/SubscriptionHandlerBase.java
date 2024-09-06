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

package com.hedera.block.server.mediator;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Gauge.Consumers;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.metrics.MetricsService;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BatchEventProcessorBuilder;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SubscriptionHandlerBase<V> implements SubscriptionHandler<V> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final Map<EventHandler<ObjectEvent<V>>, BatchEventProcessor<ObjectEvent<V>>>
            subscribers;

    protected final RingBuffer<ObjectEvent<V>> ringBuffer;
    private final ExecutorService executor;
    private final MetricsService metricsService;

    public SubscriptionHandlerBase(
            @NonNull
                    final Map<EventHandler<ObjectEvent<V>>, BatchEventProcessor<ObjectEvent<V>>>
                            subscribers,
            @NonNull final BlockNodeContext blockNodeContext) {

        this.subscribers = subscribers;
        this.metricsService = blockNodeContext.metricsService();

        final int ringBufferSize =
                blockNodeContext
                        .configuration()
                        .getConfigData(MediatorConfig.class)
                        .ringBufferSize();

        // Initialize and start the disruptor
        final Disruptor<ObjectEvent<V>> disruptor =
                new Disruptor<>(ObjectEvent::new, ringBufferSize, DaemonThreadFactory.INSTANCE);
        this.ringBuffer = disruptor.start();
        this.executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);
    }

    @Override
    public void subscribe(@NonNull final EventHandler<ObjectEvent<V>> handler) {

        // Initialize the batch event processor and set it on the ring buffer
        final var batchEventProcessor =
                new BatchEventProcessorBuilder()
                        .build(ringBuffer, ringBuffer.newBarrier(), handler);

        ringBuffer.addGatingSequences(batchEventProcessor.getSequence());
        executor.execute(batchEventProcessor);

        // Keep track of the subscriber
        subscribers.put(handler, batchEventProcessor);

        // update the subscriber metrics
        metricsService.get(Consumers).set(subscribers.size());
    }

    @Override
    public void unsubscribe(@NonNull final EventHandler<ObjectEvent<V>> handler) {

        // Remove the subscriber
        final var batchEventProcessor = subscribers.remove(handler);
        if (batchEventProcessor == null) {
            LOGGER.log(ERROR, "Subscriber not found: {0}", handler);

        } else {

            // Stop the processor
            batchEventProcessor.halt();

            // Remove the gating sequence from the ring buffer
            ringBuffer.removeGatingSequence(batchEventProcessor.getSequence());
        }

        // update the subscriber metrics
        metricsService.get(Consumers).set(subscribers.size());
    }

    @Override
    public boolean isSubscribed(@NonNull EventHandler<ObjectEvent<V>> handler) {
        return subscribers.containsKey(handler);
    }
}
