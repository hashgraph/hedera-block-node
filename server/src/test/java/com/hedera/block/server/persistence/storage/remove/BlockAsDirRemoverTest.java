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

package com.hedera.block.server.persistence.storage.remove;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.FileUtils;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.PersistTestUtils;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlockAsDirRemoverTest {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private static final String TEMP_DIR = "block-node-unit-test-dir";

    private Path testPath;
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;

    @BeforeEach
    public void setUp() throws IOException {
        testPath = Files.createTempDirectory(TEMP_DIR);
        LOGGER.log(INFO, "Created temp directory: " + testPath.toString());

        blockNodeContext =
                TestConfigUtil.getTestBlockNodeContext(
                        Map.of("persistence.storage.rootPath", testPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    }

    @Test
    public void testRemoveNonExistentBlock() throws IOException, ParseException {

        // Write a block
        final var blockItems = PersistTestUtils.generateBlockItems(1);

        final BlockWriter<List<BlockItem>> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
        for (final BlockItem blockItem : blockItems) {
            blockWriter.write(List.of(blockItem));
        }

        // Remove a block that does not exist
        final BlockRemover blockRemover = new BlockAsDirRemover(testPath, FileUtils.defaultPerms);
        blockRemover.remove(2);

        // Verify the block was not removed
        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(testConfig).build();
        Optional<Block> blockOpt = blockReader.read(1);
        assert (blockOpt.isPresent());
        assertEquals(
                blockItems.getFirst().blockHeader(),
                blockOpt.get().items().getFirst().blockHeader());

        // Now remove the block
        blockRemover.remove(1);

        // Verify the block is removed
        blockOpt = blockReader.read(1);
        assert (blockOpt.isEmpty());
    }

    @Test
    public void testRemoveBlockWithPermException() throws IOException, ParseException {

        // Write a block
        final List<BlockItem> blockItems = PersistTestUtils.generateBlockItems(1);

        final BlockWriter<List<BlockItem>> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();

        blockWriter.write(blockItems);

        // Set up the BlockRemover with permissions that will prevent the block from being removed
        BlockRemover blockRemover = new BlockAsDirRemover(testPath, TestUtils.getNoPerms());
        blockRemover.remove(1);

        // Verify the block was not removed
        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(testConfig).build();
        Optional<Block> blockOpt = blockReader.read(1);
        assert (blockOpt.isPresent());
        assertEquals(
                blockItems.getFirst().blockHeader(),
                blockOpt.get().items().getFirst().blockHeader());

        // Now remove the block
        blockRemover = new BlockAsDirRemover(testPath, FileUtils.defaultPerms);
        blockRemover.remove(1);

        // Verify the block is removed
        blockOpt = blockReader.read(1);
        assert (blockOpt.isEmpty());
    }
}
