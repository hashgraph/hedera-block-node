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
import com.swirlds.config.api.validation.annotation.Min;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Defines the configuration for the BlockStreamManager (Generator) of blocks in the Hedera Block Simulator.
 *
 * @param generationMode the mode of block generation (e.g., directory-based)
 * @param folderRootPath the root path of the folder containing block files
 * @param managerImplementation the implementation class name of the block stream manager
 * @param paddedLength the length to which block identifiers are padded
 * @param fileExtension the file extension used for block files
 * @param startBlockNumber the optional start block number for the BlockAsFileLargeDataSets manager
 * @param endBlockNumber the optional end block number for the BlockAsFileLargeDataSets manager
 */
@ConfigData("generator")
public record BlockGeneratorConfig(
        @ConfigProperty(defaultValue = "DIR") GenerationMode generationMode,
        @ConfigProperty(defaultValue = "") String folderRootPath,
        @ConfigProperty(defaultValue = "BlockAsFileBlockStreamManager") String managerImplementation,
        @ConfigProperty(defaultValue = "36") int paddedLength,
        @ConfigProperty(defaultValue = ".blk.gz") String fileExtension,
        // Optional block number range for the BlockAsFileLargeDataSets manager
        @ConfigProperty(defaultValue = "1") @Min(1) int startBlockNumber,
        @ConfigProperty(defaultValue = "-1") int endBlockNumber) {

    /**
     * Constructs a new {@code BlockGeneratorConfig} instance with validation.
     *
     * @throws IllegalArgumentException if the folder root path is not absolute or the folder does not exist
     */
    public BlockGeneratorConfig {
        // Verify folderRootPath property
        Path path = Path.of(folderRootPath);

        // If folderRootPath is empty, set it to the default data directory
        if (folderRootPath.isEmpty()) {
            path = Paths.get("").toAbsolutePath().resolve("src/main/resources/block-0.0.3");
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

        if (endBlockNumber > -1 && endBlockNumber < startBlockNumber) {
            throw new IllegalArgumentException("endBlockNumber must be greater than or equal to startBlockNumber");
        }
    }

    /**
     * Creates a new {@link Builder} for constructing a {@code BlockGeneratorConfig}.
     *
     * @return a new {@code Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for creating instances of {@link BlockGeneratorConfig}.
     */
    public static class Builder {
        private GenerationMode generationMode = GenerationMode.DIR;
        private String folderRootPath = "";
        private String managerImplementation = "BlockAsFileBlockStreamManager";
        private int paddedLength = 36;
        private String fileExtension = ".blk.gz";
        private int startBlockNumber;
        private int endBlockNumber;

        /**
         * Creates a new instance of the {@code Builder} class with default configuration values.
         */
        public Builder() {
            // Default constructor
        }

        /**
         * Sets the generation mode for block generation.
         *
         * @param generationMode the {@link GenerationMode} to use
         * @return this {@code Builder} instance
         */
        public Builder generationMode(GenerationMode generationMode) {
            this.generationMode = generationMode;
            return this;
        }

        /**
         * Sets the root path of the folder containing block files.
         *
         * @param folderRootPath the absolute path to the folder
         * @return this {@code Builder} instance
         */
        public Builder folderRootPath(String folderRootPath) {
            this.folderRootPath = folderRootPath;
            return this;
        }

        /**
         * Sets the implementation class name of the block stream manager.
         *
         * @param managerImplementation the class name of the manager implementation
         * @return this {@code Builder} instance
         */
        public Builder managerImplementation(String managerImplementation) {
            this.managerImplementation = managerImplementation;
            return this;
        }

        /**
         * Sets the length to which block identifiers are padded.
         *
         * @param paddedLength the padded length
         * @return this {@code Builder} instance
         */
        public Builder paddedLength(int paddedLength) {
            this.paddedLength = paddedLength;
            return this;
        }

        /**
         * Sets the file extension used for block files.
         *
         * @param fileExtension the file extension (e.g., ".blk.gz")
         * @return this {@code Builder} instance
         */
        public Builder fileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
            return this;
        }

        /**
         * Sets the start block number for the BlockAsFileLargeDataSets manager to
         * begin reading blocks.
         *
         * @param startBlockNumber the start block number
         * @return this {@code Builder} instance
         */
        public Builder startBlockNumber(int startBlockNumber) {
            this.startBlockNumber = startBlockNumber;
            return this;
        }

        /**
         * Sets the end block number for the BlockAsFileLargeDataSets manager to
         * stop reading blocks.
         *
         * @param endBlockNumber the end block number
         * @return this {@code Builder} instance
         */
        public Builder endBlockNumber(int endBlockNumber) {
            this.endBlockNumber = endBlockNumber;
            return this;
        }

        /**
         * Builds a new {@link BlockGeneratorConfig} instance with the configured values.
         *
         * @return a new {@code BlockGeneratorConfig}
         */
        public BlockGeneratorConfig build() {
            return new BlockGeneratorConfig(
                    generationMode,
                    folderRootPath,
                    managerImplementation,
                    paddedLength,
                    fileExtension,
                    startBlockNumber,
                    endBlockNumber);
        }
    }
}
