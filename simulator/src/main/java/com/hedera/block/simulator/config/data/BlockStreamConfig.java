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
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The BlockStreamConfig class defines the configuration data for the block stream.
 *
 * @param generationMode the mode of generation for the block stream
 * @param folderRootPath the root path of the folder containing the block stream
 */
@ConfigData("blockStream")
public record BlockStreamConfig(
        @ConfigProperty(defaultValue = "DIR") GenerationMode generationMode,
        @ConfigProperty(defaultValue = "") String folderRootPath) {

    /**
     * Constructor to set the default root path if not provided, it will be set to the data
     * directory in the current working directory
     */
    public BlockStreamConfig {
        // verify rootPath prop
        Path path = Path.of(folderRootPath);

        // if rootPath is empty, set it to the default data directory
        // default data directory is the "src/main/resources/blocks" directory
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
