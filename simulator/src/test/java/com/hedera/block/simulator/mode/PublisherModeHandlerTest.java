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

package com.hedera.block.simulator.mode;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.StreamingMode;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.metrics.MetricsServiceImpl;
import com.hedera.hapi.block.stream.protoc.Block;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.swirlds.config.api.Configuration;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PublisherModeHandlerTest {

    @Mock
    private BlockStreamConfig blockStreamConfig;

    @Mock
    private PublishStreamGrpcClient publishStreamGrpcClient;

    @Mock
    private BlockStreamManager blockStreamManager;

    @Mock
    private MetricsService metricsService;

    private PublisherModeHandler publisherModeHandler;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        Configuration configuration = TestUtils.getTestConfiguration(
                Map.of("blockStream.maxBlockItemsToStream", "100", "blockStream.streamingMode", "CONSTANT_RATE"));

        metricsService = new MetricsServiceImpl(TestUtils.getTestMetrics(configuration));
    }

    @Test
    void testStartWithMillisPerBlockStreaming_WithBlocks() throws Exception {
        // Configure blockStreamConfig
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.MILLIS_PER_BLOCK);
        when(blockStreamConfig.millisecondsPerBlock()).thenReturn(0); // No delay for testing

        publisherModeHandler = new PublisherModeHandler(
                blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);

        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);

        when(blockStreamManager.getNextBlock())
                .thenReturn(block1)
                .thenReturn(block2)
                .thenReturn(null);
        when(publishStreamGrpcClient.streamBlock(any(Block.class))).thenReturn(true);

        when(publishStreamGrpcClient.streamBlock(block1)).thenReturn(true);
        when(publishStreamGrpcClient.streamBlock(block2)).thenReturn(true);

        publisherModeHandler.start();

        verify(publishStreamGrpcClient).streamBlock(block1);
        verify(publishStreamGrpcClient).streamBlock(block2);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(3)).getNextBlock();
    }

    @Test
    void testStartWithMillisPerBlockStreaming_NoBlocks() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.MILLIS_PER_BLOCK);

        publisherModeHandler = new PublisherModeHandler(
                blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);

        when(blockStreamManager.getNextBlock()).thenReturn(null);

        publisherModeHandler.start();

        verify(publishStreamGrpcClient, never()).streamBlock(any(Block.class));
        verify(blockStreamManager).getNextBlock();
    }

    @Test
    void testStartWithConstantRateStreaming_WithinMaxItems() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);
        when(blockStreamConfig.delayBetweenBlockItems()).thenReturn(0);
        when(blockStreamConfig.maxBlockItemsToStream()).thenReturn(5);

        publisherModeHandler = new PublisherModeHandler(
                blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);
        when(publishStreamGrpcClient.streamBlock(any(Block.class))).thenReturn(true);

        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);

        BlockItem blockItem1 = mock(BlockItem.class);
        BlockItem blockItem2 = mock(BlockItem.class);
        BlockItem blockItem3 = mock(BlockItem.class);
        BlockItem blockItem4 = mock(BlockItem.class);

        when(block1.getItemsList()).thenReturn(Arrays.asList(blockItem1, blockItem2));
        when(block2.getItemsList()).thenReturn(Arrays.asList(blockItem3, blockItem4));

        when(blockStreamManager.getNextBlock())
                .thenReturn(block1)
                .thenReturn(block2)
                .thenReturn(null);

        when(publishStreamGrpcClient.streamBlock(block1)).thenReturn(true);
        when(publishStreamGrpcClient.streamBlock(block2)).thenReturn(true);

        publisherModeHandler.start();

        verify(publishStreamGrpcClient).streamBlock(block1);
        verify(publishStreamGrpcClient).streamBlock(block2);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(3)).getNextBlock();
    }

    @Test
    void testStartWithConstantRateStreaming_ExceedingMaxItems() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);
        when(blockStreamConfig.delayBetweenBlockItems()).thenReturn(0);
        when(blockStreamConfig.maxBlockItemsToStream()).thenReturn(5);

        publisherModeHandler = new PublisherModeHandler(
                blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);
        when(publishStreamGrpcClient.streamBlock(any(Block.class))).thenReturn(true);

        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);
        Block block3 = mock(Block.class);
        Block block4 = mock(Block.class);

        BlockItem blockItem1 = mock(BlockItem.class);
        BlockItem blockItem2 = mock(BlockItem.class);
        BlockItem blockItem3 = mock(BlockItem.class);
        BlockItem blockItem4 = mock(BlockItem.class);

        when(block1.getItemsList()).thenReturn(Arrays.asList(blockItem1, blockItem2));
        when(block2.getItemsList()).thenReturn(Arrays.asList(blockItem3, blockItem4));
        when(block3.getItemsList()).thenReturn(Arrays.asList(blockItem1, blockItem2));
        when(block4.getItemsList()).thenReturn(Arrays.asList(blockItem3, blockItem4));

        when(blockStreamManager.getNextBlock())
                .thenReturn(block1)
                .thenReturn(block2)
                .thenReturn(block3)
                .thenReturn(block4);

        when(publishStreamGrpcClient.streamBlock(block1)).thenReturn(true);
        when(publishStreamGrpcClient.streamBlock(block2)).thenReturn(true);
        when(publishStreamGrpcClient.streamBlock(block3)).thenReturn(true);
        when(publishStreamGrpcClient.streamBlock(block4)).thenReturn(true);

        publisherModeHandler.start();

        verify(publishStreamGrpcClient).streamBlock(block1);
        verify(publishStreamGrpcClient).streamBlock(block2);
        verify(publishStreamGrpcClient).streamBlock(block3);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(3)).getNextBlock();
    }

    @Test
    void testStartWithConstantRateStreaming_NoBlocks() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);
        publisherModeHandler = new PublisherModeHandler(
                blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);

        when(blockStreamManager.getNextBlock()).thenReturn(null);

        publisherModeHandler.start();

        verify(publishStreamGrpcClient, never()).streamBlock(any(Block.class));
        verify(blockStreamManager).getNextBlock();
    }

    @Test
    void testStartWithExceptionDuringStreaming() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.MILLIS_PER_BLOCK);

        publisherModeHandler = new PublisherModeHandler(
                blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);

        when(blockStreamManager.getNextBlock()).thenThrow(new IOException("Test exception"));

        assertThrows(IOException.class, () -> publisherModeHandler.start());

        verify(publishStreamGrpcClient, never()).streamBlock(any(Block.class));
        verify(blockStreamManager).getNextBlock();
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verifyNoMoreInteractions(blockStreamManager);
    }

    @Test
    void testMillisPerBlockStreaming_streamSuccessBecomesFalse() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.MILLIS_PER_BLOCK);
        when(blockStreamConfig.millisecondsPerBlock()).thenReturn(1000);

        publisherModeHandler = new PublisherModeHandler(
                blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);

        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);

        when(blockStreamManager.getNextBlock())
                .thenReturn(block1)
                .thenReturn(block2)
                .thenReturn(null);

        when(publishStreamGrpcClient.streamBlock(block1)).thenReturn(true);
        when(publishStreamGrpcClient.streamBlock(block2)).thenReturn(false);

        publisherModeHandler.start();

        verify(publishStreamGrpcClient).streamBlock(block1);
        verify(publishStreamGrpcClient).streamBlock(block2);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(2)).getNextBlock();
    }

    @Test
    void testConstantRateStreaming_streamSuccessBecomesFalse() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);
        when(blockStreamConfig.delayBetweenBlockItems()).thenReturn(0);
        when(blockStreamConfig.maxBlockItemsToStream()).thenReturn(100);

        publisherModeHandler = new PublisherModeHandler(
                blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);

        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);

        BlockItem blockItem1 = mock(BlockItem.class);
        BlockItem blockItem2 = mock(BlockItem.class);

        when(block1.getItemsList()).thenReturn(Collections.singletonList(blockItem1));
        when(block2.getItemsList()).thenReturn(Collections.singletonList(blockItem2));

        when(blockStreamManager.getNextBlock())
                .thenReturn(block1)
                .thenReturn(block2)
                .thenReturn(null);

        when(publishStreamGrpcClient.streamBlock(block1)).thenReturn(true);
        when(publishStreamGrpcClient.streamBlock(block2)).thenReturn(false);

        publisherModeHandler.start();

        verify(publishStreamGrpcClient).streamBlock(block1);
        verify(publishStreamGrpcClient).streamBlock(block2);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(2)).getNextBlock();
    }
}
