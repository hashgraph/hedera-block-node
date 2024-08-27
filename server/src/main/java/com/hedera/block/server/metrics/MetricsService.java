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
import edu.umd.cs.findbugs.annotations.NonNull;

/** Use member variables of this class to update metric data for the Hedera Block Node. */
public interface MetricsService {
    /**
     * Update the counter of live block items transiting via the live stream.
     *
     * @return use this metric to increase the counter of block items received
     */
    @NonNull
    Counter liveBlockItems();

    /**
     * Update the counter of blocks persisted to storage.
     *
     * @return use this counter to increase the amount of blocks persisted to disk
     */
    @NonNull
    Counter blocksPersisted();

    /**
     * Update the counter of single blocks retrieved from storage.
     *
     * @return use this metric to increase the counter of single blocks retrieved
     */
    @NonNull
    Counter singleBlocksRetrieved();

    /**
     * Update the gauge of subscribers currently consuming to the live stream.
     *
     * @return Use this to increase or decrease the amount of current subscribers to the live stream
     */
    @NonNull
    LongGauge subscribers();
}
