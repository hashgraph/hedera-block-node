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
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotifierBuilder {

    private final Notifiable blockStreamService;
    private final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> mediator;
    private final BlockNodeContext blockNodeContext;

    private Map<
                    EventHandler<ObjectEvent<PublishStreamResponse>>,
                    BatchEventProcessor<ObjectEvent<PublishStreamResponse>>>
            subscribers;

    /** The initial capacity of the subscriber map. */
    private static final int SUBSCRIBER_INIT_CAPACITY = 5;

    private NotifierBuilder(
            @NonNull final Notifiable blockStreamService,
            @NonNull final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> mediator,
            @NonNull final BlockNodeContext blockNodeContext) {

        this.subscribers = new ConcurrentHashMap<>(SUBSCRIBER_INIT_CAPACITY);
        this.blockStreamService = blockStreamService;
        this.mediator = mediator;
        this.blockNodeContext = blockNodeContext;
    }

    @NonNull
    public static NotifierBuilder newBuilder(
            @NonNull final Notifiable blockStreamService,
            @NonNull final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> mediator,
            @NonNull final BlockNodeContext blockNodeContext) {
        return new NotifierBuilder(blockStreamService, mediator, blockNodeContext);
    }

    @NonNull
    public NotifierBuilder subscribers(
            @NonNull
                    final Map<
                                    EventHandler<ObjectEvent<PublishStreamResponse>>,
                                    BatchEventProcessor<ObjectEvent<PublishStreamResponse>>>
                            subscribers) {
        this.subscribers = subscribers;
        return this;
    }

    @NonNull
    public StreamMediator<BlockItem, ObjectEvent<PublishStreamResponse>> build() {
        return new NotifierImpl(subscribers, blockStreamService, mediator, blockNodeContext);
    }
}
