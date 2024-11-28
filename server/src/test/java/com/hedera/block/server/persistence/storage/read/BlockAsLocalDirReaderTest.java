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

package com.hedera.block.server.persistence.storage.read;

import static com.hedera.block.server.Constants.BLOCK_FILE_EXTENSION;
import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static com.hedera.block.server.util.PersistTestUtils.reverseByteArray;
import static com.hedera.block.server.util.PersistTestUtils.writeBlockItemToPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalDirPathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.write.BlockAsLocalDirWriter;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.PersistTestUtils;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BlockAsLocalDirReaderTest {
    @TempDir
    private Path testLiveRootPath;

    private BlockAsLocalDirPathResolver pathResolverMock;
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;
    private List<BlockItemUnparsed> blockItems;

    @BeforeEach
    public void setUp() throws IOException {
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY, testLiveRootPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        blockItems = generateBlockItemsUnparsed(1);

        final String testConfigLiveRootPath = testConfig.liveRootPath();
        assertThat(testConfigLiveRootPath).isEqualTo(testLiveRootPath.toString());
        pathResolverMock = PersistTestUtils.getTrainedBlockAsLocalDirPathResolver(Path.of(testConfigLiveRootPath));
    }

    @Test
    public void testReadBlockDoesNotExist() throws IOException, ParseException {
        final BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        final Optional<BlockUnparsed> blockOpt = blockReader.read(10000);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testReadPermsRepairSucceeded() throws IOException, ParseException {
        final BlockWriter<List<BlockItemUnparsed>> blockWriter =
                BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock);
        for (final BlockItemUnparsed blockItem : blockItems) {
            blockWriter.write(List.of(blockItem));
        }

        // Make the block unreadable
        removeBlockReadPerms(1, testConfig);

        // The default BlockReader will attempt to repair the permissions and should succeed
        final BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        final Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().blockItems().size());
    }

    @Test
    public void testRemoveBlockItemReadPerms() throws IOException, ParseException {
        final BlockWriter<List<BlockItemUnparsed>> blockWriter =
                BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock);
        blockWriter.write(blockItems);

        removeBlockItemReadPerms(1, 1, testConfig);

        final BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        assertThrows(IOException.class, () -> blockReader.read(1));
    }

    @Test
    public void testPathIsNotDirectory() throws IOException, ParseException {

        final Path blockNodeRootPath = Path.of(testConfig.liveRootPath());

        // Write a file named "1" where a directory should be
        writeBlockItemToPath(blockNodeRootPath.resolve(Path.of("1")), blockItems.getFirst());

        // Should return empty because the path is not a directory
        final BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        final Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testRepairReadPermsFails() throws IOException, ParseException {
        final BlockWriter<List<BlockItemUnparsed>> blockWriter =
                BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock);
        blockWriter.write(blockItems);

        removeBlockReadPerms(1, testConfig);

        // Use a spy on a subclass of the BlockAsDirReader to proxy calls
        // to the actual methods but to also throw an IOException when
        // the setPerm method is called.
        final TestBlockAsLocalDirReader blockReader = spy(new TestBlockAsLocalDirReader(testConfig));
        doThrow(IOException.class).when(blockReader).setPerm(any(), any());

        final Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testBlockNodePathReadFails() throws IOException, ParseException {

        // Remove read perm on the root path
        removePathReadPerms(Path.of(testConfig.liveRootPath()));

        // Use a spy on a subclass of the BlockAsDirReader to proxy calls
        // to the actual methods but to also throw an IOException when
        // the setPerm method is called.
        final TestBlockAsLocalDirReader blockReader = spy(new TestBlockAsLocalDirReader(testConfig));
        doThrow(IOException.class).when(blockReader).setPerm(any(), any());

        final Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testParseExceptionHandling() throws IOException, ParseException {
        final BlockWriter<List<BlockItemUnparsed>> blockWriter =
                BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock);
        blockWriter.write(blockItems);

        // Read the block back and confirm it's read successfully
        final BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        final Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());

        final PersistenceStorageConfig persistenceStorageConfig =
                blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        final Path blockNodeRootPath = Path.of(persistenceStorageConfig.liveRootPath());
        Path blockPath = blockNodeRootPath.resolve(String.valueOf(1));

        byte[] bytes;
        try (final FileInputStream fis = new FileInputStream(
                blockPath.resolve("1" + BLOCK_FILE_EXTENSION).toFile())) {
            bytes = fis.readAllBytes();
        }

        // Corrupt the block item file by reversing the bytes
        try (final FileOutputStream fos = new FileOutputStream(
                blockPath.resolve("1" + BLOCK_FILE_EXTENSION).toFile())) {
            byte[] reversedBytes = reverseByteArray(bytes);
            fos.write(reversedBytes);
        }

        // Read the block. The block item file is corrupted, so the read should fail with a
        // ParseException
        assertThrows(ParseException.class, () -> blockReader.read(1));
    }

    public static void removeBlockReadPerms(int blockNumber, final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.liveRootPath());
        final Path blockPath = blockNodeRootPath.resolve(String.valueOf(blockNumber));
        removePathReadPerms(blockPath);
    }

    static void removePathReadPerms(final Path path) throws IOException {
        Files.setPosixFilePermissions(path, TestUtils.getNoRead().value());
    }

    private void removeBlockItemReadPerms(int blockNumber, int blockItem, PersistenceStorageConfig config)
            throws IOException {
        final Path blockNodeRootPath = Path.of(config.liveRootPath());
        final Path blockPath = blockNodeRootPath.resolve(String.valueOf(blockNumber));
        final Path blockItemPath = blockPath.resolve(blockItem + BLOCK_FILE_EXTENSION);
        Files.setPosixFilePermissions(blockItemPath, TestUtils.getNoRead().value());
    }

    // TestBlockAsDirReader overrides the setPerm() method to allow a test spy to simulate an
    // IOException while allowing the real setPerm() method to remain protected.
    private static final class TestBlockAsLocalDirReader extends BlockAsLocalDirReader {
        public TestBlockAsLocalDirReader(PersistenceStorageConfig config) {
            super(config);
        }

        @Override
        public void setPerm(@NonNull final Path path, @NonNull final Set<PosixFilePermission> perms)
                throws IOException {
            super.setPerm(path, perms);
        }
    }
}
