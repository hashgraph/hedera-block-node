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
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.concurrent.Flow;

/**
 * The NoOpProducerObserver class is a stub implementation of the producer observer intended for testing
 * purposes only. It is designed to isolate the Block Node from the Helidon layers during testing while
 * still providing metrics and logging for troubleshooting.
 */
public class NoOpProducerObserver
        implements Flow.Subscriber<PublishStreamRequest>, BlockNodeEventHandler<ObjectEvent<PublishStreamResponse>> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final MetricsService metricsService;

    /**
     * Creates a new NoOpProducerObserver instance for testing and troubleshooting only.
     *
     * @param publishStreamResponseObserver the stream response observer provided by Helidon
     * @param blockNodeContext the block node context
     */
    public NoOpProducerObserver(
            @NonNull final Flow.Subscriber<? super PublishStreamResponse> publishStreamResponseObserver,
            @NonNull final BlockNodeContext blockNodeContext) {
        LOGGER.log(INFO, "Using " + getClass().getName());
        this.metricsService = blockNodeContext.metricsService();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNext(PublishStreamRequest publishStreamRequest) {
        metricsService
                .get(LiveBlockItemsReceived)
                .add(publishStreamRequest.blockItems().blockItems().size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(ObjectEvent<PublishStreamResponse> publishStreamResponseObjectEvent, long l, boolean b)
            throws Exception {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSubscribe(Flow.Subscription subscription) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(Throwable throwable) {
        LOGGER.log(ERROR, "onError method invoked with an exception: ", throwable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onComplete() {}
}
