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

package com.hedera.block.simulator.config.data;

import com.hedera.block.simulator.config.types.StreamingMode;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Defines the configuration data for the block stream in the Hedera Block Simulator.
 *
 * @param delayBetweenBlockItems the delay in microseconds between streaming each block item
 * @param maxBlockItemsToStream the maximum number of block items to stream before stopping
 * @param streamingMode the mode of streaming for the block stream (e.g., time-based, count-based)
 * @param millisecondsPerBlock the duration in milliseconds for each block when using time-based streaming
 * @param blockItemsBatchSize the number of block items to stream in each batch
 */
@ConfigData("blockStream")
public record BlockStreamConfig(
        @ConfigProperty(defaultValue = "1_500_000") int delayBetweenBlockItems,
        @ConfigProperty(defaultValue = "100_000") int maxBlockItemsToStream,
        @ConfigProperty(defaultValue = "MILLIS_PER_BLOCK") StreamingMode streamingMode,
        @ConfigProperty(defaultValue = "1000") int millisecondsPerBlock,
        @ConfigProperty(defaultValue = "1000") int blockItemsBatchSize) {

    /**
     * Creates a new {@link Builder} instance for constructing a {@code BlockStreamConfig}.
     *
     * @return a new {@code Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for creating instances of {@link BlockStreamConfig}.
     */
    public static class Builder {
        private int delayBetweenBlockItems = 1_500_000;
        private int maxBlockItemsToStream = 10_000;
        private StreamingMode streamingMode = StreamingMode.MILLIS_PER_BLOCK;
        private int millisecondsPerBlock = 1000;
        private int blockItemsBatchSize = 1000;

        /**
         * Creates a new instance of the {@code Builder} class with default configuration values.
         */
        public Builder() {
            // Default constructor
        }

        /**
         * Sets the delay between streaming each block item.
         *
         * @param delayBetweenBlockItems the delay in microseconds
         * @return this {@code Builder} instance
         */
        public Builder delayBetweenBlockItems(int delayBetweenBlockItems) {
            this.delayBetweenBlockItems = delayBetweenBlockItems;
            return this;
        }

        /**
         * Sets the maximum number of block items to stream.
         *
         * @param maxBlockItemsToStream the maximum number of items
         * @return this {@code Builder} instance
         */
        public Builder maxBlockItemsToStream(int maxBlockItemsToStream) {
            this.maxBlockItemsToStream = maxBlockItemsToStream;
            return this;
        }

        /**
         * Sets the streaming mode for the block stream.
         *
         * @param streamingMode the {@link StreamingMode} to use
         * @return this {@code Builder} instance
         */
        public Builder streamingMode(StreamingMode streamingMode) {
            this.streamingMode = streamingMode;
            return this;
        }

        /**
         * Sets the duration for each block when using time-based streaming.
         *
         * @param millisecondsPerBlock the duration in milliseconds
         * @return this {@code Builder} instance
         */
        public Builder millisecondsPerBlock(int millisecondsPerBlock) {
            this.millisecondsPerBlock = millisecondsPerBlock;
            return this;
        }

        /**
         * Sets the number of block items to stream in each batch.
         *
         * @param blockItemsBatchSize the batch size
         * @return this {@code Builder} instance
         */
        public Builder blockItemsBatchSize(int blockItemsBatchSize) {
            this.blockItemsBatchSize = blockItemsBatchSize;
            return this;
        }

        /**
         * Builds a new {@link BlockStreamConfig} instance with the configured values.
         *
         * @return a new {@code BlockStreamConfig}
         */
        public BlockStreamConfig build() {
            return new BlockStreamConfig(
                    delayBetweenBlockItems,
                    maxBlockItemsToStream,
                    streamingMode,
                    millisecondsPerBlock,
                    blockItemsBatchSize);
        }
    }
}
