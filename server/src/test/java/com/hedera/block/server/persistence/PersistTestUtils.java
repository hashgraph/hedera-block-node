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

package com.hedera.block.server.persistence;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class PersistTestUtils {

    private PersistTestUtils() {}

    public static List<BlockStreamServiceGrpcProto.Block> generateBlocks(int numOfBlocks) {
        return IntStream
                .range(1, numOfBlocks + 1)
                .mapToObj(i -> BlockStreamServiceGrpcProto.Block
                        .newBuilder()
                        .setId(i)
                        .setValue("block-node-" + i).build()
                )
                .collect(Collectors.toList());
    }}
