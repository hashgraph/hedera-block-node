/*
 * Hedera Block Node
 *
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

package com.hedera.block.server.persistence.cache;

/**
 * The BlockCache interface defines operations to query, store and retrieve blocks from an
 * in-memory store to increase throughput and reduce I/O.
 *
 * @param <V> the type of block to cache
 */
public interface BlockCache<V> {
    /**
     * Inserts a block into the cache.
     *
     * @param block the block to insert
     * @return the id of the block
     */
    Long insert(final V block);

    /**
     * Retrieves a block from the cache.
     *
     * @param id the id of the block to retrieve
     * @return the block
     */
    V get(final Long id);

    /**
     * Checks if the cache contains a block with the given id.
     */
    boolean contains(final Long id);
}
