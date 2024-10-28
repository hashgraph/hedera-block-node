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

import static com.hedera.block.simulator.TestUtils.getTestMetrics;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.metrics.MetricsServiceImpl;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.swirlds.config.api.Configuration;
import io.grpc.ManagedChannel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishStreamGrpcClientImplTest {

    GrpcConfig grpcConfig;
    BlockStreamConfig blockStreamConfig;
    MetricsService metricsService;

    @BeforeEach
    void setUp() throws IOException {

        grpcConfig = TestUtils.getTestConfiguration().getConfigData(GrpcConfig.class);
        blockStreamConfig = TestUtils.getTestConfiguration(Map.of("blockStream.blockItemsBatchSize", "2"))
                .getConfigData(BlockStreamConfig.class);

        Configuration config = TestUtils.getTestConfiguration();
        metricsService = new MetricsServiceImpl(getTestMetrics(config));
    }

    @AfterEach
    void tearDown() {}

    @Test
    void streamBlockItem() {
        BlockItem blockItem = BlockItem.newBuilder().build();
        PublishStreamGrpcClientImpl publishStreamGrpcClient =
                new PublishStreamGrpcClientImpl(grpcConfig, blockStreamConfig, metricsService);
        publishStreamGrpcClient.init();
        boolean result = publishStreamGrpcClient.streamBlockItem(List.of(blockItem));
        assertTrue(result);
    }

    @Test
    void streamBlock() {
        BlockItem blockItem = BlockItem.newBuilder().build();
        Block block = Block.newBuilder().items(blockItem).build();

        Block block1 = Block.newBuilder().items(blockItem, blockItem, blockItem).build();

        PublishStreamGrpcClientImpl publishStreamGrpcClient =
                new PublishStreamGrpcClientImpl(grpcConfig, blockStreamConfig, metricsService);
        publishStreamGrpcClient.init();
        boolean result = publishStreamGrpcClient.streamBlock(block);
        assertTrue(result);

        boolean result1 = publishStreamGrpcClient.streamBlock(block1);
        assertTrue(result1);
    }

    @Test
    void testShutdown() throws Exception {
        PublishStreamGrpcClientImpl publishStreamGrpcClient =
                new PublishStreamGrpcClientImpl(grpcConfig, blockStreamConfig, metricsService);
        publishStreamGrpcClient.init();

        Field channelField = PublishStreamGrpcClientImpl.class.getDeclaredField("channel");
        ManagedChannel mockChannel = mock(ManagedChannel.class);

        try {
            channelField.setAccessible(true);
            channelField.set(publishStreamGrpcClient, mockChannel);
        } finally {
            channelField.setAccessible(false);
        }
        publishStreamGrpcClient.shutdown();

        // Verify that channel.shutdown() was called
        verify(mockChannel).shutdown();
    }
}
