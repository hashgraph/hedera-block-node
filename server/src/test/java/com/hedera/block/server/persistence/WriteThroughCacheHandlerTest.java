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

package com.hedera.block.server.persistence;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.persistence.cache.BlockCache;
import com.hedera.block.server.persistence.cache.LRUCache;
import com.hedera.block.server.persistence.storage.BlockStorage;
import com.hedera.block.server.persistence.storage.FileSystemBlockStorage;
import com.hedera.block.server.util.TestUtils;
import io.helidon.config.Config;
import io.helidon.config.MapConfigSource;
import io.helidon.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hedera.block.server.persistence.PersistTestUtils.generateBlocks;
import static org.junit.jupiter.api.Assertions.*;

public class WriteThroughCacheHandlerTest {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private static final String TEMP_DIR = "block-node-unit-test-dir";
    private static final String JUNIT = "my-junit-test";

    private Path testPath;
    private Config testConfig;

    @BeforeEach
    public void setUp() throws IOException {
        testPath = Files.createTempDirectory(TEMP_DIR);
        LOGGER.log(System.Logger.Level.INFO, "Created temp directory: " + testPath.toString());

        Map<String, String> testProperties = Map.of(JUNIT, testPath.toString());
        ConfigSource testConfigSource = MapConfigSource.builder().map(testProperties).build();
        testConfig = Config.builder(testConfigSource).build();
    }

    @AfterEach
    public void tearDown() {
        TestUtils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testMaxEntriesGreaterThanBlocks() throws IOException {

        int maxEntries = 5;
        int numOfBlocks = 4;

        FileSystemBlockStorage blockStorage = new FileSystemBlockStorage(JUNIT, testConfig);
        BlockCache<BlockStreamServiceGrpcProto.Block> blockCache = new LRUCache(maxEntries);
        BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler = new WriteThroughCacheHandler(blockStorage, blockCache);

        List<BlockStreamServiceGrpcProto.Block> blocks = generateBlocks(numOfBlocks);
        verifyPersistenceHandler(numOfBlocks, maxEntries, blockCache, blocks, blockPersistenceHandler, testPath);
    }

    @Test
    public void testMaxEntriesEqualToBlocks() throws IOException {
        int maxEntries = 3;
        int numOfBlocks = 3;

        FileSystemBlockStorage blockStorage = new FileSystemBlockStorage(JUNIT, testConfig);
        BlockCache<BlockStreamServiceGrpcProto.Block> blockCache = new LRUCache(maxEntries);
        BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler = new WriteThroughCacheHandler(blockStorage, blockCache);

        List<BlockStreamServiceGrpcProto.Block> blocks = generateBlocks(numOfBlocks);
        verifyPersistenceHandler(numOfBlocks, maxEntries, blockCache, blocks, blockPersistenceHandler, testPath);
    }

    @Test
    public void testMaxEntriesLessThanBlocks() throws IOException {
        int maxEntries = 3;
        int numOfBlocks = 4;

        BlockStorage<BlockStreamServiceGrpcProto.Block> blockStorage = new FileSystemBlockStorage(JUNIT, testConfig);
        BlockCache<BlockStreamServiceGrpcProto.Block> blockCache = new LRUCache(maxEntries);
        BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler = new WriteThroughCacheHandler(blockStorage, blockCache);

        List<BlockStreamServiceGrpcProto.Block> blocks = generateBlocks(numOfBlocks);
        verifyPersistenceHandler(numOfBlocks, maxEntries, blockCache, blocks, blockPersistenceHandler, testPath);
    }

    private static void verifyPersistenceHandler(
            int numOfBlocks,
            int maxEntries,
            BlockCache<BlockStreamServiceGrpcProto.Block> blockCache,
            List<BlockStreamServiceGrpcProto.Block> blocks,
            BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler,
            Path testPath) throws IOException {

        for (BlockStreamServiceGrpcProto.Block block : blocks) {

            // Save the block
            blockPersistenceHandler.persist(block);

            // Read the block
            long blockId = block.getId();
            verifyPersistedBlockIsAccessible(blockId, blockPersistenceHandler);

            // Verify the block was written to the fs
            verifyFileExists(blockId, block, testPath);
        }

        // Verify cache behavior
        verifyCache(numOfBlocks, maxEntries, blockCache, blocks);
    }

    private static void verifyPersistedBlockIsAccessible(long blockId, BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler) {

        // Confirm the block is accessible
        Optional<BlockStreamServiceGrpcProto.Block> blockOpt = blockPersistenceHandler.read(blockId);
        if (blockOpt.isPresent()) {
            assertEquals(blockId, blockOpt.get().getId());
        } else {
            fail("Failed to persist block " + blockId);
        }
    }

    private static void verifyFileExists(long blockId, BlockStreamServiceGrpcProto.Block block, Path testPath) throws IOException {
        // Verify the block was saved on the filesystem
        Path fullTestPath = testPath.resolve(block.getId() + FileSystemBlockStorage.BLOCK_FILE_EXTENSION);
        try (FileInputStream fis = new FileInputStream(fullTestPath.toFile())) {
            BlockStreamServiceGrpcProto.Block fetchedBlock = BlockStreamServiceGrpcProto.Block.parseFrom(fis);
            assertEquals(blockId, fetchedBlock.getId());
            assertEquals(block.getValue(), fetchedBlock.getValue());
        }
    }

    private static void verifyCache(
            int numOfBlocks,
            int maxEntries,
            BlockCache<BlockStreamServiceGrpcProto.Block> blockCache,
            List<BlockStreamServiceGrpcProto.Block> blocks) {

        // Test the cache after all the entries are inserted
        for (BlockStreamServiceGrpcProto.Block block : blocks) {

            long blockId = block.getId();
            BlockStreamServiceGrpcProto.Block cachedBlock = blockCache.get(blockId);

            if (numOfBlocks > maxEntries) {
                // Calculate if the block should be in the cache or evicted
                int maxIndexOutsideCache = numOfBlocks - maxEntries;
                if (blockId <= maxIndexOutsideCache) {
                    // expect a cache miss
                    assertNull(cachedBlock);
                } else {
                    // expect a cache hit
                    assertNotNull(cachedBlock);
                }
            } else {
                // All the blocks should be in the cache
                assertNotNull(cachedBlock);
            }
        }
    }
}
