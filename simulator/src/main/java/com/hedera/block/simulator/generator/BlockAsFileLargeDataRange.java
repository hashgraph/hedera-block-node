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
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
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

public class BlockAsFileLargeDataRange implements BlockStreamManager {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final String blockStreamPath;
    private final int startBlockIndex = 243;
    private int currentBlockIndex = startBlockIndex;
    private final int endBlockIndex = 245;

    private int currentBlockItemIndex;

    private Block currentBlock;
    private final String formatString;

    /**
     * Constructs a new BlockAsFileLargeDataSets instance.
     *
     * @param config the block stream configuration
     */
    @Inject
    public BlockAsFileLargeDataRange(@NonNull BlockStreamConfig config) {
        this.blockStreamPath = config.folderRootPath();
        this.formatString = "%0" + config.paddedLength() + "d" + config.fileExtension();
    }

    @Override
    public GenerationMode getGenerationMode() {
        return GenerationMode.DIR;
    }

    @Override
    public BlockItem getNextBlockItem() throws IOException, BlockSimulatorParsingException {
        // Not implemented
        return null;
    }

    @Override
    public Block getNextBlock() throws IOException, BlockSimulatorParsingException {

        if (currentBlockIndex > endBlockIndex) {
            return null;
        }

        String nextBlockFileName = String.format(formatString, currentBlockIndex);
        File blockFile = new File(blockStreamPath, nextBlockFileName);

        if (!blockFile.exists()) {
            return null;
        }

        try {
            LOGGER.log(INFO, "Loading block: " + blockFile.getName());

            byte[] blockBytes = readFileBytes(blockFile.toPath());
            Block block = Block.PROTOBUF.parse(Bytes.wrap(blockBytes));

            LOGGER.log(INFO, "Block loaded with items size= " + block.items().size());

            currentBlockIndex++;
            return block;
        } catch (ParseException e) {
            throw new BlockSimulatorParsingException(e.getMessage());
        }
    }
}
