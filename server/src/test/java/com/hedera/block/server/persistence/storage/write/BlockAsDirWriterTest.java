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

import static com.hedera.block.server.persistence.storage.read.BlockAsLocalDirReaderTest.removeBlockReadPerms;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static com.hedera.block.server.util.TestConfigUtil.DEFAULT_TEST_FOLDER_PERMISSIONS;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.read.BlockAsLocalDirReader;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockAsLocalDirRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
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
    private static final String TEMP_DIR = "block-node-unit-test-dir";
    private static final String PERSISTENCE_STORAGE_ROOT_PATH_KEY = "persistence.storage.liveRootPath";
    private final Logger LOGGER = System.getLogger(getClass().getName());
    private Path testPath;
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;

    private List<BlockItemUnparsed> blockItems;

    @BeforeEach
    public void setUp() throws IOException {
        testPath = Files.createTempDirectory(TEMP_DIR);
        LOGGER.log(INFO, "Created temp directory: " + testPath.toString());

        blockNodeContext =
                TestConfigUtil.getTestBlockNodeContext(Map.of(PERSISTENCE_STORAGE_ROOT_PATH_KEY, testPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        blockItems = generateBlockItemsUnparsed(1);
    }

    @AfterEach
    public void tearDown() {
        if (!TestUtils.deleteDirectory(testPath.toFile())) {
            LOGGER.log(ERROR, "Failed to delete temp directory: " + testPath.toString());
        }
    }

    @Test
    public void testWriterAndReaderHappyPath() throws IOException, ParseException {

        final BlockWriter<List<BlockItemUnparsed>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class))
                .folderPermissions(DEFAULT_TEST_FOLDER_PERMISSIONS)
                .build();
        for (int i = 0; i < 10; i++) {
            if (i == 9) {
                Optional<List<BlockItemUnparsed>> result = blockWriter.write(List.of(blockItems.get(i)));
                if (result.isPresent()) {
                    assertEquals(blockItems.get(i), result.get().get(0));
                } else {
                    fail("The optional should contain the last block proof block item");
                }
            } else {
                Optional<List<BlockItemUnparsed>> result = blockWriter.write(List.of(blockItems.get(i)));
                assertTrue(result.isEmpty());
            }
        }

        // Confirm the block
        BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig, null);
        Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());

        boolean hasHeader = false;
        boolean hasBlockProof = false;
        boolean hasStartEvent = false;

        BlockUnparsed block = blockOpt.get();
        for (BlockItemUnparsed blockItem : block.blockItems()) {
            if (blockItem.hasBlockHeader()) {
                hasHeader = true;
            } else if (blockItem.hasBlockProof()) {
                hasBlockProof = true;
            } else if (blockItem.hasEventHeader()) {
                hasStartEvent = true;
            } else {
                fail("Unknown block item type");
            }
        }

        assertTrue(hasHeader, "Block should have a header");
        assertTrue(hasBlockProof, "Block should have a block proof");
        assertTrue(hasStartEvent, "Block should have a start event");
    }

    @Test
    public void testRemoveBlockWritePerms() throws IOException, ParseException {

        final BlockWriter<List<BlockItemUnparsed>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class))
                .build();

        // Change the permissions on the block node root directory
        removeRootWritePerms(testConfig);

        // The first BlockItem contains a header which will create a new block directory.
        // The BlockWriter will attempt to repair the permissions and should succeed.
        Optional<List<BlockItemUnparsed>> result = blockWriter.write(List.of(blockItems.getFirst()));
        assertFalse(result.isPresent());

        // Confirm we're able to read 1 block item
        BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig, null);
        Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(1, blockOpt.get().blockItems().size());
        assertTrue(blockOpt.get().blockItems().getFirst().hasBlockHeader());

        // Remove all permissions on the block directory and
        // attempt to write the next block item
        removeBlockAllPerms(1, testConfig);
        result = blockWriter.write(List.of(blockItems.get(1)));
        assertFalse(result.isPresent());

        // There should now be 2 blockItems in the block
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(2, blockOpt.get().blockItems().size());
        assertFalse(blockOpt.get().blockItems().get(1).hasBlockHeader());

        // Remove read permission on the block directory
        removeBlockReadPerms(1, testConfig);
        result = blockWriter.write(List.of(blockItems.get(2)));
        assertFalse(result.isPresent());

        // There should now be 3 blockItems in the block
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(3, blockOpt.get().blockItems().size());
        assertFalse(blockOpt.get().blockItems().get(1).hasBlockHeader());
    }

    @Test
    public void testUnrecoverableIOExceptionOnWrite() throws IOException, ParseException {

        final BlockRemover blockRemover = BlockAsLocalDirRemover.of(mock(BlockPathResolver.class));

        // Use a spy to simulate an IOException when the first block item is written
        final BlockWriter<List<BlockItemUnparsed>> blockWriter =
                spy(BlockAsDirWriterBuilder.newBuilder(blockNodeContext, blockRemover, mock(BlockPathResolver.class))
                        .build());
        doThrow(IOException.class).when(blockWriter).write(blockItems);
        assertThrows(IOException.class, () -> blockWriter.write(blockItems));
    }

    @Test
    public void testRemoveRootDirReadPerm() throws IOException, ParseException {

        final BlockWriter<List<BlockItemUnparsed>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class))
                .build();

        // Write the first block item to create the block
        // directory
        Optional<List<BlockItemUnparsed>> result = blockWriter.write(List.of(blockItems.getFirst()));
        assertFalse(result.isPresent());

        // Remove root dir read permissions and
        // block dir read permissions
        removeRootReadPerms(testConfig);
        removeBlockReadPerms(1, testConfig);

        // Attempt to write the remaining block
        // items
        for (int i = 1; i < 10; i++) {
            if (i == 9) {
                result = blockWriter.write(List.of(blockItems.get(i)));
                if (result.isPresent()) {
                    assertEquals(blockItems.get(i), result.get().get(0));
                } else {
                    fail("The optional should contain the last block proof block item");
                }
            } else {
                result = blockWriter.write(List.of(blockItems.get(i)));
                assertTrue(result.isEmpty());
            }
        }

        BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig, null);
        Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().blockItems().size());
    }

    @Test
    public void testPartialBlockRemoval() throws IOException, ParseException {
        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(3);
        final BlockRemover blockRemover = BlockAsLocalDirRemover.of(mock(BlockPathResolver.class));
        // Use a spy of TestBlockAsDirWriter to proxy block items to the real writer
        // for the first 22 block items. Then simulate an IOException on the 23rd block item
        // thrown from a protected write method in the real class. This should trigger the
        // blockRemover instance to remove the partially written block.
        final TestBlockAsDirWriter blockWriter = spy(new TestBlockAsDirWriter(blockRemover, null, blockNodeContext));

        for (int i = 0; i < 23; i++) {
            // Prepare the block writer to call the actual write method
            // for 23 block items
            doCallRealMethod().when(blockWriter).write(any(Path.class), same(blockItems.get(i)));
        }

        // Simulate an IOException when writing the 24th block item
        // from an overridden write method in sub-class.
        doThrow(IOException.class).when(blockWriter).write(any(Path.class), same(blockItems.get(23)));

        // Now make the calls
        for (int i = 0; i < 23; i++) {
            Optional<List<BlockItemUnparsed>> result = blockWriter.write(List.of(blockItems.get(i)));
            if (i == 9 || i == 19) {
                // The last block item in each block is the block proof
                // and should be returned by the write method
                assertTrue(result.isPresent());
                assertEquals(blockItems.get(i), result.get().get(0));
            } else {
                // The write method should return an empty optional
                assertTrue(result.isEmpty());
            }
        }

        // Verify the IOException was thrown on the 24th block item
        assertThrows(IOException.class, () -> blockWriter.write(List.of(blockItems.get(23))));

        // Verify the partially written block was removed
        final BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig, null);
        Optional<BlockUnparsed> blockOpt = blockReader.read(3);
        assertTrue(blockOpt.isEmpty());

        // Confirm blocks 1 and 2 still exist
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().blockItems().size());
        assertEquals(
                1,
                BlockHeader.PROTOBUF
                        .parse(blockOpt.get().blockItems().get(0).blockHeader())
                        .number());

        blockOpt = blockReader.read(2);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().blockItems().size());
        assertEquals(
                2,
                BlockHeader.PROTOBUF
                        .parse(blockOpt.get().blockItems().get(0).blockHeader())
                        .number());
    }

    private void removeRootWritePerms(final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.liveRootPath());
        Files.setPosixFilePermissions(blockNodeRootPath, TestUtils.getNoWrite().value());
    }

    private void removeRootReadPerms(final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.liveRootPath());
        Files.setPosixFilePermissions(blockNodeRootPath, TestUtils.getNoRead().value());
    }

    private void removeBlockAllPerms(final int blockNumber, final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.liveRootPath());
        final Path blockPath = blockNodeRootPath.resolve(String.valueOf(blockNumber));
        Files.setPosixFilePermissions(blockPath, TestUtils.getNoPerms().value());
    }

    //     TestBlockAsDirWriter overrides the write() method to allow a test spy to simulate an
    //     IOException while allowing the real write() method to remain protected.
    private static final class TestBlockAsDirWriter extends BlockAsDirWriter {
        public TestBlockAsDirWriter(
                final BlockRemover blockRemover,
                final FileAttribute<Set<PosixFilePermission>> filePerms,
                final BlockNodeContext blockNodeContext)
                throws IOException {
            super(blockNodeContext, blockRemover, mock(BlockPathResolver.class), filePerms);
        }

        @Override
        public void write(@NonNull final Path blockItemFilePath, @NonNull final BlockItemUnparsed blockItem)
                throws IOException {
            super.write(blockItemFilePath, blockItem);
        }
    }
}
