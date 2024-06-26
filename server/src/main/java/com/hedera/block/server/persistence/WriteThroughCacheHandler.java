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

package com.hedera.block.server.persistence;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.persistence.cache.BlockCache;
import com.hedera.block.server.persistence.storage.BlockStorage;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

/**
 * Write-Through cache handler coordinates between the block storage and the block cache to ensure the block
 * is persisted to the storage before being cached.
 */
public class WriteThroughCacheHandler implements BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> {

    private final BlockStorage<BlockStreamServiceGrpcProto.Block> blockStorage;
    private final BlockCache<BlockStreamServiceGrpcProto.Block> blockCache;

    /**
     * Constructor for the WriteThroughCacheHandler class.
     *
     * @param blockStorage the block storage
     * @param blockCache the block cache
     */
    public WriteThroughCacheHandler(final BlockStorage<BlockStreamServiceGrpcProto.Block> blockStorage,
                                    final BlockCache<BlockStreamServiceGrpcProto.Block> blockCache) {
        this.blockStorage = blockStorage;
        this.blockCache = blockCache;
    }

    /**
     * Persists the block to the block storage and cache the block.
     *
     * @param block the block to persist
     * @return the block id
     */
    @Override
    public Long persist(final BlockStreamServiceGrpcProto.Block block) {

        // Write-Through cache
        blockStorage.write(block);
        return blockCache.insert(block);
    }

    /**
     * Reads a range of blocks from the block storage and cache.
     *
     * @param startBlockId the start block id
     * @param endBlockId the end block id
     * @return the blocks
     */
    @Override
    public Queue<BlockStreamServiceGrpcProto.Block> readRange(final long startBlockId, final long endBlockId) {
        final Queue<BlockStreamServiceGrpcProto.Block> blocks = new ArrayDeque<>();

        long count = startBlockId;
        Optional<BlockStreamServiceGrpcProto.Block> blockOpt = read(count);
        while (count <= endBlockId && blockOpt.isPresent()) {
            final BlockStreamServiceGrpcProto.Block block = blockOpt.get();
            blocks.offer(block);
            blockOpt = read(++count);
        }

        return blocks;
    }

    /**
     * Reads a block from cache first.  If the block is not in cache, read from storage and update the cache.
     *
     * @param id the block id
     * @return the block
     */
    @Override
    public Optional<BlockStreamServiceGrpcProto.Block> read(final long id) {

        if (blockCache.contains(id)) {
            return Optional.of(blockCache.get(id));
        } else {
            // Update the cache with the block from storage
            Optional<BlockStreamServiceGrpcProto.Block> block = blockStorage.read(id);
            block.ifPresent(blockCache::insert);

            return block;
        }
    }
}
