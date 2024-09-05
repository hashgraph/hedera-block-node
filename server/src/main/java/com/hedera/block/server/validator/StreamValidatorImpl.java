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
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.lmax.disruptor.EventHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

public class StreamValidatorImpl implements EventHandler<ObjectEvent<SubscribeStreamResponse>> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final SubscriptionHandler<ObjectEvent<SubscribeStreamResponse>> subscriptionHandler;
    private final BlockWriter<BlockItem> blockWriter;
    private final StreamMediator<BlockItem, ObjectEvent<PublishStreamResponse>> notifier;
    private final MetricsService metricsService;

    public StreamValidatorImpl(
            @NonNull
                    final SubscriptionHandler<ObjectEvent<SubscribeStreamResponse>>
                            subscriptionHandler,
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final StreamMediator<BlockItem, ObjectEvent<PublishStreamResponse>> notifier,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.subscriptionHandler = subscriptionHandler;
        this.blockWriter = blockWriter;
        this.notifier = notifier;
        this.metricsService = blockNodeContext.metricsService();
    }

    @Override
    public void onEvent(
            ObjectEvent<SubscribeStreamResponse> event, long sequence, boolean endOfBatch)
            throws Exception {
        try {
            // Persist the BlockItem
            final SubscribeStreamResponse subscribeStreamResponse = event.get();
            final BlockItem blockItem = subscribeStreamResponse.blockItem();
            blockWriter.write(blockItem);

        } catch (IOException e) {

            // Unsubscribe from the mediator to avoid additional onEvent calls.
            subscriptionHandler.unsubscribe(this);

            // Broadcast the problem to the notifier
            notifier.publish(null);
        }
    }
}
