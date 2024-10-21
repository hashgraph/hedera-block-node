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
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;

public class MockNoOpLiveStreamMediator implements LiveStreamMediator {

    private final MetricsService metricsService;

    public MockNoOpLiveStreamMediator(@NonNull final BlockNodeContext blockNodeContext) {
        System.getLogger(getClass().getName()).log(INFO, "Using " + getClass().getSimpleName());
        this.metricsService = blockNodeContext.metricsService();
    }

    @Override
    public void publish(@NonNull BlockItem data) {
        metricsService.get(LiveBlockItems).increment();
    }

    @Override
    public void subscribe(
            @NonNull BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> handler) {}

    @Override
    public void unsubscribe(
            @NonNull BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> handler) {}

    @Override
    public boolean isSubscribed(
            @NonNull BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> handler) {
        return false;
    }

    @Override
    public void unsubscribeAllExpired() {}

    @Override
    public void notifyUnrecoverableError() {}
}
