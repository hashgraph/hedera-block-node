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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.StreamingMode;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import java.io.IOException;
import java.util.Arrays;
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
    }

    @Test
    void testStartWithMillisPerBlockStreaming_WithBlocks() throws Exception {
        // Configure blockStreamConfig
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.MILLIS_PER_BLOCK);
        when(blockStreamConfig.millisecondsPerBlock()).thenReturn(0); // No delay for testing

        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);

        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);

        when(blockStreamManager.getNextBlock())
                .thenReturn(block1)
                .thenReturn(block2)
                .thenReturn(null);

        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient).streamBlock(block1);
        verify(publishStreamGrpcClient).streamBlock(block2);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(3)).getNextBlock();
    }

    @Test
    void testStartWithMillisPerBlockStreaming_NoBlocks() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.MILLIS_PER_BLOCK);

        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);

        when(blockStreamManager.getNextBlock()).thenReturn(null);

        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient, never()).streamBlock(any(Block.class));
        verify(blockStreamManager).getNextBlock();
    }

    @Test
    void testStartWithConstantRateStreaming_WithinMaxItems() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);
        when(blockStreamConfig.delayBetweenBlockItems()).thenReturn(0);
        when(blockStreamConfig.maxBlockItemsToStream()).thenReturn(5);

        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);

        Block block1 = mock(Block.class);
        Block block2 = mock(Block.class);

        BlockItem blockItem1 = mock(BlockItem.class);
        BlockItem blockItem2 = mock(BlockItem.class);
        BlockItem blockItem3 = mock(BlockItem.class);
        BlockItem blockItem4 = mock(BlockItem.class);

        when(block1.items()).thenReturn(Arrays.asList(blockItem1, blockItem2));
        when(block2.items()).thenReturn(Arrays.asList(blockItem3, blockItem4));

        when(blockStreamManager.getNextBlock())
                .thenReturn(block1)
                .thenReturn(block2)
                .thenReturn(null);

        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient).streamBlock(block1);
        verify(publishStreamGrpcClient).streamBlock(block2);
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verify(blockStreamManager, times(3)).getNextBlock();
    }

    @Test
    void testStartWithConstantRateStreaming_NoBlocks() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.CONSTANT_RATE);
        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);

        when(blockStreamManager.getNextBlock()).thenReturn(null);

        publisherModeHandler.start(blockStreamManager);

        verify(publishStreamGrpcClient, never()).streamBlock(any(Block.class));
        verify(blockStreamManager).getNextBlock();
    }

    @Test
    void testStartWithExceptionDuringStreaming() throws Exception {
        when(blockStreamConfig.streamingMode()).thenReturn(StreamingMode.MILLIS_PER_BLOCK);

        publisherModeHandler = new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);

        when(blockStreamManager.getNextBlock()).thenThrow(new IOException("Test exception"));

        assertThrows(IOException.class, () -> publisherModeHandler.start(blockStreamManager));

        verify(publishStreamGrpcClient, never()).streamBlock(any(Block.class));
        verify(blockStreamManager).getNextBlock();
        verifyNoMoreInteractions(publishStreamGrpcClient);
        verifyNoMoreInteractions(blockStreamManager);
    }
}
