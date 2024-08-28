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

import edu.umd.cs.findbugs.annotations.NonNull;

public class BlockNodeMetricNames {
    public enum Counter implements MetricMetadata {
        LiveBlockItems("live_block_items", "Live BlockItems"),
        BlocksPersisted("blocks_persisted", "Blocks Persisted"),
        LiveBlockItemsConsumed("live_block_items_consumed", "Live Block Items Consumed"),
        SingleBlocksRetrieved("single_blocks_retrieved", "Single Blocks Retrieved");

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

    public enum Gauge implements MetricMetadata {
        Subscribers("subscribers", "Subscribers");

        private final String grafanaLabel;
        private final String description;

        Gauge(String grafanaLabel, String description) {
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

    public interface MetricMetadata {
        String grafanaLabel();

        String description();
    }
}
