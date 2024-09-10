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
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Optional;

public class StreamValidatorImpl
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler;
    private final BlockWriter<BlockItem> blockWriter;
    private final Notifier notifier;
    private final MetricsService metricsService;

    public StreamValidatorImpl(
            @NonNull final SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler,
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final Notifier notifier,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.subscriptionHandler = subscriptionHandler;
        this.blockWriter = blockWriter;
        this.notifier = notifier;
        this.metricsService = blockNodeContext.metricsService();
    }

    @Override
    public void onEvent(
            ObjectEvent<SubscribeStreamResponse> event, long sequence, boolean endOfBatch) {
        try {
            // Persist the BlockItem
            final SubscribeStreamResponse subscribeStreamResponse = event.get();
            final BlockItem blockItem = subscribeStreamResponse.blockItem();
            Optional<BlockItem> result = blockWriter.write(blockItem);
            if (result.isPresent()) {
                notifier.publish(blockItem);
            }

        } catch (IOException e) {

            // Unsubscribe from the mediator to avoid additional onEvent calls.
            subscriptionHandler.unsubscribe(this);

            // Broadcast the problem to the notifier
            notifier.notifyUnrecoverableError();
        }
    }
}
