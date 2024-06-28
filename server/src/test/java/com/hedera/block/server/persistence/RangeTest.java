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
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.hedera.block.server.persistence.PersistTestUtils.generateBlocks;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RangeTest {

    @Test
    public void testReadRangeWithEvenEntries() {

        int maxEntries = 100;
        int numOfBlocks = 100;

        BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler = generateInMemoryTestBlockPersistenceHandler(maxEntries);
        List<BlockStreamServiceGrpcProto.Block> blocks = generateBlocks(numOfBlocks);
        for (BlockStreamServiceGrpcProto.Block block : blocks) {
            blockPersistenceHandler.persist(block);
        }

        int window = 10;
        int numOfWindows = numOfBlocks / window;

        verifyReadRange(window, numOfWindows, blockPersistenceHandler);
    }

    @Test
    public void testReadRangeWithNoBlocks() {
        int maxEntries = 100;

        BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler = generateInMemoryTestBlockPersistenceHandler(maxEntries);
        Queue<BlockStreamServiceGrpcProto.Block> results = blockPersistenceHandler.readRange(1, 100);
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testReadRangeWhenBlocksLessThanWindow() {
        int maxEntries = 100;
        int numOfBlocks = 9;

        BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler = generateInMemoryTestBlockPersistenceHandler(maxEntries);
        List<BlockStreamServiceGrpcProto.Block> blocks = generateBlocks(numOfBlocks);
        for (BlockStreamServiceGrpcProto.Block block : blocks) {
            blockPersistenceHandler.persist(block);
        }

        int window = 10;

        Queue<BlockStreamServiceGrpcProto.Block> results = blockPersistenceHandler.readRange(1, window);
        assertNotNull(results);
        assertEquals(numOfBlocks, results.size());
    }

    private static void verifyReadRange(
            int window,
            int numOfWindows,
            BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler) {

        for (int j = 0; j < numOfWindows;++j) {

            int startBlockId = (j * window) + 1;
            int endBlockId = (startBlockId + window) - 1;
            Queue<BlockStreamServiceGrpcProto.Block> results = blockPersistenceHandler.readRange(startBlockId, endBlockId);

            for (int i = startBlockId;i <= endBlockId && results.peek() != null;++i) {
                BlockStreamServiceGrpcProto.Block block = results.poll();
                assertNotNull(block);
                assertEquals(i, block.getId());
            }
        }
    }

    private static BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> generateInMemoryTestBlockPersistenceHandler(int maxEntries) {
        // Mock up a simple, in-memory persistence handler
        BlockStorage<BlockStreamServiceGrpcProto.Block> blockStorage = new NoOpTestBlockStorage();
        return new WriteThroughCacheHandler(blockStorage);
    }

    private static class NoOpTestBlockStorage implements BlockStorage<BlockStreamServiceGrpcProto.Block> {

        private final Map<Long, BlockStreamServiceGrpcProto.Block> cache;

        public NoOpTestBlockStorage() {
            this.cache = new HashMap<>();
        }

        @Override
        public Optional<Long> write(BlockStreamServiceGrpcProto.Block block) {
            cache.put(block.getId(), block);
            return Optional.of(block.getId());
        }

        @Override
        public Optional<BlockStreamServiceGrpcProto.Block> read(Long blockId) {
            return Optional.ofNullable(cache.get(blockId));
        }
    }
}
