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

/**
 * The BlockNodeMetricNames class contains the names of the metrics used by the BlockNode.
 *
 * <p>These names are used to register the metrics with the metrics service.
 */
public final class BlockNodeMetricTypes {
    private BlockNodeMetricTypes() {}

    /**
     * Add new counting metrics to this enum to automatically register them with the metrics
     * service.
     *
     * <p>Each enum value should have a unique grafana label and meaningful description. These
     * counters can capture data on standard operations or errors.
     */
    public enum Counter implements MetricMetadata {
        // Standard counters
        /** The number of live block items received from a producer. */
        LiveBlockItemsReceived("live_block_items_received", "Live Block Items Received"),

        /** The number of live block items received before publishing to the RingBuffer. */
        LiveBlockItems("live_block_items", "Live BlockItems"),

        /**
         * The number of blocks persisted to storage.
         *
         * <p>Block items are not counted here, only the blocks.
         */
        BlocksPersisted("blocks_persisted", "Blocks Persisted"),

        /** The number of live block items consumed from the by each consumer observer. */
        LiveBlockItemsConsumed("live_block_items_consumed", "Live Block Items Consumed"),

        /** The number of single blocks retrieved from the singleBlock rpc service. */
        SingleBlocksRetrieved("single_blocks_retrieved", "Single Blocks Retrieved"),

        /** The number of single blocks not found via the singleBlock rpc service. */
        SingleBlocksNotFound("single_blocks_not_found", "Single Blocks Not Found"),

        // Error counters

        /** The number of errors encountered by the live block stream mediator. */
        LiveBlockStreamMediatorError(
                "live_block_stream_mediator_error", "Live Block Stream Mediator Error");

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

    /**
     * Add new gauge metrics to this enum to automatically register them with the metrics service.
     *
     * <p>Each enum value should have a unique grafana label and meaningful description. These
     * gauges can capture data on standard operations or errors.
     */
    public enum Gauge implements MetricMetadata {

        /** The number of subscribers receiving the live block stream. */
        Subscribers("subscribers", "Subscribers"),

        /** The number of producers publishing block items. */
        Producers("producers", "Producers");

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

    private interface MetricMetadata {
        String grafanaLabel();

        String description();
    }
}
