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

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An LRU cache implementation which uses a custom LinkedHashMap to store blocks in memory and
 * evict the least recently used block when the cache size exceeds a specified limit.
 */
@Singleton
public class LRUCache implements BlockCache<BlockStreamServiceGrpcProto.Block> {

    private final Map<Long, BlockStreamServiceGrpcProto.Block> m;

    /**
     * Constructor for the LRUCache class.
     *
     * @param maxEntries the maximum number of entries in the cache
     */
    public LRUCache(final long maxEntries) {
        final Logger LOGGER = Logger.getLogger(getClass().getName());
        LOGGER.finer("Creating LRUCache with maxEntries: " + maxEntries);

        m = Collections.synchronizedMap(new BNLinkedHashMap<>(maxEntries));
    }

    /**
     * Inserts a block into the cache.
     *
     * @param block the block to insert
     * @return the id of the block
     */
    @Override
    public Long insert(final BlockStreamServiceGrpcProto.Block block) {
        final long id = block.getId();
        m.putIfAbsent(id, block);
        return id;
    }

    /**
     * Retrieves a block from the cache.
     *
     * @param id the id of the block to retrieve
     * @return the block
     */
    @Override
    public BlockStreamServiceGrpcProto.Block get(final Long id) {
        return m.get(id);
    }

    /**
     * Checks to see if the cache contains the specified block.
     *
     * @param id the id of the block to query
     */
    @Override
    public boolean contains(final Long id) {
        return m.containsKey(id);
    }
}
