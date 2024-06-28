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
import com.hedera.block.server.persistence.storage.BlockStorage;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

/**
 * Write-Through cache handler coordinates between the block storage and the block cache to ensure the block
 * is persisted to the storage before being cached.
 */
public class WriteThroughCacheHandler implements BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> {

    private final BlockStorage<BlockStreamServiceGrpcProto.Block> blockStorage;

    /**
     * Constructor for the WriteThroughCacheHandler class.
     *
     * @param blockStorage the block storage
     */
    public WriteThroughCacheHandler(final BlockStorage<BlockStreamServiceGrpcProto.Block> blockStorage) {
        this.blockStorage = blockStorage;
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
        return block.getId();
    }

    /**
     * Reads a range of blocks from the block storage and cache.
     *
     * @param startBlockId the start block id
     * @param endBlockId the end block id
     * @return a queue of blocks
     */
    @Override
    public Queue<BlockStreamServiceGrpcProto.Block> readRange(final long startBlockId, final long endBlockId) {
        final Queue<BlockStreamServiceGrpcProto.Block> blocks = new LinkedList<>();

        long count = startBlockId;
        Optional<BlockStreamServiceGrpcProto.Block> blockOpt = read(count);
        while (count <= endBlockId && blockOpt.isPresent()) {
            final BlockStreamServiceGrpcProto.Block block = blockOpt.get();
            blocks.add(block);
            blockOpt = read(++count);
        }

        return blocks;
    }

    /**
     * The read method first checks the cache for the block.
     * If the block is not in cache, then it reads from storage and
     * updates the cache.
     *
     * @param id the block id
     * @return an Optional with the block
     */
    @Override
    public Optional<BlockStreamServiceGrpcProto.Block> read(final long id) {
        return blockStorage.read(id);
    }
}
