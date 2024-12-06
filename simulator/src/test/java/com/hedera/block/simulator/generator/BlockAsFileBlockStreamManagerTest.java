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

package com.hedera.block.simulator.generator;

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockAsFileBlockStreamManagerTest {

    private final String gzRootFolder = "src/main/resources/block-0.0.3/";
    private BlockStreamManager blockStreamManager;

    private String getAbsoluteFolder(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath().toString();
    }

    @BeforeEach
    void setUp() {
        blockStreamManager = getBlockAsFileBlockStreamManager(getAbsoluteFolder(gzRootFolder));
        blockStreamManager.init();
    }

    @Test
    void getGenerationMode() {
        assertEquals(GenerationMode.DIR, blockStreamManager.getGenerationMode());
    }

    @Test
    void getNextBlock() throws IOException, BlockSimulatorParsingException {
        for (int i = 0; i < 3000; i++) {
            assertNotNull(blockStreamManager.getNextBlock());
        }
    }

    @Test
    void getNextBlockItem() throws IOException, BlockSimulatorParsingException {
        for (int i = 0; i < 35000; i++) {
            assertNotNull(blockStreamManager.getNextBlockItem());
        }
    }

    @Test
    void loadBlockBlk() throws IOException, BlockSimulatorParsingException {
        String blkRootFolder = "src/test/resources/block-0.0.3-blk/";
        BlockStreamManager blockStreamManager = getBlockAsFileBlockStreamManager(getAbsoluteFolder(blkRootFolder));
        blockStreamManager.init();

        assertNotNull(blockStreamManager.getNextBlock());
    }

    @Test
    void BlockAsFileBlockStreamManagerInvalidRootPath() {
        assertThrows(
                RuntimeException.class,
                () -> getBlockAsFileBlockStreamManager(getAbsoluteFolder("src/test/resources/BlockAsDirException/1/")));
    }

    private BlockStreamManager getBlockAsFileBlockStreamManager(String rootFolder) {
        BlockGeneratorConfig blockGeneratorConfig = BlockGeneratorConfig.builder()
                .generationMode(GenerationMode.DIR)
                .folderRootPath(rootFolder)
                .managerImplementation("BlockAsFileBlockStreamManager")
                .paddedLength(36)
                .fileExtension(".blk")
                .build();

        BlockStreamManager blockStreamManager = new BlockAsFileBlockStreamManager(blockGeneratorConfig);
        blockStreamManager.init();
        return blockStreamManager;
    }
}
