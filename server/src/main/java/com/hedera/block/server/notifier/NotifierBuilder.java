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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.PublishStreamResponse;
import com.lmax.disruptor.BatchEventProcessor;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use builder methods to create a {@link Notifier} to handle live stream response events from the
 * persistence layer to N producers.
 *
 * <p>When a stream mediator is created, it will accept block item responses from the persistence
 * layer and publish them to all producers subscribed to the stream
 */
public class NotifierBuilder {

    private final Notifiable mediator;
    private final BlockNodeContext blockNodeContext;
    private final ServiceStatus serviceStatus;

    private final Map<
                    BlockNodeEventHandler<ObjectEvent<PublishStreamResponse>>,
                    BatchEventProcessor<ObjectEvent<PublishStreamResponse>>>
            subscribers;

    /** The initial capacity of producers in the subscriber map. */
    private static final int SUBSCRIBER_INIT_CAPACITY = 5;

    private NotifierBuilder(
            @NonNull final Notifiable mediator,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {

        this.subscribers = new ConcurrentHashMap<>(SUBSCRIBER_INIT_CAPACITY);
        this.mediator = mediator;
        this.blockNodeContext = blockNodeContext;
        this.serviceStatus = serviceStatus;
    }

    /**
     * Create a new instance of the builder using the minimum required parameters.
     *
     * @param mediator is required to provide notification of critical system events.
     * @param blockNodeContext is required to provide metrics reporting mechanisms to the stream
     *     mediator.
     * @param serviceStatus is required to provide the stream mediator with access to check the
     *     status of the server and to stop the web server if necessary.
     * @return a new stream mediator builder configured with required parameters.
     */
    @NonNull
    public static NotifierBuilder newBuilder(
            @NonNull final Notifiable mediator,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        return new NotifierBuilder(mediator, blockNodeContext, serviceStatus);
    }

    /**
     * Use the build method to construct a notifier to handle live stream response events from the
     * persistence layer to N producers.
     *
     * @return the notifier to handle live stream response events between they persistence layer
     *     producer and N producers.
     */
    @NonNull
    public Notifier build() {
        return new NotifierImpl(subscribers, mediator, blockNodeContext, serviceStatus);
    }
}
