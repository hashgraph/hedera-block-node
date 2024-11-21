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

package com.hedera.block.server.persistence.storage.write;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.BlocksPersisted;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.path.PathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The NoOpBlockWriter class is a stub implementation of the block writer intended for testing purposes only. It is
 * designed to isolate the Producer and Mediator components from storage implementation during testing while still
 * providing metrics and logging for troubleshooting.
 */
public class NoOpBlockWriter implements LocalBlockWriter<List<BlockItem>> {
    private final MetricsService metricsService;
    private final BlockRemover blockRemover; // todo do I need here?
    private final PathResolver pathResolver; // todo do I need here?

    /**
     * Creates a new NoOpBlockWriter instance for testing and troubleshooting only.
     *
     * @param blockNodeContext the block node context
     * @param pathResolver used internally
     */
    public NoOpBlockWriter(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final PathResolver pathResolver) {
        this.metricsService = Objects.requireNonNull(blockNodeContext.metricsService());
        this.blockRemover = Objects.requireNonNull(blockRemover);
        this.pathResolver = Objects.requireNonNull(pathResolver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<List<BlockItem>> write(@NonNull final List<BlockItem> toWrite) throws IOException {
        if (toWrite.getLast().hasBlockProof()) {
            metricsService.get(BlocksPersisted).increment();
        }
        return Optional.empty();
    }
}
