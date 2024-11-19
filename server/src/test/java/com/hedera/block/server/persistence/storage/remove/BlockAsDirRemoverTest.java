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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.mockito.Mockito.mock;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.PersistTestUtils;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BlockAsDirRemoverTest {
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;

    @TempDir
    private Path testPath;

    @BeforeEach
    public void setUp() throws IOException {
        blockNodeContext =
                TestConfigUtil.getTestBlockNodeContext(Map.of("persistence.storage.rootPath", testPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    }

    @Test
    public void testRemoveNonExistentBlock() throws IOException, ParseException {
        // Write a block
        final List<BlockItem> blockItems = PersistTestUtils.generateBlockItems(1);

        final BlockWriter<List<BlockItem>> blockWriter = BlockAsDirWriterBuilder.newBuilder(
                        blockNodeContext, mock(BlockRemover.class))
                .build();
        for (final BlockItem blockItem : blockItems) {
            blockWriter.write(List.of(blockItem));
        }

        // Remove a block that does not exist
        final BlockRemover toTest = new BlockAsDirRemover(testPath);
        toTest.remove(2);

        // Verify the block was not removed
        final BlockReader<Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(testConfig).build();
        final Optional<Block> before = blockReader.read(1);
        assertThat(before)
                .isNotNull()
                .isPresent()
                .get()
                .returns(
                        blockItems.getFirst().blockHeader(),
                        from(block -> block.items().getFirst().blockHeader()));

        // Now remove the block
        toTest.remove(1);

        // Verify the block is removed
        final Optional<Block> after = blockReader.read(1);
        assertThat(after).isNotNull().isEmpty();
    }
}
