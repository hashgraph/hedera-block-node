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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.StreamingMode;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

public class PublisherModeHandlerTest {

    @Mock private BlockStreamConfig blockStreamConfig;

    @Mock private PublishStreamGrpcClient publishStreamGrpcClient;

    @Mock private BlockStreamManager blockStreamManager;

    private PublisherModeHandler publisherModeHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize blockStreamConfig with default behavior
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.MILLIS_PER_BLOCK);
        when(blockStreamConfig.delayBetweenBlockItems()).thenReturn(0);
        when(blockStreamConfig.millisecondsPerBlock()).thenReturn(0);
        when(blockStreamConfig.maxBlockItemsToStream()).thenReturn(Integer.MAX_VALUE);

        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);
    }

    @Test
    void testStartWithMillisPerBlockStreaming() throws Exception {
        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);
        when(blockStreamManager.getNextBlock())
                .thenReturn(block1)
                .thenReturn(block2)
                .thenReturn(null); // End of stream

        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient).streamBlock(block1);
        verify(publishStreamGrpcClient).streamBlock(block2);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(3)).getNextBlock();
    }

    @Test
    void testStartWithConstantRateStreaming() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);
        when(blockStreamConfig.delayBetweenBlockItems()).thenReturn(0);
        when(blockStreamConfig.maxBlockItemsToStream()).thenReturn(2);

        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);

        BlockItem blockItem1 = mock(BlockItem.class);
        BlockItem blockItem2 = mock(BlockItem.class);
        when(blockStreamManager.getNextBlockItem())
                .thenReturn(blockItem1)
                .thenReturn(blockItem2)
                .thenReturn(null); // End of block items

        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient).streamBlockItem(blockItem1);
        verify(publishStreamGrpcClient).streamBlockItem(blockItem2);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(2)).getNextBlockItem();
    }

    @Test
    void testStartWithMillisPerBlockStreaming_NoBlocks() throws Exception {
        when(blockStreamManager.getNextBlock()).thenReturn(null);
        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient, never()).streamBlock(any(Block.class));
        verify(blockStreamManager, times(1)).getNextBlock();
    }

    @Test
    void testStartWithConstantRateStreaming_NoBlockItems() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);

        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);

        when(blockStreamManager.getNextBlockItem()).thenReturn(null);

        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient, never()).streamBlockItem(any(BlockItem.class));
        verify(blockStreamManager, times(1)).getNextBlockItem();
    }

    @Test
    void testStartWithMaxBlockItems() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);
        when(blockStreamConfig.maxBlockItemsToStream()).thenReturn(2);

        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);

        BlockItem blockItem1 = mock(BlockItem.class);
        BlockItem blockItem2 = mock(BlockItem.class);
        BlockItem blockItem3 = mock(BlockItem.class);
        when(blockStreamManager.getNextBlockItem())
                .thenReturn(blockItem1)
                .thenReturn(blockItem2)
                .thenReturn(blockItem3);

        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient).streamBlockItem(blockItem1);
        verify(publishStreamGrpcClient).streamBlockItem(blockItem2);
        verify(publishStreamGrpcClient, never()).streamBlockItem(blockItem3);
        verifyNoMoreInteractions(publishStreamGrpcClient);
    }
}
