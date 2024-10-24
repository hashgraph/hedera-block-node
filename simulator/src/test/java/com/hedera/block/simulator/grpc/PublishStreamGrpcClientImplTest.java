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

package com.hedera.block.simulator.grpc;

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublishStreamGrpcClientImplTest {

    GrpcConfig grpcConfig;
    BlockStreamConfig blockStreamConfig;

    @BeforeEach
    void setUp() throws IOException {

        grpcConfig = TestUtils.getTestConfiguration().getConfigData(GrpcConfig.class);
        blockStreamConfig =
                TestUtils.getTestConfiguration(Map.of("blockStream.blockItemsBatchSize", "2"))
                        .getConfigData(BlockStreamConfig.class);
    }

    @AfterEach
    void tearDown() {}

    @Test
    void streamBlockItem() {
        BlockItem blockItem = BlockItem.newBuilder().build();
        PublishStreamGrpcClientImpl publishStreamGrpcClient =
                new PublishStreamGrpcClientImpl(grpcConfig, blockStreamConfig);
        boolean result = publishStreamGrpcClient.streamBlockItem(List.of(blockItem));
        assertTrue(result);
    }

    @Test
    void streamBlock() {
        BlockItem blockItem = BlockItem.newBuilder().build();
        Block block = Block.newBuilder().items(blockItem).build();

        Block block1 = Block.newBuilder().items(blockItem, blockItem, blockItem).build();

        PublishStreamGrpcClientImpl publishStreamGrpcClient =
                new PublishStreamGrpcClientImpl(grpcConfig, blockStreamConfig);

        boolean result = publishStreamGrpcClient.streamBlock(block);
        assertTrue(result);

        boolean result1 = publishStreamGrpcClient.streamBlock(block1);
        assertTrue(result1);
    }
}
