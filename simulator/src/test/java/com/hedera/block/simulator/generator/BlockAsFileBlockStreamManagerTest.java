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
import org.junit.jupiter.api.Test;

class BlockAsFileBlockStreamManagerTest {

    private final String gzRootFolder =
            "/Users/user/Projects/hedera-block-node/simulator/src/main/resources/block-0.0.3/";

    @Test
    void getGenerationMode() {
        BlockStreamManager blockStreamManager = getBlockAsFileBlockStreamManager(gzRootFolder);
        assertEquals(GenerationMode.DIR, blockStreamManager.getGenerationMode());
    }

    @Test
    void BlockAsFileBlockStreamManagerInvalidRootPath() {
        assertThrows(RuntimeException.class, () -> getBlockAsFileBlockStreamManager("/etc"));
    }

    @Test
    void getNextBlock() {
        BlockStreamManager blockStreamManager = getBlockAsFileBlockStreamManager(gzRootFolder);
        for (int i = 0; i < 3000; i++) {
            assertNotNull(blockStreamManager.getNextBlock());
        }
    }

    @Test
    void getNextBlockItem() {
        BlockStreamManager blockStreamManager = getBlockAsFileBlockStreamManager(gzRootFolder);
        for (int i = 0; i < 1000; i++) {
            assertNotNull(blockStreamManager.getNextBlockItem());
        }
    }

    @Test
    void loadBlockBlk() {
        String blkRootFolder =
                "/Users/user/Projects/hedera-block-node/simulator/src/main/resources/block-0.0.3-blk/";
        BlockStreamManager blockStreamManager = getBlockAsFileBlockStreamManager(blkRootFolder);
        assertNotNull(blockStreamManager.getNextBlock());
    }

    private BlockAsFileBlockStreamManager getBlockAsFileBlockStreamManager(String rootFolder) {
        BlockStreamConfig blockStreamConfig = new BlockStreamConfig(GenerationMode.DIR, rootFolder);
        return new BlockAsFileBlockStreamManager(blockStreamConfig);
    }
}
