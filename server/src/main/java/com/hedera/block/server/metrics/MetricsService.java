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

public class MetricsService {

    private static final String CATEGORY = "app";

    private static final LongGauge.Config EXAMPLE_GAUGE =
            new LongGauge.Config(CATEGORY, "exampleGauge").withDescription("An example gauge");

    /** An example gauge. */
    public final LongGauge exampleGauge;

    private static final Counter.Config EXAMPLE_COUNTER =
            new Counter.Config(CATEGORY, "exampleCounter").withDescription("An example counter");

    /** An example counter. */
    public final Counter exampleCounter;

    /**
     * Creates a new instance of {@link MetricsService}.
     *
     * @param metrics the metrics instance
     */
    public MetricsService(@NonNull final Metrics metrics) {
        this.exampleGauge = metrics.getOrCreate(EXAMPLE_GAUGE);
        this.exampleCounter = metrics.getOrCreate(EXAMPLE_COUNTER);
    }
}
