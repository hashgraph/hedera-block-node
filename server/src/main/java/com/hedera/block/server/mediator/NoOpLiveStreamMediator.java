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

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItems;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;

/**
 * The NoOpLiveStreamMediator class is a stub implementation of the live stream mediator intended for testing
 * purposes only. It is designed to isolate the Producer component from downstream components subscribed to
 * the ring buffer during testing while still providing metrics and logging for troubleshooting.
 */
public class NoOpLiveStreamMediator implements LiveStreamMediator {

    private final MetricsService metricsService;

    /**
     * Creates a new NoOpLiveStreamMediator instance for testing and troubleshooting only.
     *
     * @param blockNodeContext the block node context
     */
    public NoOpLiveStreamMediator(@NonNull final BlockNodeContext blockNodeContext) {
        System.getLogger(getClass().getName()).log(INFO, "Using " + getClass().getSimpleName());
        this.metricsService = blockNodeContext.metricsService();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(@NonNull List<BlockItemUnparsed> blockItems) {
        metricsService.get(LiveBlockItems).add(blockItems.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(@NonNull BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> handler) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe(@NonNull BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> handler) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscribed(@NonNull BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> handler) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribeAllExpired() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyUnrecoverableError() {}

    @Override
    public int subscriberCount() {
        return 0;
    }
}
