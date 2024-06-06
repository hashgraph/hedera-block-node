package com.hedera.block.server.persistence;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.BlockStreamService;
import com.hedera.block.server.persistence.cache.BlockCache;
import com.hedera.block.server.persistence.cache.LRUCache;
import com.hedera.block.server.persistence.storage.BlockStorage;
import com.hedera.block.server.persistence.storage.FileSystemBlockStorage;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class WriteThroughCacheHandlerTest {

    private static final String TEMP_DIR = "block-stream-temp-dir";

    @Test
    public void testMaxEntriesExceedsBlocks() throws IOException {

        int maxEntries = 5;
        int numOfBlocks = 4;

        Path testPath = Files.createTempDirectory(TEMP_DIR);
        FileSystemBlockStorage blockStorage = new FileSystemBlockStorage(testPath);
        BlockCache blockCache = new LRUCache(maxEntries);
        BlockPersistenceHandler blockPersistenceHandler = new WriteThroughCacheHandler(blockStorage, blockCache);

        List<BlockStreamServiceGrpcProto.Block> blocks = generateBlocks(numOfBlocks);
        verifyPersistenceHandler(numOfBlocks, maxEntries, blockCache, blocks, blockPersistenceHandler, testPath);
    }

    @Test
    public void testMaxEntriesEqualToBlocks() throws IOException {
        int maxEntries = 3;
        int numOfBlocks = 3;

        Path testPath = Files.createTempDirectory(TEMP_DIR);
        FileSystemBlockStorage blockStorage = new FileSystemBlockStorage(testPath);
        BlockCache blockCache = new LRUCache(maxEntries);
        BlockPersistenceHandler blockPersistenceHandler = new WriteThroughCacheHandler(blockStorage, blockCache);

        List<BlockStreamServiceGrpcProto.Block> blocks = generateBlocks(numOfBlocks);
        verifyPersistenceHandler(numOfBlocks, maxEntries, blockCache, blocks, blockPersistenceHandler, testPath);
    }

    @Test
    public void testMaxEntriesLessThanBlocks() throws IOException {
        int maxEntries = 3;
        int numOfBlocks = 4;

        Path testPath = Files.createTempDirectory(TEMP_DIR);
        FileSystemBlockStorage blockStorage = new FileSystemBlockStorage(testPath);
        BlockCache blockCache = new LRUCache(maxEntries);
        BlockPersistenceHandler blockPersistenceHandler = new WriteThroughCacheHandler(blockStorage, blockCache);

        List<BlockStreamServiceGrpcProto.Block> blocks = generateBlocks(numOfBlocks);
        verifyPersistenceHandler(numOfBlocks, maxEntries, blockCache, blocks, blockPersistenceHandler, testPath);
    }

    private static void verifyPersistenceHandler(
            int numOfBlocks,
            int maxEntries,
            BlockCache blockCache,
            List<BlockStreamServiceGrpcProto.Block> blocks,
            BlockPersistenceHandler blockPersistenceHandler,
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

    private static void verifyPersistedBlockIsAccessible(long blockId, BlockPersistenceHandler blockPersistenceHandler) {

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
        Path fullTestPath = testPath.resolve(block.getId() + ".txt");
        try (FileInputStream fis = new FileInputStream(fullTestPath.toFile())) {
            BlockStreamServiceGrpcProto.Block fetchedBlock = BlockStreamServiceGrpcProto.Block.parseFrom(fis);
            assertEquals(blockId, fetchedBlock.getId());
            assertEquals(block.getValue(), fetchedBlock.getValue());
        }
    }

    private static void verifyCache(
            int numOfBlocks,
            int maxEntries,
            BlockCache blockCache,
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

    private static List<BlockStreamServiceGrpcProto.Block> generateBlocks(int numOfBlocks) {
        return IntStream
                .range(1, numOfBlocks + 1)
                .mapToObj(i -> BlockStreamServiceGrpcProto.Block
                            .newBuilder()
                            .setId(i)
                            .setValue("block-node-" + i).build()
                )
                .collect(Collectors.toList());
    }
}
