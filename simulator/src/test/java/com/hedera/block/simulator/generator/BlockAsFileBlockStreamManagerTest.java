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
import com.hedera.block.simulator.config.types.SimulatorMode;
import com.hedera.block.simulator.config.types.StreamingMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class BlockAsFileBlockStreamManagerTest {

    private final String gzRootFolder = "src/main/resources/block-0.0.3/";
    private final SimulatorMode simulatorMode = SimulatorMode.PUBLISHER;

    private String getAbsoluteFolder(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath().toString();
    }

    @Test
    void getGenerationMode() {
        BlockStreamManager blockStreamManager =
                getBlockAsFileBlockStreamManager(getAbsoluteFolder(gzRootFolder));
        assertEquals(GenerationMode.DIR, blockStreamManager.getGenerationMode());
    }

    @Test
    void getNextBlock() throws IOException, BlockSimulatorParsingException {
        BlockStreamManager blockStreamManager =
                getBlockAsFileBlockStreamManager(getAbsoluteFolder(gzRootFolder));
        for (int i = 0; i < 3000; i++) {
            assertNotNull(blockStreamManager.getNextBlock());
        }
    }

    @Test
    void getNextBlockItem() throws IOException, BlockSimulatorParsingException {
        BlockStreamManager blockStreamManager =
                getBlockAsFileBlockStreamManager(getAbsoluteFolder(gzRootFolder));
        for (int i = 0; i < 35000; i++) {
            assertNotNull(blockStreamManager.getNextBlockItem());
        }
    }

    @Test
    void loadBlockBlk() throws IOException, BlockSimulatorParsingException {
        String blkRootFolder = "src/test/resources/block-0.0.3-blk/";
        BlockStreamManager blockStreamManager =
                getBlockAsFileBlockStreamManager(getAbsoluteFolder(blkRootFolder));
        assertNotNull(blockStreamManager.getNextBlock());
    }

    @Test
    void BlockAsFileBlockStreamManagerInvalidRootPath() {
        assertThrows(
                RuntimeException.class,
                () ->
                        getBlockAsFileBlockStreamManager(
                                getAbsoluteFolder("src/test/resources/BlockAsDirException/1/")));
    }

    private BlockAsFileBlockStreamManager getBlockAsFileBlockStreamManager(String rootFolder) {
        BlockStreamConfig blockStreamConfig =
                new BlockStreamConfig(
                        simulatorMode,
                        GenerationMode.DIR,
                        rootFolder,
                        1_500_000,
                        "BlockAsFileBlockStreamManager",
                        10_000,
                        36,
                        ".blk",
                        StreamingMode.CONSTANT_RATE,
                        1000);
        return new BlockAsFileBlockStreamManager(blockStreamConfig);
    }
}
