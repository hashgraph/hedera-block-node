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

/** The block as file block stream manager. */
public class BlockAsFileBlockStreamManager implements BlockStreamManager {

    private static final System.Logger LOGGER =
            System.getLogger(BlockAsFileBlockStreamManager.class.getName());

    final String rootFolder;

    final List<Block> blockList = new ArrayList<>();

    int currentBlockIndex = 0;
    int currentBlockItemIndex = 0;
    int lastGivenBlockNumber = 0;

    /**
     * Constructor for the block as file block stream manager.
     *
     * @param blockStreamConfig the block stream config
     */
    @Inject
    public BlockAsFileBlockStreamManager(@NonNull BlockStreamConfig blockStreamConfig) {
        this.rootFolder = blockStreamConfig.folderRootPath();
        try {
            this.loadBlocks();
        } catch (IOException | ParseException | IllegalArgumentException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Error loading blocks", e);
            throw new RuntimeException(e);
        }

        LOGGER.log(System.Logger.Level.INFO, "Loaded " + blockList.size() + " blocks into memory");
    }

    @Override
    public GenerationMode getGenerationMode() {
        return GenerationMode.DIR;
    }

    @Override
    public BlockItem getNextBlockItem() {
        BlockItem nextBlockItem =
                blockList.get(currentBlockIndex).items().get(currentBlockItemIndex);
        currentBlockItemIndex++;
        if (currentBlockItemIndex >= blockList.get(currentBlockIndex).items().size()) {
            currentBlockItemIndex = 0;
            currentBlockIndex++;
        }
        return nextBlockItem;
    }

    @Override
    public Block getNextBlock() {
        Block nextBlock = blockList.get(currentBlockIndex);
        currentBlockIndex++;
        lastGivenBlockNumber++;
        if (currentBlockIndex >= blockList.size()) {
            currentBlockIndex = 0;
        }
        return nextBlock;
    }

    private void loadBlocks() throws IOException, ParseException {

        Path rootPath = Path.of(rootFolder);

        try (Stream<Path> blockFiles = Files.list(rootPath)) {

            List<Path> sortedBlockFiles =
                    blockFiles.sorted(Comparator.comparing(Path::getFileName)).toList();

            for (Path blockPath : sortedBlockFiles) {

                byte[] blockBytes;
                if (blockPath.toString().endsWith(".gz")) {
                    blockBytes = Utils.readGzFile(blockPath);
                } else if (blockPath.toString().endsWith(".blk")) {
                    blockBytes = Files.readAllBytes(blockPath);
                } else {
                    throw new IllegalArgumentException("Invalid file format: " + blockPath);
                }

                Block block = Block.PROTOBUF.parse(Bytes.wrap(blockBytes));
                blockList.add(block);
                LOGGER.log(System.Logger.Level.DEBUG, "Loaded block: " + blockPath);
            }
        }
    }
}
