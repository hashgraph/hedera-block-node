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

package com.hedera.block.server.pbj;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.grpc.ServiceInterface;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PbjBlockAccessServiceProxyTest {

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private BlockReader<Block> blockReader;

    @Mock
    private ServiceInterface.RequestOptions options;

    @Mock
    private Flow.Subscriber<? super Bytes> replies;

    private BlockNodeContext blockNodeContext;

    private static final int testTimeout = 100;

    @BeforeEach
    public void setUp() throws IOException {
        Map<String, String> properties = new HashMap<>();
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(properties);
    }

    @Test
    public void testOpenWithIncorrectMethod() {

        final PbjBlockAccessServiceProxy pbjBlockAccessServiceProxy =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);
        Pipeline<? super Bytes> pipeline = pbjBlockAccessServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, replies);

        verify(replies, timeout(testTimeout).times(1)).onError(any());
        assertNotNull(pipeline);
    }

    @Test
    public void testSingleBlock() throws IOException, ParseException {
        final PbjBlockAccessServiceProxy pbjBlockAccessServiceProxy =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);
        final Pipeline<? super Bytes> pipeline =
                pbjBlockAccessServiceProxy.open(PbjBlockAccessService.BlockAccessMethod.singleBlock, options, replies);
        assertNotNull(pipeline);

        when(serviceStatus.isRunning()).thenReturn(true);

        final Block block = Block.newBuilder()
                .items(BlockItem.newBuilder()
                        .blockHeader(BlockHeader.newBuilder().number(1).build())
                        .build())
                .build();
        when(blockReader.read(1)).thenReturn(Optional.of(block));

        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();
        pipeline.onNext(SingleBlockRequest.PROTOBUF.toBytes(singleBlockRequest));

        final var readSuccessResponse = SingleBlockResponse.newBuilder()
                .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                .block(block)
                .build();
        verify(replies, timeout(testTimeout).times(1)).onSubscribe(any());
        verify(replies, timeout(testTimeout).times(1))
                .onNext(SingleBlockResponse.PROTOBUF.toBytes(readSuccessResponse));
        verify(replies, timeout(testTimeout).times(1)).onComplete();
    }

    @Test
    public void testSingleBlockNotFound() throws IOException, ParseException {
        final PbjBlockAccessServiceProxy pbjBlockAccessServiceProxy =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);
        final Pipeline<? super Bytes> pipeline =
                pbjBlockAccessServiceProxy.open(PbjBlockAccessService.BlockAccessMethod.singleBlock, options, replies);
        assertNotNull(pipeline);

        when(serviceStatus.isRunning()).thenReturn(true);
        when(blockReader.read(1)).thenReturn(Optional.empty());

        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();
        pipeline.onNext(SingleBlockRequest.PROTOBUF.toBytes(singleBlockRequest));

        final var blockNotFound = SingleBlockResponse.newBuilder()
                .status(SingleBlockResponseCode.READ_BLOCK_NOT_FOUND)
                .build();
        verify(replies, timeout(testTimeout).times(1)).onSubscribe(any());
        verify(replies, timeout(testTimeout).times(1)).onNext(SingleBlockResponse.PROTOBUF.toBytes(blockNotFound));
        verify(replies, timeout(testTimeout).times(1)).onComplete();
    }

    @Test
    public void testSingleBlockIOException() throws IOException, ParseException {
        final PbjBlockAccessServiceProxy pbjBlockAccessServiceProxy =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);
        final Pipeline<? super Bytes> pipeline =
                pbjBlockAccessServiceProxy.open(PbjBlockAccessService.BlockAccessMethod.singleBlock, options, replies);
        assertNotNull(pipeline);

        when(serviceStatus.isRunning()).thenReturn(true);
        when(blockReader.read(1)).thenThrow(new IOException("Test IOException"));

        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();
        pipeline.onNext(SingleBlockRequest.PROTOBUF.toBytes(singleBlockRequest));

        final var blockNotAvailable = SingleBlockResponse.newBuilder()
                .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                .build();
        verify(replies, timeout(testTimeout).times(1)).onSubscribe(any());
        verify(replies, timeout(testTimeout).times(1)).onNext(SingleBlockResponse.PROTOBUF.toBytes(blockNotAvailable));
        verify(replies, timeout(testTimeout).times(1)).onComplete();
    }
}
