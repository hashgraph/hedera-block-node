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

import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.input.EventHeader;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.hapi.platform.event.EventCore;

public class DynamicBlockItemGenerator implements BlockStreamManager {

    private long totalBlockItems;
    private long blockItemCount = 1;
    private long blockCount;

    private final long blockItemsPerBlock;

    public DynamicBlockItemGenerator(final long blockItemsPerBlock) {
        this.blockItemsPerBlock = blockItemsPerBlock;
    }

    @Override
    public GenerationMode getGenerationMode() {
        return GenerationMode.ADHOC;
    }

    @Override
    public BlockItem getNextBlockItem() {

        BlockItem currentBlockItem = null;
        if (blockItemCount == 1) {

            final BlockHeader blockHeader = BlockHeader.newBuilder().number(1).build();
            currentBlockItem = BlockItem.newBuilder().blockHeader(blockHeader).build();

        } else if (blockItemCount < blockItemsPerBlock) {

            final EventHeader eventHeader =
                    EventHeader.newBuilder().eventCore(EventCore.newBuilder().build()).build();
            currentBlockItem = BlockItem.newBuilder().eventHeader(eventHeader).build();

        } else {

            final BlockProof blockProof = BlockProof.newBuilder().block(1).build();
            currentBlockItem = BlockItem.newBuilder().blockProof(blockProof).build();

            blockCount++;

            // reset
            blockItemCount = 0;
        }

        blockItemCount++;
        totalBlockItems++;

        return currentBlockItem;
    }

    @Override
    public Block getNextBlock() {
        return null;
    }
}
