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

package com.hedera.block.server.producer;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItemsReceived;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.concurrent.Flow;

public class MockNoOpProducerObserver
        implements Flow.Subscriber<PublishStreamRequest>,
                BlockNodeEventHandler<ObjectEvent<PublishStreamResponse>> {

    private final MetricsService metricsService;

    public MockNoOpProducerObserver(@NonNull final BlockNodeContext blockNodeContext) {
        System.getLogger(getClass().getName()).log(INFO, "Using MockNoOpProducerObserver");
        this.metricsService = blockNodeContext.metricsService();
    }

    @Override
    public void onNext(PublishStreamRequest item) {
        metricsService.get(LiveBlockItemsReceived).increment();
    }

    @Override
    public void onEvent(
            ObjectEvent<PublishStreamResponse> publishStreamResponseObjectEvent, long l, boolean b)
            throws Exception {}

    @Override
    public void onSubscribe(Flow.Subscription subscription) {}

    @Override
    public void onError(Throwable throwable) {}

    @Override
    public void onComplete() {}
}
