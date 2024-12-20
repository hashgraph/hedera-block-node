// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.config.data;

import static com.hedera.block.common.utils.Preconditions.requireWhole;
import static java.util.Objects.requireNonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Represents the status of the stream.
 *
 * @param publishedBlocks the number of published blocks
 * @param consumedBlocks the number of consumed blocks
 * @param lastKnownPublisherStatuses the last known publisher statuses
 * @param lastKnownConsumersStatuses the last known consumers statuses
 */
public record StreamStatus(
        long publishedBlocks,
        long consumedBlocks,
        Deque<String> lastKnownPublisherStatuses,
        Deque<String> lastKnownConsumersStatuses) {

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
        private long publishedBlocks = 0;
        private long consumedBlocks = 0;
        private Deque<String> lastKnownPublisherStatuses = new ArrayDeque<>();
        private Deque<String> lastKnownConsumersStatuses = new ArrayDeque<>();

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
        public Builder publishedBlocks(long publishedBlocks) {
            requireWhole(publishedBlocks);
            this.publishedBlocks = publishedBlocks;
            return this;
        }

        /**
         * Sets the number of consumed blocks.
         *
         * @param consumedBlocks the number of consumed blocks
         * @return the builder instance
         */
        public Builder consumedBlocks(long consumedBlocks) {
            requireWhole(consumedBlocks);
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
            this.lastKnownPublisherStatuses = new ArrayDeque<>(lastKnownPublisherStatuses);
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
            this.lastKnownConsumersStatuses = new ArrayDeque<>(lastKnownConsumersStatuses);
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
