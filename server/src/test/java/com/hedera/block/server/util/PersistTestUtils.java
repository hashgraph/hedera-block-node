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

package com.hedera.block.server.util;

import static com.hedera.block.protos.BlockStreamService.BlockItem;
import static com.hedera.block.protos.BlockStreamService.BlockProof;
import static com.hedera.block.protos.BlockStreamService.EventMetadata;

import com.hedera.block.protos.BlockStreamService;
import java.util.ArrayList;
import java.util.List;

public final class PersistTestUtils {

    private PersistTestUtils() {}

    public static List<BlockItem> generateBlockItems(int numOfBlocks) {

        List<BlockItem> blockItems = new ArrayList<>();
        for (int i = 1; i <= numOfBlocks; i++) {
            for (int j = 1; j <= 10; j++) {
                switch (j) {
                    case 1:
                        // First block is always the header
                        blockItems.add(
                                BlockItem.newBuilder()
                                        .setHeader(
                                                BlockStreamService.BlockHeader.newBuilder()
                                                        .setBlockNumber(i)
                                                        .build())
                                        .setValue("block-item-" + (j))
                                        .build());
                        break;
                    case 10:
                        // Last block is always the state proof
                        blockItems.add(
                                BlockItem.newBuilder()
                                        .setStateProof(BlockProof.newBuilder().setBlock(i).build())
                                        .build());
                        break;
                    default:
                        // Middle blocks are events
                        blockItems.add(
                                BlockItem.newBuilder()
                                        .setStartEvent(
                                                EventMetadata.newBuilder().setCreatorId(i).build())
                                        .build());
                        break;
                }
            }
        }

        return blockItems;
    }
}