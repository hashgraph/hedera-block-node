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

package com.hedera.block.simulator.metrics;

import com.swirlds.metrics.api.Counter;
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

    private static final String CATEGORY = "hedera_block_node_simulator";

    private final EnumMap<SimulatorMetricTypes.Counter, Counter> counters =
            new EnumMap<>(SimulatorMetricTypes.Counter.class);

    /**
     * Create singleton instance of metrics service to be used throughout the application.
     *
     * @param metrics the metrics instance
     */
    @Inject
    public MetricsServiceImpl(@NonNull final Metrics metrics) {
        // Initialize the counters
        for (SimulatorMetricTypes.Counter counter : SimulatorMetricTypes.Counter.values()) {
            counters.put(
                    counter,
                    metrics.getOrCreate(new Counter.Config(CATEGORY, counter.grafanaLabel())
                            .withDescription(counter.description())));
        }
    }

    /**
     * Use this method to get a specific counter for the given metric type.
     *
     * @param key to get a specific counter
     * @return the counter
     */
    @NonNull
    @Override
    public Counter get(@NonNull SimulatorMetricTypes.Counter key) {
        return counters.get(key);
    }
}
