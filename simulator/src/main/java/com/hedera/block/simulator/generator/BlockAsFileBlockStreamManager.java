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

import static com.hedera.block.simulator.generator.Utils.readFileBytes;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
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

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    final String rootFolder;

    final List<Block> blocks = new ArrayList<>();

    int currentBlockIndex = 0;
    int currentBlockItemIndex = 0;
    int lastGivenBlockNumber = 0;

    /**
     * Constructor for the block as file block stream manager.
     *
     * @param blockStreamConfig the block stream config
     */
    @Inject
    public BlockAsFileBlockStreamManager(@NonNull BlockGeneratorConfig blockStreamConfig) {
        this.rootFolder = blockStreamConfig.folderRootPath();
        try {
            this.loadBlocks();
        } catch (IOException | ParseException | IllegalArgumentException e) {
            LOGGER.log(ERROR, "Error loading blocks", e);
            throw new RuntimeException(e);
        }

        LOGGER.log(INFO, "Loaded " + blocks.size() + " blocks into memory");
    }

    @Override
    public GenerationMode getGenerationMode() {
        return GenerationMode.DIR;
    }

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

        try (Stream<Path> blockFiles = Files.list(rootPath)) {

            List<Path> sortedBlockFiles =
                    blockFiles.sorted(Comparator.comparing(Path::getFileName)).toList();

            for (Path blockPath : sortedBlockFiles) {

                byte[] blockBytes = readFileBytes(blockPath);
                // skip if block is null, usually due to SO files like .DS_STORE
                if (blockBytes == null) {
                    continue;
                }

                Block block = Block.PROTOBUF.parse(Bytes.wrap(blockBytes));
                blocks.add(block);
                LOGGER.log(DEBUG, "Loaded block: " + blockPath);
            }
        }
    }
}
