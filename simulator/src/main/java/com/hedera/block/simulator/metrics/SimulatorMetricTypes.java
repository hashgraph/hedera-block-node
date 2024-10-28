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

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The BlockNodeMetricNames class contains the names of the metrics used by the BlockNode.
 *
 * <p>These names are used to register the metrics with the metrics service.
 */
public final class SimulatorMetricTypes {
    private SimulatorMetricTypes() {}

    /**
     * Add new counting metrics to this enum to automatically register them with the metrics
     * service.
     *
     * <p>Each enum value should have a unique grafana label and meaningful description. These
     * counters can capture data on standard operations or errors.
     */
    public enum Counter implements MetricMetadata {
        // Standard counters
        /** The number of live block items sent by the simulator . */
        LiveBlockItemsSent("live_block_items_sent", "Live Block Items Sent");

        private final String grafanaLabel;
        private final String description;

        Counter(String grafanaLabel, String description) {
            this.grafanaLabel = grafanaLabel;
            this.description = description;
        }

        @Override
        @NonNull
        public String grafanaLabel() {
            return grafanaLabel;
        }

        @Override
        @NonNull
        public String description() {
            return description;
        }
    }

    private interface MetricMetadata {
        String grafanaLabel();

        String description();
    }
}
