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

import static com.hedera.block.common.utils.FileUtilities.readFileBytesUnsafe;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;

/** A block stream manager that reads blocks from files in a directory. */
public class BlockAsFileLargeDataSets implements BlockStreamManager {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final String blockstreamPath;
    private int currentBlockIndex = 0;
    private int currentBlockItemIndex = 0;

    private Block currentBlock = null;
    private final String formatString;

    /**
     * Constructs a new BlockAsFileLargeDataSets instance.
     *
     * @param config the block stream configuration
     */
    @Inject
    public BlockAsFileLargeDataSets(@NonNull BlockGeneratorConfig config) {
        this.blockstreamPath = config.folderRootPath();
        this.formatString = "%0" + config.paddedLength() + "d" + config.fileExtension();
    }

    @Override
    public GenerationMode getGenerationMode() {
        return GenerationMode.DIR;
    }

    @Override
    public BlockItem getNextBlockItem() throws IOException, BlockSimulatorParsingException {
        if (currentBlock != null && currentBlock.items().size() > currentBlockItemIndex) {
            return currentBlock.items().get(currentBlockItemIndex++);
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
        currentBlockIndex++;

        final String nextBlockFileName = String.format(formatString, currentBlockIndex);
        final File blockFile = new File(blockstreamPath, nextBlockFileName);

        if (!blockFile.exists()) {
            return null;
        }

        try {
            final byte[] blockBytes = readFileBytesUnsafe(blockFile);

            LOGGER.log(INFO, "Loading block: " + blockFile.getName());

            final Block block = Block.PROTOBUF.parse(Bytes.wrap(blockBytes));
            LOGGER.log(INFO, "block loaded with items size= " + block.items().size());
            return block;
        } catch (final ParseException e) {
            throw new BlockSimulatorParsingException(e.getMessage());
        }
    }
}
