// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.generator;

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneratorInjectionModuleTest {

    @Test
    void providesBlockStreamManager_AsFileLargeDataSets() throws IOException {

        BlockGeneratorConfig blockGeneratorConfig = TestUtils.getTestConfiguration(
                        Map.of("generator.managerImplementation", "BlockAsFileLargeDataSets"))
                .getConfigData(BlockGeneratorConfig.class);

        BlockStreamManager blockStreamManager =
                GeneratorInjectionModule.providesBlockStreamManager(blockGeneratorConfig);

        assertEquals(blockStreamManager.getClass().getName(), BlockAsFileLargeDataSets.class.getName());
    }

    @Test
    void providesBlockStreamManager_AsFile() throws IOException {
        BlockGeneratorConfig blockGeneratorConfig = TestUtils.getTestConfiguration(Map.of(
                        "generator.managerImplementation",
                        "BlockAsFileBlockStreamManager",
                        "generator.folderRootPath",
                        ""))
                .getConfigData(BlockGeneratorConfig.class);

        BlockStreamManager blockStreamManager =
                GeneratorInjectionModule.providesBlockStreamManager(blockGeneratorConfig);

        assertEquals(blockStreamManager.getClass().getName(), BlockAsFileBlockStreamManager.class.getName());
    }

    @Test
    void providesBlockStreamManager_AsDir() throws IOException {
        BlockGeneratorConfig blockGeneratorConfig = TestUtils.getTestConfiguration(
                        Map.of("generator.managerImplementation", "BlockAsDirBlockStreamManager"))
                .getConfigData(BlockGeneratorConfig.class);

        BlockStreamManager blockStreamManager =
                GeneratorInjectionModule.providesBlockStreamManager(blockGeneratorConfig);

        assertEquals(
                BlockAsDirBlockStreamManager.class.getName(),
                blockStreamManager.getClass().getName());
    }

    @Test
    void providesBlockStreamManager_default() throws IOException {
        BlockGeneratorConfig blockGeneratorConfig = TestUtils.getTestConfiguration(
                        Map.of("generator.managerImplementation", "", "generator.folderRootPath", ""))
                .getConfigData(BlockGeneratorConfig.class);

        BlockStreamManager blockStreamManager =
                GeneratorInjectionModule.providesBlockStreamManager(blockGeneratorConfig);

        assertEquals(blockStreamManager.getClass().getName(), BlockAsFileBlockStreamManager.class.getName());
    }
}
