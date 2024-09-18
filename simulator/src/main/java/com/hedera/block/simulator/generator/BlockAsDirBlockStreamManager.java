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

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;

/**
 * The BlockAsDirBlockStreamManager class implements the BlockStreamManager interface to manage the
 * block stream from a directory.
 */
public class BlockAsDirBlockStreamManager implements BlockStreamManager {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    final String rootFolder;

    final List<Block> blocks = new ArrayList<>();

    int currentBlockIndex = 0;
    int currentBlockItemIndex = 0;
    int lastGivenBlockNumber = 0;

    /**
     * Constructor to initialize the BlockAsDirBlockStreamManager with the block stream
     * configuration.
     *
     * @param blockStreamConfig the block stream configuration
     */
    @Inject
    public BlockAsDirBlockStreamManager(@NonNull BlockStreamConfig blockStreamConfig) {
        this.rootFolder = blockStreamConfig.folderRootPath();
        try {
            this.loadBlocks();
        } catch (IOException | ParseException | IllegalArgumentException e) {
            LOGGER.log(ERROR, "Error loading blocks", e);
            throw new RuntimeException(e);
        }

        LOGGER.log(INFO, "Loaded " + blocks.size() + " blocks into memory");
    }

    /** Generation Mode of the implementation */
    @Override
    public GenerationMode getGenerationMode() {
        return GenerationMode.DIR;
    }

    /** gets the next block item from the manager */
    @Override
    public BlockItem getNextBlockItem() {
        BlockItem nextBlockItem = blocks.get(currentBlockIndex).items().get(currentBlockItemIndex);
        currentBlockItemIndex++;
        if (currentBlockItemIndex >= blocks.get(currentBlockIndex).items().size()) {
            currentBlockItemIndex = 0;
            currentBlockIndex++;
            if (currentBlockIndex >= blocks.size()) {
                currentBlockIndex = 0;
            }
        }
        return nextBlockItem;
    }

    /** gets the next block from the manager */
    @Override
    public Block getNextBlock() {
        Block nextBlock = blocks.get(currentBlockIndex);
        currentBlockIndex++;
        lastGivenBlockNumber++;
        if (currentBlockIndex >= blocks.size()) {
            currentBlockIndex = 0;
        }
        return nextBlock;
    }

    private void loadBlocks() throws IOException, ParseException {
        Path rootPath = Path.of(rootFolder);

        try (Stream<Path> blockDirs = Files.list(rootPath).filter(Files::isDirectory)) {
            List<Path> sortedBlockDirs =
                    blockDirs.sorted(Comparator.comparing(Path::getFileName)).toList();

            for (Path blockDirPath : sortedBlockDirs) {
                List<BlockItem> parsedBlockItems = new ArrayList<>();

                try (Stream<Path> blockItems =
                        Files.list(blockDirPath).filter(Files::isRegularFile)) {
                    List<Path> sortedBlockItems =
                            blockItems
                                    .sorted(
                                            Comparator.comparing(
                                                    BlockAsDirBlockStreamManager
                                                            ::extractNumberFromPath))
                                    .toList();

                    for (Path pathBlockItem : sortedBlockItems) {
                        byte[] blockItemBytes = readBlockItemBytes(pathBlockItem);
                        // if null means the file is not a block item and we can skip the file.
                        if (blockItemBytes == null) {
                            continue;
                        }
                        BlockItem blockItem = BlockItem.PROTOBUF.parse(Bytes.wrap(blockItemBytes));
                        parsedBlockItems.add(blockItem);
                    }
                }

                blocks.add(Block.newBuilder().items(parsedBlockItems).build());
                LOGGER.log(DEBUG, "Loaded block: " + blockDirPath);
            }
        }
    }

    private byte[] readBlockItemBytes(Path pathBlockItem) throws IOException {
        if (pathBlockItem.toString().endsWith(".gz")) {
            return Utils.readGzFile(pathBlockItem);
        } else if (pathBlockItem.toString().endsWith(".blk")) {
            return Files.readAllBytes(pathBlockItem);
        }
        return null;
    }

    // Method to extract the numeric part of the filename from a Path object
    // Returns -1 if the filename is not a valid number
    private static int extractNumberFromPath(Path path) {
        String filename = path.getFileName().toString();
        String numPart = filename.split("\\.")[0]; // Get the part before the first dot
        try {
            return Integer.parseInt(numPart);
        } catch (NumberFormatException e) {
            return -1; // Return -1 if parsing fails
        }
    }
}
