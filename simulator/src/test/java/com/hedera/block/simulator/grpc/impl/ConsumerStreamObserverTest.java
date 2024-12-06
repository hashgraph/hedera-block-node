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

package com.hedera.block.simulator.grpc.impl;

import static com.hedera.block.simulator.TestUtils.getTestMetrics;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.metrics.MetricsServiceImpl;
import com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter;
import com.hedera.hapi.block.protoc.BlockItemSet;
import com.hedera.hapi.block.protoc.SubscribeStreamResponse;
import com.hedera.hapi.block.protoc.SubscribeStreamResponseCode;
import com.hedera.hapi.block.stream.output.protoc.BlockHeader;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hedera.hapi.block.stream.protoc.BlockProof;
import com.swirlds.config.api.Configuration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsumerStreamObserverTest {

    private MetricsService metricsService;
    private CountDownLatch streamLatch;
    private List<String> lastKnownStatuses;
    private ConsumerStreamObserver observer;

    @BeforeEach
    void setUp() throws IOException {
        Configuration config = TestUtils.getTestConfiguration();

        metricsService = spy(new MetricsServiceImpl(getTestMetrics(config)));
        streamLatch = mock(CountDownLatch.class);
        List<String> lastKnownStatuses = new ArrayList<>();

        observer = new ConsumerStreamObserver(metricsService, streamLatch, lastKnownStatuses);
    }

    @Test
    void testConstructorWithNullArguments() {
        assertThrows(
                NullPointerException.class, () -> new ConsumerStreamObserver(null, streamLatch, lastKnownStatuses));
        assertThrows(
                NullPointerException.class, () -> new ConsumerStreamObserver(metricsService, null, lastKnownStatuses));
        assertThrows(NullPointerException.class, () -> new ConsumerStreamObserver(metricsService, streamLatch, null));
    }

    @Test
    void testOnNextWithStatusResponse() {
        SubscribeStreamResponse response = SubscribeStreamResponse.newBuilder()
                .setStatus(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                .build();

        observer.onNext(response);

        verifyNoInteractions(metricsService);
        verifyNoInteractions(streamLatch);
    }

    @Test
    void testOnNextWithBlockItemsResponse() {
        BlockItem blockItemHeader = BlockItem.newBuilder()
                .setBlockHeader(BlockHeader.newBuilder().setNumber(0).build())
                .build();
        BlockItem blockItemProof = BlockItem.newBuilder()
                .setBlockProof(BlockProof.newBuilder().setBlock(0).build())
                .build();
        BlockItem blockItemProof1 = BlockItem.newBuilder()
                .setBlockProof(BlockProof.newBuilder().setBlock(1).build())
                .build();

        BlockItemSet blockItemsSet = BlockItemSet.newBuilder()
                .addBlockItems(blockItemHeader)
                .addBlockItems(blockItemProof)
                .addBlockItems(blockItemProof1)
                .build();

        SubscribeStreamResponse response = SubscribeStreamResponse.newBuilder()
                .setBlockItems(blockItemsSet)
                .build();
        assertEquals(0, metricsService.get(Counter.LiveBlocksConsumed).get());

        observer.onNext(response);

        assertEquals(2, metricsService.get(Counter.LiveBlocksConsumed).get());
        verifyNoInteractions(streamLatch);
    }

    @Test
    void testOnNextWithUnknownResponseType() {
        SubscribeStreamResponse response = SubscribeStreamResponse.newBuilder().build();

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> observer.onNext(response));

        assertEquals("Unknown response type: RESPONSE_NOT_SET", exception.getMessage());
        verifyNoInteractions(metricsService);
        verifyNoInteractions(streamLatch);
    }

    @Test
    void testOnError() {
        Throwable testException = new RuntimeException("Test exception");

        observer.onError(testException);

        verify(streamLatch).countDown();
        verifyNoInteractions(metricsService);
    }

    @Test
    void testOnCompleted() {
        observer.onCompleted();

        verify(streamLatch).countDown();
        verifyNoInteractions(metricsService);
    }
}
