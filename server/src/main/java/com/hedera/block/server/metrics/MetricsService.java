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

/**
 * Use member variables of this class to update metric data for the Hedera Block Node.
 *
 * <p>Metrics are updated by calling the appropriate method on the metric object instance. For
 * example, to increment a counter, call {@link Counter#increment()}.
 */
public class MetricsService {

    private static final String CATEGORY = "hedera_block_node";

    private static final LongGauge.Config EXAMPLE_GAUGE =
            new LongGauge.Config(CATEGORY, "exampleGauge").withDescription("An example gauge");

    private static final Counter.Config EXAMPLE_COUNTER =
            new Counter.Config(CATEGORY, "exampleCounter").withDescription("An example counter");

    // Live BlockItem Counter
    private static final Counter.Config LIVE_BLOCK_ITEM_COUNTER =
            new Counter.Config(CATEGORY, "live_block_items").withDescription("Live BlockItems");

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

    /** An example gauge. */
    public final LongGauge exampleGauge;

    /** An example counter. */
    public final Counter exampleCounter;

    /** Update the counter of live block items transiting via the live stream. */
    public final Counter liveBlockItems;

    /** Update the counter of blocks persisted to storage. */
    public final Counter blocksPersisted;

    /** Update the counter of single blocks retrieved from storage. */
    public final Counter singleBlocksRetrieved;

    /** Update the gauge of subscribers currently consuming to the live stream. */
    public final LongGauge subscribers;

    /**
     * Create singleton instance of metrics service to be used throughout the application.
     *
     * @param metrics the metrics instance
     */
    public MetricsService(@NonNull final Metrics metrics) {
        this.exampleGauge = metrics.getOrCreate(EXAMPLE_GAUGE);
        this.exampleCounter = metrics.getOrCreate(EXAMPLE_COUNTER);

        this.liveBlockItems = metrics.getOrCreate(LIVE_BLOCK_ITEM_COUNTER);
        this.blocksPersisted = metrics.getOrCreate(BLOCK_PERSISTENCE_COUNTER);
        this.singleBlocksRetrieved = metrics.getOrCreate(SINGLE_BLOCK_RETRIEVED_COUNTER);
        this.subscribers = metrics.getOrCreate(SUBSCRIBER_GAUGE);
    }
}
