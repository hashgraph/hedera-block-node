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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.block.simulator.exception.BlockSimulatorException;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class BlockAsDirBlockStreamManagerTest {

    private final String rootFolder = "src/test/resources/blockAsDirExample/";

    private String getAbsoluteFolder(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath().toString();
    }

    @Test
    void getGenerationMode() {
        BlockStreamManager blockStreamManager =
                getBlockAsDirBlockStreamManager(getAbsoluteFolder(rootFolder));
        assertEquals(GenerationMode.DIR, blockStreamManager.getGenerationMode());

        assertEquals(GenerationMode.DIR, blockStreamManager.getGenerationMode());
    }

    @Test
    void getNextBlockItem() throws BlockSimulatorParsingException, BlockSimulatorException {
        BlockStreamManager blockStreamManager =
                getBlockAsDirBlockStreamManager(getAbsoluteFolder(rootFolder));

        for (int i = 0; i < 1000; i++) {
            assertNotNull(blockStreamManager.getNextBlockItem());
        }
    }

    @Test
    void getNextBlock() throws BlockSimulatorParsingException, BlockSimulatorException {
        BlockStreamManager blockStreamManager =
                getBlockAsDirBlockStreamManager(getAbsoluteFolder(rootFolder));

        for (int i = 0; i < 3000; i++) {
            assertNotNull(blockStreamManager.getNextBlock());
        }
    }

    @Test
    void BlockAsFileBlockStreamManagerInvalidRootPath() {
        assertThrows(
                RuntimeException.class,
                () ->
                        getBlockAsDirBlockStreamManager(
                                getAbsoluteFolder("src/test/resources/BlockAsDirException/")));
    }

    private BlockStreamManager getBlockAsDirBlockStreamManager(String rootFolder) {
        BlockStreamConfig blockStreamConfig =
                new BlockStreamConfig(
                        GenerationMode.DIR,
                        rootFolder,
                        1_500_000,
                        "BlockAsDirBlockStreamManager",
                        10_000,
                        36,
                        ".blk");
        return new BlockAsDirBlockStreamManager(blockStreamConfig);
    }
}
