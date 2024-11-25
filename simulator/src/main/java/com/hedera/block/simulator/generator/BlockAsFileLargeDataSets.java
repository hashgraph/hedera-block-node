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

import static com.hedera.block.simulator.Constants.GZ_EXTENSION;
import static com.hedera.block.simulator.Constants.RECORD_EXTENSION;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.common.utils.FileUtilities;
import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.hapi.block.stream.protoc.Block;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.inject.Inject;

/** A block stream manager that reads blocks from files in a directory. */
public class BlockAsFileLargeDataSets implements BlockStreamManager {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // State for getNextBlock()
    private final String blockStreamPath;
    private int currentBlockIndex;
    private final int endBlockNumber;

    // State for getNextBlockItem()
    private int currentBlockItemIndex;
    private Block currentBlock;
    private final String formatString;

    /**
     * Constructs a new BlockAsFileLargeDataSets instance.
     *
     * @param config the block stream configuration
     */
    @Inject
    public BlockAsFileLargeDataSets(@NonNull BlockGeneratorConfig config) {

        this.blockStreamPath = config.folderRootPath();
        this.endBlockNumber = config.endBlockNumber();

        // Override if startBlockNumber is set
        this.currentBlockIndex = (config.startBlockNumber() > 1) ? config.startBlockNumber() : 1;

        this.formatString = "%0" + config.paddedLength() + "d" + config.fileExtension();
    }

    /**
     * Initialize the block stream manager and load blocks into memory.
     */
    @Override
    public void init() {
        // Do nothing, because we don't have real initializing and loading blocks into memory for this implementation.
    }

    @Override
    public GenerationMode getGenerationMode() {
        return GenerationMode.DIR;
    }

    @Override
    public BlockItem getNextBlockItem() throws IOException, BlockSimulatorParsingException {
        if (currentBlock != null && currentBlock.getItemsList().size() > currentBlockItemIndex) {
            return currentBlock.getItemsList().get(currentBlockItemIndex++);
        } else {
            currentBlock = getNextBlock();
            if (currentBlock != null) {
                currentBlockItemIndex = 0; // Reset for new block
                return getNextBlockItem();
            }
        }

        return null; // No more blocks/items
    }

    @Override
    public Block getNextBlock() throws IOException, BlockSimulatorParsingException {

        // If endBlockNumber is set, evaluate if we've exceeded the
        // range. If so, then return null.
        if (endBlockNumber > 0 && currentBlockIndex > endBlockNumber) {
            return null;
        }

        final String nextBlockFileName = String.format(formatString, currentBlockIndex);
        final Path localBlockStreamPath = Path.of(blockStreamPath).resolve(nextBlockFileName);
        if (!Files.exists(localBlockStreamPath)) {
            return null;
        }
        final byte[] blockBytes =
                FileUtilities.readFileBytesUnsafe(localBlockStreamPath, RECORD_EXTENSION, GZ_EXTENSION);

        if (Objects.isNull(blockBytes)) {
            throw new NullPointerException(
                    "Unable to read block file [%s]! Most likely not found with the extensions '%s' or '%s'"
                            .formatted(localBlockStreamPath, RECORD_EXTENSION, GZ_EXTENSION));
        }

        LOGGER.log(INFO, "Loading block: " + localBlockStreamPath.getFileName());

        final Block block = Block.parseFrom(blockBytes);
        LOGGER.log(INFO, "block loaded with items size= " + block.getItemsList().size());

        currentBlockIndex++;

        return block;
    }
}
