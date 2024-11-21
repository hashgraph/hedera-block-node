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
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static com.hedera.block.server.util.PersistTestUtils.reverseByteArray;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.PersistTestUtils;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
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

public class BlockAsDirReaderTest {
    @TempDir
    private Path testPath;

    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig config;

    @BeforeEach
    public void setUp() throws IOException {
        blockNodeContext =
                TestConfigUtil.getTestBlockNodeContext(Map.of("persistence.storage.rootPath", testPath.toString()));
        config = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    }

    @Test
    public void testReadBlockDoesNotExist() throws IOException, ParseException {
        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(config).build();
        final Optional<Block> blockOpt = blockReader.read(10000);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testReadPermsRepairSucceeded() throws IOException, ParseException {
        final List<BlockItem> blockItems = generateBlockItems(1);

        final BlockWriter<List<BlockItem>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class))
                .build();
        for (BlockItem blockItem : blockItems) {
            blockWriter.write(List.of(blockItem));
        }

        // Make the block unreadable
        removeBlockReadPerms(1, config);

        // The default BlockReader will attempt to repair the permissions and should succeed
        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(config).build();
        final Optional<Block> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().items().size());
    }

    @Test
    public void testRemoveBlockReadPermsRepairFailed() throws IOException, ParseException {
        final List<BlockItem> blockItems = generateBlockItems(1);

        final BlockWriter<List<BlockItem>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class))
                .build();
        blockWriter.write(blockItems);

        // Make the block unreadable
        removeBlockReadPerms(1, config);

        // For this test, build the Reader with ineffective repair permissions to
        // simulate a failed repair (root changed the perms, etc.)
        final BlockReader<Block> blockReader = BlockAsDirReaderBuilder.newBuilder(config)
                .folderPermissions(TestUtils.getNoPerms())
                .build();
        final Optional<Block> blockOpt = blockReader.read(1);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testRemoveBlockItemReadPerms() throws IOException {
        final List<BlockItem> blockItems = generateBlockItems(1);

        final BlockWriter<List<BlockItem>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class))
                .build();
        blockWriter.write(blockItems);

        removeBlockItemReadPerms(1, 1, config);

        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(config).build();
        assertThrows(IOException.class, () -> blockReader.read(1));
    }

    @Test
    public void testPathIsNotDirectory() throws IOException, ParseException {
        final List<BlockItem> blockItems = generateBlockItems(1);
        final Path blockNodeRootPath = Path.of(config.rootPath());

        // Write a file named "1" where a directory should be
        PersistTestUtils.writeBlockItemToPath(blockNodeRootPath.resolve(Path.of("1")), blockItems.getFirst());

        // Should return empty because the path is not a directory
        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(config).build();
        final Optional<Block> blockOpt = blockReader.read(1);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testRepairReadPermsFails() throws IOException, ParseException {

        final List<BlockItem> blockItems = generateBlockItems(1);

        final BlockWriter<List<BlockItem>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class))
                .build();
        blockWriter.write(blockItems);

        removeBlockReadPerms(1, config);

        // Use a spy on a subclass of the BlockAsDirReader to proxy calls
        // to the actual methods but to also throw an IOException when
        // the setPerm method is called.
        final TestBlockAsDirReader blockReader = spy(new TestBlockAsDirReader(config));
        doThrow(IOException.class).when(blockReader).setPerm(any(), any());

        final Optional<Block> blockOpt = blockReader.read(1);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testBlockNodePathReadFails() throws IOException, ParseException {

        // Remove read perm on the root path
        removePathReadPerms(Path.of(config.rootPath()));

        // Use a spy on a subclass of the BlockAsDirReader to proxy calls
        // to the actual methods but to also throw an IOException when
        // the setPerm method is called.
        final TestBlockAsDirReader blockReader = spy(new TestBlockAsDirReader(config));
        doThrow(IOException.class).when(blockReader).setPerm(any(), any());

        final Optional<Block> blockOpt = blockReader.read(1);
        assertTrue(blockOpt.isEmpty());
    }

    @Test
    public void testParseExceptionHandling() throws IOException, ParseException {
        final List<BlockItem> blockItems = generateBlockItems(1);

        final BlockWriter<List<BlockItem>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class))
                .build();
        blockWriter.write(blockItems);

        // Read the block back and confirm it's read successfully
        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(config).build();
        final Optional<Block> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());

        final PersistenceStorageConfig persistenceStorageConfig =
                blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        final Path blockNodeRootPath = Path.of(persistenceStorageConfig.rootPath());
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
        final Path blockNodeRootPath = Path.of(config.rootPath());
        final Path blockPath = blockNodeRootPath.resolve(String.valueOf(blockNumber));
        removePathReadPerms(blockPath);
    }

    static void removePathReadPerms(final Path path) throws IOException {
        Files.setPosixFilePermissions(path, TestUtils.getNoRead().value());
    }

    private void removeBlockItemReadPerms(int blockNumber, int blockItem, PersistenceStorageConfig config)
            throws IOException {
        final Path blockNodeRootPath = Path.of(config.rootPath());
        final Path blockPath = blockNodeRootPath.resolve(String.valueOf(blockNumber));
        final Path blockItemPath = blockPath.resolve(blockItem + BLOCK_FILE_EXTENSION);
        Files.setPosixFilePermissions(blockItemPath, TestUtils.getNoRead().value());
    }

    // TestBlockAsDirReader overrides the setPerm() method to allow a test spy to simulate an
    // IOException while allowing the real setPerm() method to remain protected.
    private static final class TestBlockAsDirReader extends BlockAsDirReader {
        public TestBlockAsDirReader(PersistenceStorageConfig config) {
            super(config, null);
        }

        @Override
        public void setPerm(@NonNull final Path path, @NonNull final Set<PosixFilePermission> perms)
                throws IOException {
            super.setPerm(path, perms);
        }
    }
}
