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
import com.hedera.hapi.block.stream.BlockItem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlockAsFileLargeDataSetsTest {

    private static final String rootFolder = "src/test/resources/block-0.0.3-blk/";
    private final SimulatorMode simulatorMode = SimulatorMode.PUBLISHER;
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
    void getNextBlock() throws IOException, BlockSimulatorParsingException {
        BlockStreamManager blockStreamManager =
                getBlockAsFileLargeDatasetsBlockStreamManager(getAbsoluteFolder(rootFolder));
        for (int i = 0; i < filesInFolder; i++) {
            assertNotNull(blockStreamManager.getNextBlock());
        }

        assertNull(blockStreamManager.getNextBlock());
    }

    @Test
    void getNextBlockItem() throws IOException, BlockSimulatorParsingException {
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

    @Test
    void gettingNextBlockItemThrowsParsingException(@TempDir Path tempDir) throws IOException {
        String blockFolderName = "block-0.0.3-blk";
        Path blockDirPath = tempDir.resolve(blockFolderName);
        Files.createDirectories(blockDirPath);

        int nextBlockIndex = 1;
        int paddedLength = 36;
        String fileExtension = ".blk";
        String formatString = "%0" + paddedLength + "d" + fileExtension;

        String currentBlockFileName = String.format(formatString, nextBlockIndex);
        Path currentBlockFilePath = blockDirPath.resolve(currentBlockFileName);

        byte[] invalidData = "invalid block data".getBytes();
        Files.write(currentBlockFilePath, invalidData);

        BlockStreamConfig blockStreamConfig =
                new BlockStreamConfig(
                        simulatorMode,
                        GenerationMode.DIR,
                        blockDirPath.toString(),
                        1_500_000,
                        "BlockAsFileBlockStreamManager",
                        10_000,
                        36,
                        ".blk",
                        StreamingMode.CONSTANT_RATE,
                        1000);
        BlockAsFileLargeDataSets blockStreamManager =
                new BlockAsFileLargeDataSets(blockStreamConfig);

        assertThrows(
                BlockSimulatorParsingException.class,
                blockStreamManager::getNextBlock,
                "Expected getNextBlock() to throw BlockSimulatorParsingException");
    }

    private BlockAsFileLargeDataSets getBlockAsFileLargeDatasetsBlockStreamManager(
            String rootFolder) {
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
