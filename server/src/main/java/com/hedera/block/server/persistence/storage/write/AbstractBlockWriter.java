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

import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.swirlds.metrics.api.Counter;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;

/**
 * TODO: add documentation
 */
abstract class AbstractBlockWriter<V> implements BlockWriter<V> {
    private final Counter blocksPersistedCounter;
    protected final BlockRemover blockRemover;

    protected AbstractBlockWriter(
            @NonNull final MetricsService metricsService, @NonNull final BlockRemover blockRemover) {
        Objects.requireNonNull(metricsService);
        this.blocksPersistedCounter = Objects.requireNonNull(metricsService.get(BlocksPersisted));
        this.blockRemover = Objects.requireNonNull(blockRemover);
    }

    protected final void incrementBlocksPersisted() {
        blocksPersistedCounter.increment();
    }
}
