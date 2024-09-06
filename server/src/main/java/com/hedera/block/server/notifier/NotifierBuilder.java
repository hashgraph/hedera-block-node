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
import com.hedera.hapi.block.stream.BlockItem;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotifierBuilder {

    private Notifiable blockStreamService;
    private final Notifiable mediator;
    private final BlockNodeContext blockNodeContext;

    private Map<
                    EventHandler<ObjectEvent<PublishStreamResponse>>,
                    BatchEventProcessor<ObjectEvent<PublishStreamResponse>>>
            subscribers;

    /** The initial capacity of the subscriber map. */
    private static final int SUBSCRIBER_INIT_CAPACITY = 5;

    private NotifierBuilder(
            @NonNull final Notifiable mediator, @NonNull final BlockNodeContext blockNodeContext) {

        this.subscribers = new ConcurrentHashMap<>(SUBSCRIBER_INIT_CAPACITY);
        this.mediator = mediator;
        this.blockNodeContext = blockNodeContext;
    }

    @NonNull
    public static NotifierBuilder newBuilder(
            @NonNull final Notifiable mediator, @NonNull final BlockNodeContext blockNodeContext) {
        return new NotifierBuilder(mediator, blockNodeContext);
    }

    public NotifierBuilder blockStreamService(Notifiable blockStreamService) {
        this.blockStreamService = blockStreamService;
        return this;
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
    public StreamMediator<BlockItem, PublishStreamResponse> build() {
        return new NotifierImpl(subscribers, blockStreamService, mediator, blockNodeContext);
    }
}
