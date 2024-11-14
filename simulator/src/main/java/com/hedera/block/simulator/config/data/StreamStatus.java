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

import com.hedera.block.common.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

import static com.hedera.block.common.utils.Preconditions.requirePositive;
import static java.util.Objects.requireNonNull;

/**
 * Represents the status of the stream.
 *
 * @param publishedBlocks the number of published blocks
 * @param consumedBlocks the number of consumed blocks
 * @param lastKnownPublisherStatuses the last known publisher statuses
 * @param lastKnownConsumersStatuses the last known consumers statuses
 */
public record StreamStatus(
        int publishedBlocks,
        int consumedBlocks,
        List<String> lastKnownPublisherStatuses,
        List<String> lastKnownConsumersStatuses) {

    /**
     * Creates a new {@link Builder} instance for constructing a {@code StreamStatus}.
     *
     * @return a new {@code Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for creating instances of {@link StreamStatus}.
     */
    public static class Builder {
        private int publishedBlocks = 0;
        private int consumedBlocks = 0;
        private List<String> lastKnownPublisherStatuses = new ArrayList<>();
        private List<String> lastKnownConsumersStatuses = new ArrayList<>();

        /**
         * Creates a new instance of the {@code Builder} class with default configuration values.
         */
        public Builder() {
            // Default constructor
        }

        /**
         * Sets the number of published blocks.
         *
         * @param publishedBlocks the number of published blocks
         * @return the builder instance
         */
        public Builder publishedBlocks(int publishedBlocks) {
            requirePositive(publishedBlocks);
            this.publishedBlocks = publishedBlocks;
            return this;
        }

        /**
         * Sets the number of consumed blocks.
         *
         * @param consumedBlocks the number of consumed blocks
         * @return the builder instance
         */
        public Builder consumedBlocks(int consumedBlocks) {
            requirePositive(consumedBlocks);
            this.consumedBlocks = consumedBlocks;
            return this;
        }

        /**
         * Sets the last known publisher statuses.
         *
         * @param lastKnownPublisherStatuses the last known publisher statuses
         * @return the builder instance
         */
        public Builder lastKnownPublisherStatuses(List<String> lastKnownPublisherStatuses) {
            requireNonNull(lastKnownPublisherStatuses);
            this.lastKnownPublisherStatuses = new ArrayList<>(lastKnownPublisherStatuses);
            return this;
        }

        /**
         * Sets the last known consumers statuses.
         *
         * @param lastKnownConsumersStatuses the last known consumers statuses
         * @return the builder instance
         */
        public Builder lastKnownConsumersStatuses(List<String> lastKnownConsumersStatuses) {
            requireNonNull(lastKnownConsumersStatuses);
            this.lastKnownConsumersStatuses = new ArrayList<>(lastKnownConsumersStatuses);
            return this;
        }

        /**
         * Builds a new {@link StreamStatus} instance.
         *
         * @return a new {@link StreamStatus} instance
         */
        public StreamStatus build() {
            return new StreamStatus(
                    publishedBlocks, consumedBlocks, lastKnownPublisherStatuses, lastKnownConsumersStatuses);
        }
    }
}
