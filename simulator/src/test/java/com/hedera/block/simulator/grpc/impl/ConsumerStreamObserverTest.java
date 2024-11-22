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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.metrics.MetricsServiceImpl;
import com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter;
import com.hedera.hapi.block.protoc.BlockItemSet;
import com.hedera.hapi.block.protoc.SubscribeStreamResponse;
import com.hedera.hapi.block.protoc.SubscribeStreamResponseCode;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.swirlds.config.api.Configuration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        BlockItem blockItemWithProof = mock(BlockItem.class);
        when(blockItemWithProof.hasBlockProof()).thenReturn(true);

        BlockItem blockItemWithoutProof = mock(BlockItem.class);
        when(blockItemWithoutProof.hasBlockProof()).thenReturn(false);

        List<BlockItem> blockItems = Arrays.asList(blockItemWithProof, blockItemWithoutProof, blockItemWithProof);
        BlockItemSet blockItemSet =
                BlockItemSet.newBuilder().addAllBlockItems(blockItems).build();

        SubscribeStreamResponse response =
                SubscribeStreamResponse.newBuilder().setBlockItems(blockItemSet).build();
        assertEquals(metricsService.get(Counter.LiveBlocksConsumed).get(), 0);

        observer.onNext(response);

        assertEquals(metricsService.get(Counter.LiveBlocksConsumed).get(), 2);
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
