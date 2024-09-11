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
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;

public class StreamValidatorBuilder {
    private final BlockWriter<BlockItem> blockWriter;
    private final BlockNodeContext blockNodeContext;
    private final ServiceStatus serviceStatus;

    private SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler;
    private Notifier notifier;

    private StreamValidatorBuilder(
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        this.blockWriter = blockWriter;
        this.blockNodeContext = blockNodeContext;
        this.serviceStatus = serviceStatus;
    }

    public static StreamValidatorBuilder newBuilder(
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        return new StreamValidatorBuilder(blockWriter, blockNodeContext, serviceStatus);
    }

    public StreamValidatorBuilder subscriptionHandler(
            @NonNull final SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler) {
        this.subscriptionHandler = subscriptionHandler;
        return this;
    }

    public StreamValidatorBuilder notifier(@NonNull final Notifier notifier) {
        this.notifier = notifier;
        return this;
    }

    public BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> build() {
        return new StreamValidatorImpl(
                subscriptionHandler, blockWriter, notifier, blockNodeContext, serviceStatus);
    }
}
