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

import static com.hedera.block.server.persistence.storage.read.BlockAsDirReaderTest.removeBlockReadPerms;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.FileUtils;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockAsDirRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.util.PersistTestUtils;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlockAsDirWriterTest {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private static final String TEMP_DIR = "block-node-unit-test-dir";
    private static final String PERSISTENCE_STORAGE_ROOT_PATH_KEY = "persistence.storage.rootPath";

    private Path testPath;
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;

    @BeforeEach
    public void setUp() throws IOException {
        testPath = Files.createTempDirectory(TEMP_DIR);
        LOGGER.log(System.Logger.Level.INFO, "Created temp directory: " + testPath.toString());

        blockNodeContext =
                TestConfigUtil.getTestBlockNodeContext(
                        Map.of(PERSISTENCE_STORAGE_ROOT_PATH_KEY, testPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    }

    @AfterEach
    public void tearDown() {
        if (!TestUtils.deleteDirectory(testPath.toFile())) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Failed to delete temp directory: " + testPath.toString());
        }
    }

    @Test
    public void testWriterAndReaderHappyPath() throws IOException {

        // Write a block
        final List<BlockItem> blockItems = PersistTestUtils.generateBlockItems(1);

        final BlockWriter<BlockItem> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
        for (BlockItem blockItem : blockItems) {
            blockWriter.write(blockItem);
        }

        // Confirm the block
        BlockReader<Block> blockReader = BlockAsDirReaderBuilder.newBuilder(testConfig).build();
        Optional<Block> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());

        boolean hasHeader = false;
        boolean hasBlockProof = false;
        boolean hasStartEvent = false;

        Block block = blockOpt.get();
        for (BlockItem blockItem : block.items()) {
            if (blockItem.hasBlockHeader()) {
                hasHeader = true;
            } else if (blockItem.hasBlockProof()) {
                hasBlockProof = true;
            } else if (blockItem.hasEventHeader()) {
                hasStartEvent = true;
            }
        }

        assertTrue(hasHeader, "Block should have a header");
        assertTrue(hasBlockProof, "Block should have a block proof");
        assertTrue(hasStartEvent, "Block should have a start event");
    }

    @Test
    public void testRemoveBlockWritePerms() throws IOException {

        final List<BlockItem> blockItems = PersistTestUtils.generateBlockItems(1);

        final BlockWriter<BlockItem> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();

        // Change the permissions on the block node root directory
        removeRootWritePerms(testConfig);

        // The first BlockItem contains a header which will create a new block directory.
        // The BlockWriter will attempt to repair the permissions and should succeed.
        blockWriter.write(blockItems.getFirst());

        // Confirm we're able to read 1 block item
        BlockReader<Block> blockReader = BlockAsDirReaderBuilder.newBuilder(testConfig).build();
        Optional<Block> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(1, blockOpt.get().items().size());
        assertTrue(blockOpt.get().items().get(0).hasBlockHeader());

        // Remove all permissions on the block directory and
        // attempt to write the next block item
        removeBlockAllPerms(1, testConfig);
        blockWriter.write(blockItems.get(1));

        // There should now be 2 blockItems in the block
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(2, blockOpt.get().items().size());
        assertFalse(blockOpt.get().items().get(1).hasBlockHeader());

        // Remove read permission on the block directory
        removeBlockReadPerms(1, testConfig);
        blockWriter.write(blockItems.get(2));

        // There should now be 3 blockItems in the block
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(3, blockOpt.get().items().size());
        assertFalse(blockOpt.get().items().get(1).hasBlockHeader());
    }

    @Test
    public void testUnrecoverableIOExceptionOnWrite() throws IOException {

        final List<BlockItem> blockItems = PersistTestUtils.generateBlockItems(1);
        final BlockRemover blockRemover =
                new BlockAsDirRemover(Path.of(testConfig.rootPath()), FileUtils.defaultPerms);

        // Use a spy to simulate an IOException when the first block item is written
        final BlockWriter<BlockItem> blockWriter =
                spy(
                        BlockAsDirWriterBuilder.newBuilder(blockNodeContext)
                                .blockRemover(blockRemover)
                                .build());
        doThrow(IOException.class).when(blockWriter).write(blockItems.getFirst());
        assertThrows(IOException.class, () -> blockWriter.write(blockItems.getFirst()));
    }

    @Test
    public void testRemoveRootDirReadPerm() throws IOException {
        final List<BlockItem> blockItems = PersistTestUtils.generateBlockItems(1);

        final BlockWriter<BlockItem> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();

        // Write the first block item to create the block
        // directory
        blockWriter.write(blockItems.getFirst());

        // Remove root dir read permissions and
        // block dir read permissions
        removeRootReadPerms(testConfig);
        removeBlockReadPerms(1, testConfig);

        // Attempt to write the remaining block
        // items
        for (int i = 1; i < 10; i++) {
            blockWriter.write(blockItems.get(i));
        }

        BlockReader<Block> blockReader = BlockAsDirReaderBuilder.newBuilder(testConfig).build();
        Optional<Block> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().items().size());
    }

    @Test
    public void testPartialBlockRemoval() throws IOException {
        final List<BlockItem> blockItems = PersistTestUtils.generateBlockItems(3);
        final BlockRemover blockRemover =
                new BlockAsDirRemover(Path.of(testConfig.rootPath()), FileUtils.defaultPerms);

        // Use a spy of TestBlockAsDirWriter to proxy block items to the real writer
        // for the first 22 block items.  Then simulate an IOException on the 23rd block item
        // thrown from a protected write method in the real class.  This should trigger the
        // blockRemover instance to remove the partially written block.
        final TestBlockAsDirWriter blockWriter =
                spy(
                        new TestBlockAsDirWriter(
                                blockRemover, FileUtils.defaultPerms, blockNodeContext));

        for (int i = 0; i < 23; i++) {
            // Prepare the block writer to call the actual write method
            // for 23 block items
            doCallRealMethod().when(blockWriter).write(same(blockItems.get(i)));
        }

        // Simulate an IOException when writing the 24th block item
        // from an overridden write method in sub-class.
        doThrow(IOException.class).when(blockWriter).write(any(), same(blockItems.get(23)));

        // Now make the calls
        for (int i = 0; i < 23; i++) {
            blockWriter.write(blockItems.get(i));
        }

        // Verify the IOException was thrown on the 23rd block item
        assertThrows(IOException.class, () -> blockWriter.write(blockItems.get(23)));

        // Verify the partially written block was removed
        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(testConfig).build();
        Optional<Block> blockOpt = blockReader.read(3);
        assertTrue(blockOpt.isEmpty());

        // Confirm blocks 1 and 2 still exist
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().items().size());
        assertEquals(1, blockOpt.get().items().getFirst().blockHeader().number());

        blockOpt = blockReader.read(2);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().items().size());
        assertEquals(2, blockOpt.get().items().getFirst().blockHeader().number());
    }

    private void removeRootWritePerms(final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.rootPath());
        Files.setPosixFilePermissions(blockNodeRootPath, TestUtils.getNoWrite().value());
    }

    private void removeRootReadPerms(final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.rootPath());
        Files.setPosixFilePermissions(blockNodeRootPath, TestUtils.getNoRead().value());
    }

    private void removeBlockAllPerms(final int blockNumber, final PersistenceStorageConfig config)
            throws IOException {
        final Path blockNodeRootPath = Path.of(config.rootPath());
        final Path blockPath = blockNodeRootPath.resolve(String.valueOf(blockNumber));
        Files.setPosixFilePermissions(blockPath, TestUtils.getNoPerms().value());
    }

    // TestBlockAsDirWriter overrides the write() method to allow a test spy to simulate an
    // IOException while allowing the real write() method to remain protected.
    private static final class TestBlockAsDirWriter extends BlockAsDirWriter {
        public TestBlockAsDirWriter(
                final BlockRemover blockRemover,
                final FileAttribute<Set<PosixFilePermission>> filePerms,
                final BlockNodeContext blockNodeContext)
                throws IOException {
            super(blockRemover, filePerms, blockNodeContext);
        }

        @Override
        public void write(@NonNull final Path blockItemFilePath, @NonNull final BlockItem blockItem)
                throws IOException {
            super.write(blockItemFilePath, blockItem);
        }
    }
}
