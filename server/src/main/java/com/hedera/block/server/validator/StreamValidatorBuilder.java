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

package com.hedera.block.server.validator;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.lmax.disruptor.EventHandler;
import edu.umd.cs.findbugs.annotations.NonNull;

public class StreamValidatorBuilder {
    private final BlockWriter<BlockItem> blockWriter;
    private final BlockNodeContext blockNodeContext;
    private SubscriptionHandler<ObjectEvent<SubscribeStreamResponse>> subscriptionHandler;
    private StreamMediator<BlockItem, ObjectEvent<PublishStreamResponse>> notifier;

    private StreamValidatorBuilder(
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.blockWriter = blockWriter;
        this.blockNodeContext = blockNodeContext;
    }

    public static StreamValidatorBuilder newBuilder(
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext) {
        return new StreamValidatorBuilder(blockWriter, blockNodeContext);
    }

    public StreamValidatorBuilder subscriptionHandler(
            @NonNull
                    final SubscriptionHandler<ObjectEvent<SubscribeStreamResponse>>
                            subscriptionHandler) {
        this.subscriptionHandler = subscriptionHandler;
        return this;
    }

    public StreamValidatorBuilder notifier(
            @NonNull final StreamMediator<BlockItem, ObjectEvent<PublishStreamResponse>> notifier) {
        this.notifier = notifier;
        return this;
    }

    public EventHandler<ObjectEvent<SubscribeStreamResponse>> build() {
        return new StreamValidatorImpl(
                subscriptionHandler, blockWriter, notifier, blockNodeContext);
    }
}
