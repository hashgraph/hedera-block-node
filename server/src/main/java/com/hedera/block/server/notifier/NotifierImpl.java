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

package com.hedera.block.server.notifier;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SuccessfulPubStreamResp;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Gauge.Producers;
import static com.hedera.block.server.producer.Util.getFakeHash;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.ItemAcknowledgement;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BatchEventProcessorBuilder;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class NotifierImpl implements StreamMediator<BlockItem, ObjectEvent<PublishStreamResponse>> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final Notifiable blockStreamService;
    private final Notifiable mediator;

    private final RingBuffer<ObjectEvent<PublishStreamResponse>> ringBuffer;
    private final ExecutorService executor;

    private final Map<
                    EventHandler<ObjectEvent<PublishStreamResponse>>,
                    BatchEventProcessor<ObjectEvent<PublishStreamResponse>>>
            subscribers;

    private final MetricsService metricsService;

    NotifierImpl(
            @NonNull
                    final Map<
                                    EventHandler<ObjectEvent<PublishStreamResponse>>,
                                    BatchEventProcessor<ObjectEvent<PublishStreamResponse>>>
                            subscribers,
            @NonNull final Notifiable blockStreamService,
            @NonNull final Notifiable mediator,
            @NonNull final BlockNodeContext blockNodeContext) {

        this.subscribers = subscribers;
        this.blockStreamService = blockStreamService;
        this.mediator = mediator;
        this.metricsService = blockNodeContext.metricsService();

        // Initialize and start the disruptor
        final Disruptor<ObjectEvent<PublishStreamResponse>> disruptor =
                // TODO: replace ring buffer size with a configurable value, create a MediatorConfig
                new Disruptor<>(ObjectEvent::new, 1024, DaemonThreadFactory.INSTANCE);
        this.ringBuffer = disruptor.start();
        this.executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);
    }

    //    @Override
    //    public void notifyUnrecoverableError() {
    //        blockStreamService.notifyUnrecoverableError();
    //        mediator.notifyUnrecoverableError();
    //
    //        // Publish a response to the subscribers
    //        ringBuffer.publishEvent((event, sequence) -> event.set(new
    // PublishStreamResponse(null)));
    //    }

    @Override
    public void publish(@NonNull BlockItem blockItem) throws IOException {

        try {
            // Publish the block item to the subscribers
            final var publishStreamResponse =
                    PublishStreamResponse.newBuilder().acknowledgement(buildAck(blockItem)).build();
            ringBuffer.publishEvent((event, sequence) -> event.set(publishStreamResponse));

            metricsService.get(SuccessfulPubStreamResp).increment();

        } catch (NoSuchAlgorithmException e) {
            final var errorResponse = buildErrorStreamResponse();
            LOGGER.log(ERROR, "Error calculating hash: ", e);

            ringBuffer.publishEvent((event, sequence) -> event.set(errorResponse));
        }
    }

    @NonNull
    public static PublishStreamResponse buildErrorStreamResponse() {
        // TODO: Replace this with a real error enum.
        final EndOfStream endOfStream =
                EndOfStream.newBuilder()
                        .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                        .build();
        return PublishStreamResponse.newBuilder().status(endOfStream).build();
    }

    /**
     * Protected method meant for testing. Builds an Acknowledgement for the block item.
     *
     * @param blockItem the block item to build the Acknowledgement for
     * @return the Acknowledgement for the block item
     * @throws NoSuchAlgorithmException if the hash algorithm is not supported
     */
    @NonNull
    protected Acknowledgement buildAck(@NonNull final BlockItem blockItem)
            throws NoSuchAlgorithmException {
        final ItemAcknowledgement itemAck =
                ItemAcknowledgement.newBuilder()
                        // TODO: Replace this with a real hash generator
                        .itemHash(Bytes.wrap(getFakeHash(blockItem)))
                        .build();

        return Acknowledgement.newBuilder().itemAck(itemAck).build();
    }

    @Override
    public void subscribe(@NonNull final EventHandler<ObjectEvent<PublishStreamResponse>> handler) {

        // Initialize the batch event processor and set it on the ring buffer
        final var batchEventProcessor =
                new BatchEventProcessorBuilder()
                        .build(ringBuffer, ringBuffer.newBarrier(), handler);

        ringBuffer.addGatingSequences(batchEventProcessor.getSequence());
        executor.execute(batchEventProcessor);

        // Keep track of the subscriber
        subscribers.put(handler, batchEventProcessor);

        // update the producer metrics
        metricsService.get(Producers).set(subscribers.size());
    }

    @Override
    public void unsubscribe(
            @NonNull final EventHandler<ObjectEvent<PublishStreamResponse>> handler) {

        // Remove the subscriber
        final var batchEventProcessor = subscribers.remove(handler);
        if (batchEventProcessor == null) {
            LOGGER.log(ERROR, "Producer not found: {0}", handler);

        } else {

            // Stop the processor
            batchEventProcessor.halt();

            // Remove the gating sequence from the ring buffer
            ringBuffer.removeGatingSequence(batchEventProcessor.getSequence());
        }

        // update the subscriber metrics
        metricsService.get(Producers).set(subscribers.size());
    }

    @Override
    public boolean isSubscribed(
            @NonNull final EventHandler<ObjectEvent<PublishStreamResponse>> handler) {
        return subscribers.containsKey(handler);
    }
}
