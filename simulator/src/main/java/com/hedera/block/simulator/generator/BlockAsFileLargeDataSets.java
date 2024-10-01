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
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;

public class BlockAsFileLargeDataSets implements BlockStreamManager {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final String blockstreamPath;
    private int currentBlockIndex = 0;
    private int currentBlockItemIndex = 0;

    private Block currentBlock = null;
    int paddedLength;
    String fileExtension;

    public BlockAsFileLargeDataSets(@NonNull BlockStreamConfig config) {
        this.blockstreamPath = config.folderRootPath();
        this.paddedLength = config.paddedLength();
        this.fileExtension = config.fileExtension();
    }

    @Override
    public GenerationMode getGenerationMode() {
        return GenerationMode.DIR;
    }

    @Override
    public BlockItem getNextBlockItem() throws IOException, ParseException {
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
    public Block getNextBlock() throws IOException, ParseException {
        currentBlockIndex++;

        String formatString = "%0" + paddedLength + "d" + fileExtension;
        String nextBlockFileName = String.format(formatString, currentBlockIndex);
        File blockFile = new File(blockstreamPath, nextBlockFileName);

        if (blockFile.exists()) {
            byte[] blockBytes = readFileBytes(blockFile.toPath());

            LOGGER.log(INFO, "Loading block: " + blockFile.getName());

            Block block = Block.PROTOBUF.parse(Bytes.wrap(blockBytes));
            LOGGER.log(INFO, "block loaded with items size= " + block.items().size());
            return block;
        }

        return null; // No more blocks found
    }
}
