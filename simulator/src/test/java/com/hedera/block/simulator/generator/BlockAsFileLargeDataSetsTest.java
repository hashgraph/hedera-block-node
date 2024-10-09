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

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.block.simulator.exception.BlockSimulatorException;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.hapi.block.stream.BlockItem;
import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockAsFileLargeDataSetsTest {

    private static final String rootFolder = "src/test/resources/block-0.0.3-blk/";
    private static int filesInFolder;

    @BeforeAll
    static void setUp() {
        filesInFolder = getFilesInFolder(getAbsoluteFolder(rootFolder));
    }

    @AfterEach
    void tearDown() {}

    @Test
    void getGenerationMode() {
        BlockStreamManager blockStreamManager =
                getBlockAsFileLargeDatasetsBlockStreamManager(getAbsoluteFolder(rootFolder));

        assertEquals(GenerationMode.DIR, blockStreamManager.getGenerationMode());
    }

    @Test
    void getNextBlock() throws BlockSimulatorParsingException, BlockSimulatorException {
        BlockStreamManager blockStreamManager =
                getBlockAsFileLargeDatasetsBlockStreamManager(getAbsoluteFolder(rootFolder));
        for (int i = 0; i < filesInFolder; i++) {
            assertNotNull(blockStreamManager.getNextBlock());
        }

        assertNull(blockStreamManager.getNextBlock());
    }

    @Test
    void getNextBlockItem() throws BlockSimulatorParsingException, BlockSimulatorException {
        BlockStreamManager blockStreamManager =
                getBlockAsFileLargeDatasetsBlockStreamManager(getAbsoluteFolder(rootFolder));

        while (true) {
            BlockItem blockItem = blockStreamManager.getNextBlockItem();
            if (blockItem == null) {
                break;
            }
            assertNotNull(blockItem);
        }
    }

    private BlockAsFileLargeDataSets getBlockAsFileLargeDatasetsBlockStreamManager(
            String rootFolder) {
        BlockStreamConfig blockStreamConfig =
                new BlockStreamConfig(
                        GenerationMode.DIR,
                        rootFolder,
                        1_500_000,
                        "BlockAsFileBlockStreamManager",
                        10_000,
                        36,
                        ".blk");
        return new BlockAsFileLargeDataSets(blockStreamConfig);
    }

    private static String getAbsoluteFolder(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath().toString();
    }

    private static int getFilesInFolder(String absolutePath) {
        File folder = new File(absolutePath);
        File[] blkFiles =
                folder.listFiles(
                        file ->
                                file.isFile()
                                        && (file.getName().endsWith(".blk")
                                                || file.getName().endsWith(".blk.gz")));
        return blkFiles.length;
    }
}
