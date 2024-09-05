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

import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use builder methods to create a {@link StreamMediator} to handle live stream events from a
 * producer to N consumers.
 *
 * <p>When a stream mediator is created, it will accept new block items from a producer, publish
 * them to all consumers subscribed to the stream, and persist the block items to storage
 * represented by a {@link BlockWriter}.
 */
public class LiveStreamMediatorBuilder {

    private final BlockNodeContext blockNodeContext;
    private final ServiceStatus serviceStatus;

    private Map<
                    EventHandler<ObjectEvent<SubscribeStreamResponse>>,
                    BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
            subscribers;

    /** The initial capacity of the subscriber map. */
    private static final int SUBSCRIBER_INIT_CAPACITY = 32;

    private LiveStreamMediatorBuilder(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
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
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
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
                                    EventHandler<ObjectEvent<SubscribeStreamResponse>>,
                                    BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
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
