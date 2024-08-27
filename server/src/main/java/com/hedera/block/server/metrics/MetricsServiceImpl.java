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

package com.hedera.block.server.metrics;

import com.swirlds.metrics.api.Counter;
import com.swirlds.metrics.api.LongGauge;
import com.swirlds.metrics.api.Metrics;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.EnumMap;
import javax.inject.Inject;

/**
 * Use member variables of this class to update metric data for the Hedera Block Node.
 *
 * <p>Metrics are updated by calling the appropriate method on the metric object instance. For
 * example, to increment a counter, call {@link Counter#increment()}.
 */
public class MetricsServiceImpl implements MetricsService {

    private static final String CATEGORY = "hedera_block_node";

    // Live BlockItem Counter
    private static final Counter.Config liveBlockItemCounter =
            new Counter.Config(CATEGORY, CounterMetrics.LIVE_BLOCK_ITEMS.toString())
                    .withDescription("Live BlockItems");

    // Live BlockItem Consumed Counter
    private static final Counter.Config liveBlockItemsConsumed =
            new Counter.Config(CATEGORY, CounterMetrics.LIVE_BLOCK_ITEMS_CONSUMED.toString())
                    .withDescription("Live Block Items Consumed");

    // Block Persistence Counter
    private static final Counter.Config BLOCK_PERSISTENCE_COUNTER =
            new Counter.Config(CATEGORY, "blocks_persisted").withDescription("Blocks Persisted");

    // Subscriber Gauge
    private static final LongGauge.Config SUBSCRIBER_GAUGE =
            new LongGauge.Config(CATEGORY, "subscribers").withDescription("Subscribers");

    // Single Block Retrieved Counter
    private static final Counter.Config SINGLE_BLOCK_RETRIEVED_COUNTER =
            new Counter.Config(CATEGORY, "single_blocks_retrieved")
                    .withDescription("Single Blocks Retrieved");

    private final Counter liveBlockItems;

    private final Counter blocksPersisted;

    private final Counter singleBlocksRetrieved;

    private final LongGauge subscribers;

    /** Update the counter of live block items transiting via the live stream. */
    @Override
    @NonNull
    public final Counter liveBlockItems() {
        return liveBlockItems;
    }

    @Override
    @NonNull
    public final Counter getCounter(CounterMetrics key) {
        return counters.get(key);
    }

    /** Update the counter of blocks persisted to storage. */
    @Override
    @NonNull
    public final Counter blocksPersisted() {
        return blocksPersisted;
    }

    /** Update the counter of single blocks retrieved from storage. */
    @Override
    @NonNull
    public final Counter singleBlocksRetrieved() {
        return singleBlocksRetrieved;
    }

    /** Update the gauge of subscribers currently consuming to the live stream. */
    @Override
    @NonNull
    public final LongGauge subscribers() {
        return subscribers;
    }

    private final EnumMap<CounterMetrics, Counter> counters = new EnumMap<>(CounterMetrics.class);

    /**
     * Create singleton instance of metrics service to be used throughout the application.
     *
     * @param metrics the metrics instance
     */
    @Inject
    public MetricsServiceImpl(@NonNull final Metrics metrics) {
        this.counters.put(
                CounterMetrics.LIVE_BLOCK_ITEMS, metrics.getOrCreate(liveBlockItemsConsumed));

        this.liveBlockItems = metrics.getOrCreate(liveBlockItemCounter);
        this.blocksPersisted = metrics.getOrCreate(BLOCK_PERSISTENCE_COUNTER);
        this.singleBlocksRetrieved = metrics.getOrCreate(SINGLE_BLOCK_RETRIEVED_COUNTER);
        this.subscribers = metrics.getOrCreate(SUBSCRIBER_GAUGE);
    }
}
