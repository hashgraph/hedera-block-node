// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.mediator;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.lmax.disruptor.BatchEventProcessor;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use builder methods to create a {@link StreamMediator} to handle live stream events from a
 * producer to N consumers.
 *
 * <p>When a stream mediator is created, it will accept new block items from a producer, publish
 * them to all consumers subscribed to the stream, and persist the block items to storage
 * represented by a {@link com.hedera.block.server.persistence.storage.write.AsyncBlockWriter}.
 */
public class LiveStreamMediatorBuilder {

    private final BlockNodeContext blockNodeContext;
    private final ServiceStatus serviceStatus;

    private Map<
                    BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>,
                    BatchEventProcessor<ObjectEvent<SubscribeStreamResponseUnparsed>>>
            subscribers;

    /** The initial capacity of the subscriber map. */
    private static final int SUBSCRIBER_INIT_CAPACITY = 32;

    private LiveStreamMediatorBuilder(
            @NonNull final BlockNodeContext blockNodeContext, @NonNull final ServiceStatus serviceStatus) {
        this.subscribers = new ConcurrentHashMap<>(SUBSCRIBER_INIT_CAPACITY);
        this.blockNodeContext = blockNodeContext;
        this.serviceStatus = serviceStatus;
    }

    /**
     * Create a new instance of the builder using the minimum required parameters.
     *
     * @param blockNodeContext is required to provide metrics reporting mechanisms to the stream
     *     mediator.
     * @param serviceStatus is required to provide the stream mediator with access to check the
     *     status of the server and to stop the web server if necessary.
     * @return a new stream mediator builder configured with required parameters.
     */
    @NonNull
    public static LiveStreamMediatorBuilder newBuilder(
            @NonNull final BlockNodeContext blockNodeContext, @NonNull final ServiceStatus serviceStatus) {
        return new LiveStreamMediatorBuilder(blockNodeContext, serviceStatus);
    }

    /**
     * Optionally, provide a map implementation of subscribers the stream mediator. This method
     * should only be used for testing purposely. Provided map implementations should be thread-safe
     * to handle subscribers being added and removed dynamically from the stream mediator at
     * runtime.
     *
     * @param subscribers is the map of subscribers to set
     * @return the builder
     */
    @NonNull
    public LiveStreamMediatorBuilder subscribers(
            @NonNull
                    final Map<
                                    BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>,
                                    BatchEventProcessor<ObjectEvent<SubscribeStreamResponseUnparsed>>>
                            subscribers) {
        this.subscribers = subscribers;
        return this;
    }

    /**
     * Use the build method to construct a stream mediator to handle live stream events from a
     * producer to N consumers.
     *
     * @return the stream mediator to handle live stream events between a producer and N consumers.
     */
    @NonNull
    public LiveStreamMediator build() {
        return new LiveStreamMediatorImpl(subscribers, serviceStatus, blockNodeContext);
    }
}
