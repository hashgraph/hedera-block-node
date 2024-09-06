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

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItems;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockStreamMediatorError;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Gauge.Consumers;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.stream.BlockItem;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BatchEventProcessorBuilder;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LiveStreamMediatorImpl is an implementation of the StreamMediator interface. It is responsible
 * for managing the subscribe and unsubscribe operations of downstream consumers. It also proxies
 * block items to the subscribers as they arrive via a RingBuffer and persists the block items to a
 * store.
 */
class LiveStreamMediatorImpl implements LiveStreamMediator {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final RingBuffer<ObjectEvent<SubscribeStreamResponse>> ringBuffer;
    private final ExecutorService executor;

    private final Map<
                    EventHandler<ObjectEvent<SubscribeStreamResponse>>,
                    BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
            subscribers;

    private final ServiceStatus serviceStatus;
    private final MetricsService metricsService;

    /**
     * Constructs a new LiveStreamMediatorImpl instance with the given subscribers, block writer,
     * and service status. This constructor is primarily used for testing purposes. Users of this
     * constructor should take care to supply a thread-safe map implementation for the subscribers
     * to handle the dynamic addition and removal of subscribers at runtime.
     *
     * @param subscribers the map of subscribers to batch event processors. It's recommended the map
     *     implementation is thread-safe
     * @param serviceStatus the service status to stop the service and web server if an exception
     *     occurs while persisting a block item, stop the web server for maintenance, etc
     */
    LiveStreamMediatorImpl(
            @NonNull
                    final Map<
                                    EventHandler<ObjectEvent<SubscribeStreamResponse>>,
                                    BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
                            subscribers,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final BlockNodeContext blockNodeContext) {

        this.subscribers = subscribers;
        this.serviceStatus = serviceStatus;
        this.metricsService = blockNodeContext.metricsService();

        final int ringBufferSize =
                blockNodeContext
                        .configuration()
                        .getConfigData(MediatorConfig.class)
                        .ringBufferSize();

        // Initialize and start the disruptor
        final Disruptor<ObjectEvent<SubscribeStreamResponse>> disruptor =
                new Disruptor<>(ObjectEvent::new, ringBufferSize, DaemonThreadFactory.INSTANCE);
        this.ringBuffer = disruptor.start();
        this.executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);
    }

    /**
     * Publishes the given block item to all subscribers. If an exception occurs while persisting
     * the block item, the service status is set to not running, and all downstream consumers are
     * unsubscribed.
     *
     * @param blockItem the block item from the upstream producer to publish to downstream consumers
     * @throws IOException is thrown if an exception occurs while persisting the block item
     */
    @Override
    public void publish(@NonNull final BlockItem blockItem) throws IOException {

        if (serviceStatus.isRunning()) {

            // Publish the block for all subscribers to receive
            LOGGER.log(DEBUG, "Publishing BlockItem: " + blockItem);
            final var subscribeStreamResponse =
                    SubscribeStreamResponse.newBuilder().blockItem(blockItem).build();
            ringBuffer.publishEvent((event, sequence) -> event.set(subscribeStreamResponse));

            // Increment the block item counter
            metricsService.get(LiveBlockItems).increment();

        } else {
            LOGGER.log(ERROR, "StreamMediator is not accepting BlockItems");
        }
    }

    @Override
    public void notifyUnrecoverableError() {

        // Increment the error counter
        metricsService.get(LiveBlockStreamMediatorError).increment();

        // Disable BlockItem publication for upstream producers
        serviceStatus.stopRunning(this.getClass().getName());
        //        LOGGER.log(
        //                ERROR,
        //                "An exception occurred while attempting to persist the BlockItem: "
        //                        + blockItem,
        //                e);

        LOGGER.log(ERROR, "Send a response to end the stream");

        // Publish the block for all subscribers to receive
        final SubscribeStreamResponse endStreamResponse = buildEndStreamResponse();
        ringBuffer.publishEvent((event, sequence) -> event.set(endStreamResponse));

        // Unsubscribe all downstream consumers
        for (final var subscriber : subscribers.keySet()) {
            LOGGER.log(ERROR, String.format("Unsubscribing: %s", subscriber));
            unsubscribe(subscriber);
        }
    }

    @Override
    public void subscribe(
            @NonNull final EventHandler<ObjectEvent<SubscribeStreamResponse>> handler) {

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
    public void unsubscribe(
            @NonNull final EventHandler<ObjectEvent<SubscribeStreamResponse>> handler) {

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
    public boolean isSubscribed(
            @NonNull EventHandler<ObjectEvent<SubscribeStreamResponse>> handler) {
        return subscribers.containsKey(handler);
    }

    @NonNull
    private static SubscribeStreamResponse buildEndStreamResponse() {
        // The current spec does not contain a generic error code for
        // SubscribeStreamResponseCode.
        // TODO: Replace READ_STREAM_SUCCESS (2) with a generic error code?
        return SubscribeStreamResponse.newBuilder()
                .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                .build();
    }
}
