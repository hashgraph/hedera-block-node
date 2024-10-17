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

package com.hedera.block.simulator.config.data;

import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.block.simulator.config.types.SimulatorMode;
import com.hedera.block.simulator.config.types.StreamingMode;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The BlockStreamConfig class defines the configuration data for the block stream.
 *
 * @param simulatorMode the working mode of the simulator
 * @param generationMode the mode of generation for the block stream
 * @param folderRootPath the root path of the folder containing the block stream
 * @param delayBetweenBlockItems the delay between block items
 * @param managerImplementation the implementation of the block stream manager
 * @param maxBlockItemsToStream the maximum number of block items to stream
 * @param paddedLength the padded length of 0 the block file format
 * @param fileExtension the file extension of the block file format
 * @param streamingMode the mode of streaming for the block stream
 * @param millisecondsPerBlock the milliseconds per block
 */
@ConfigData("blockStream")
public record BlockStreamConfig(
        @ConfigProperty(defaultValue = "PUBLISHER") SimulatorMode simulatorMode,
        @ConfigProperty(defaultValue = "DIR") GenerationMode generationMode,
        @ConfigProperty(defaultValue = "") String folderRootPath,
        @ConfigProperty(defaultValue = "1_500_000") int delayBetweenBlockItems,
        @ConfigProperty(defaultValue = "BlockAsFileBlockStreamManager")
                String managerImplementation,
        @ConfigProperty(defaultValue = "10_000") int maxBlockItemsToStream,
        @ConfigProperty(defaultValue = "36") int paddedLength,
        @ConfigProperty(defaultValue = ".blk.gz") String fileExtension,
        @ConfigProperty(defaultValue = "MILLIS_PER_BLOCK") StreamingMode streamingMode,
        @ConfigProperty(defaultValue = "1000") int millisecondsPerBlock) {

    /**
     * Constructor to set the default root path if not provided, it will be set to the data
     * directory in the current working directory
     */
    public BlockStreamConfig {
        // verify rootPath prop
        Path path = Path.of(folderRootPath);

        // if rootPath is empty, set it to the default data directory
        if (folderRootPath.isEmpty()) {
            path =
                    Paths.get(folderRootPath)
                            .toAbsolutePath()
                            .resolve("src/main/resources/block-0.0.3");
        }
        // Check if absolute
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException(folderRootPath + " Root path must be absolute");
        }
        // Check if the folder exists
        if (Files.notExists(path) && generationMode == GenerationMode.DIR) {
            throw new IllegalArgumentException("Folder does not exist: " + path);
        }

        folderRootPath = path.toString();
    }
}
