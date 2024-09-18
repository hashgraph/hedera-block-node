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

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneratorInjectionModuleTest {

    @Test
    void providesBlockStreamManager_AsFile() throws IOException {
        BlockStreamConfig blockStreamConfig =
                TestUtils.getTestConfiguration().getConfigData(BlockStreamConfig.class);

        BlockStreamManager blockStreamManager =
                GeneratorInjectionModule.providesBlockStreamManager(blockStreamConfig);

        assertEquals(
                blockStreamManager.getClass().getName(),
                BlockAsFileBlockStreamManager.class.getName());
    }

    @Test
    void providesBlockStreamManager_AsDir() throws IOException {
        BlockStreamConfig blockStreamConfig =
                TestUtils.getTestConfiguration(
                                Map.of(
                                        "blockStream.managerImplementation",
                                        "BlockAsDirBlockStreamManager"))
                        .getConfigData(BlockStreamConfig.class);

        BlockStreamManager blockStreamManager =
                GeneratorInjectionModule.providesBlockStreamManager(blockStreamConfig);

        assertEquals(
                blockStreamManager.getClass().getName(),
                BlockAsDirBlockStreamManager.class.getName());
    }

    @Test
    void providesBlockStreamManager_default() throws IOException {
        BlockStreamConfig blockStreamConfig =
                TestUtils.getTestConfiguration(Map.of("blockStream.managerImplementation", ""))
                        .getConfigData(BlockStreamConfig.class);

        BlockStreamManager blockStreamManager =
                GeneratorInjectionModule.providesBlockStreamManager(blockStreamConfig);

        assertEquals(
                blockStreamManager.getClass().getName(),
                BlockAsFileBlockStreamManager.class.getName());
    }
}
