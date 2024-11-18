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

package com.hedera.block.server.consumer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.grpc.Pipeline;
import java.io.IOException;
import java.time.InstantSource;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConsumerStreamResponseObserverTest {

    private final long TIMEOUT_THRESHOLD_MILLIS = 50L;
    private final long TEST_TIME = 1_719_427_664_950L;

    private static final int testTimeout = 1000;

    @Mock
    private StreamMediator<BlockItemUnparsed, SubscribeStreamResponseUnparsed> streamMediator;

    @Mock
    private Pipeline<SubscribeStreamResponseUnparsed> responseStreamObserver;

    @Mock
    private ObjectEvent<SubscribeStreamResponseUnparsed> objectEvent;

    @Mock
    private InstantSource testClock;

    final BlockNodeContext testContext;

    public ConsumerStreamResponseObserverTest() throws IOException {
        this.testContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY, String.valueOf(TIMEOUT_THRESHOLD_MILLIS)));
    }

    //    @Test
    //    public void testProducerTimeoutWithinWindow() {
    //
    //        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);
    //
    //        final var consumerBlockItemObserver =
    //                new ConsumerStreamResponseObserver(testClock, streamMediator, responseStreamObserver,
    // testContext);
    //
    //        final BlockHeader blockHeader = BlockHeader.newBuilder().number(1).build();
    //        final BlockItemUnparsed blockItem =
    //                BlockItemUnparsed.newBuilder().blockItem(BlockHeader.PROTOBUF.toBytes(blockHeader)).build();
    //        final BlockItemSetUnparsed blockItemSet =
    //                BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build();
    //        final SubscribeStreamResponseUnparsed subscribeStreamResponse =
    //                SubscribeStreamResponseUnparsed.newBuilder().blockItems(blockItemSet).build();
    //
    //        when(objectEvent.get()).thenReturn(subscribeStreamResponse);
    //
    //        consumerBlockItemObserver.onEvent(objectEvent, 0, true);
    //
    //        // verify the observer is called with the next BlockItem
    //        verify(responseStreamObserver).onNext(subscribeStreamResponse);
    //
    //        // verify the mediator is NOT called to unsubscribe the observer
    //        verify(streamMediator, timeout(testTimeout).times(0)).unsubscribe(consumerBlockItemObserver);
    //    }

    @Test
    public void testProducerTimeoutOutsideWindow() throws InterruptedException {

        // Mock a clock with 2 different return values in response to anticipated
        // millis() calls. Here the second call will always be outside the timeout window.
        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS + 1);

        final var consumerBlockItemObserver =
                new ConsumerStreamResponseObserver(testClock, streamMediator, responseStreamObserver, testContext);

        consumerBlockItemObserver.onEvent(objectEvent, 0, true);
        verify(streamMediator).unsubscribe(consumerBlockItemObserver);
    }

    //    @Test
    //    public void testConsumerNotToSendBeforeBlockHeader() {
    //
    //        // Mock a clock with 2 different return values in response to anticipated
    //        // millis() calls. Here the second call will always be inside the timeout window.
    //        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);
    //
    //        final var consumerBlockItemObserver =
    //                new ConsumerStreamResponseObserver(testClock, streamMediator, responseStreamObserver,
    // testContext);
    //
    //        // Send non-header BlockItems to validate that the observer does not send them
    //        for (int i = 1; i <= 10; i++) {
    //
    //            if (i % 2 == 0) {
    //                final EventHeader eventHeader = EventHeader.newBuilder()
    //                        .eventCore(EventCore.newBuilder().build())
    //                        .build();
    //                final BlockItem blockItem =
    //                        BlockItem.newBuilder().eventHeader(eventHeader).build();
    //                final BlockItemSet blockItemSet =
    //                        BlockItemSet.newBuilder().blockItems(blockItem).build();
    //                final SubscribeStreamResponseUnparsed subscribeStreamResponse =
    // SubscribeStreamResponseUnparsed.newBuilder()
    //                        .blockItems(BlockItemSetUnparsed.PROTOBUF.toBytes(blockItemSet))
    //                        .build();
    //                when(objectEvent.get()).thenReturn(subscribeStreamResponse);
    //            } else {
    //                final BlockProof blockProof = BlockProof.newBuilder().block(i).build();
    //                final BlockItem blockItem =
    //                        BlockItem.newBuilder().blockProof(blockProof).build();
    //                final BlockItemSet blockItemSet =
    //                        BlockItemSet.newBuilder().blockItems(blockItem).build();
    //                final SubscribeStreamResponse subscribeStreamResponse = SubscribeStreamResponse.newBuilder()
    //                        .blockItems(blockItemSet)
    //                        .build();
    //                when(objectEvent.get()).thenReturn(subscribeStreamResponse);
    //            }
    //
    //            consumerBlockItemObserver.onEvent(objectEvent, 0, true);
    //        }
    //
    //        final BlockItem blockItem = BlockItem.newBuilder().build();
    //        final BlockItemSet blockItemSet =
    //                BlockItemSet.newBuilder().blockItems(blockItem).build();
    //        final SubscribeStreamResponse subscribeStreamResponse =
    //                SubscribeStreamResponse.newBuilder().blockItems(blockItemSet).build();
    //
    //        // Confirm that the observer was called with the next BlockItem
    //        // since we never send a BlockItem with a Header to start the stream.
    //        verify(responseStreamObserver, timeout(testTimeout).times(0)).onNext(subscribeStreamResponse);
    //    }
    //
    //    @Test
    //    public void testSubscriberStreamResponseIsBlockItemWhenBlockItemIsNull() {
    //
    //        // The generated objects contain safeguards to prevent a SubscribeStreamResponse
    //        // being created with a null BlockItem. Here, I have to used a spy() to even
    //        // manufacture this scenario. This should not happen in production.
    //        final BlockItem blockItem = BlockItem.newBuilder().build();
    //        final BlockItemSet blockItemSet =
    //                BlockItemSet.newBuilder().blockItems(blockItem).build();
    //        final SubscribeStreamResponse subscribeStreamResponse = spy(
    //                SubscribeStreamResponse.newBuilder().blockItems(blockItemSet).build());
    //
    //        when(subscribeStreamResponse.blockItems()).thenReturn(null);
    //        when(objectEvent.get()).thenReturn(subscribeStreamResponse);
    //
    //        final var consumerBlockItemObserver =
    //                new ConsumerStreamResponseObserver(testClock, streamMediator, responseStreamObserver,
    // testContext);
    //        assertThrows(IllegalArgumentException.class, () -> consumerBlockItemObserver.onEvent(objectEvent, 0,
    // true));
    //    }

    //    @Test
    //    public void testSubscribeStreamResponseTypeNotSupported() {
    //
    //        final SubscribeStreamResponse subscribeStreamResponse =
    //                SubscribeStreamResponse.newBuilder().build();
    //        when(objectEvent.get()).thenReturn(subscribeStreamResponse);
    //
    //        final var consumerBlockItemObserver =
    //                new ConsumerStreamResponseObserver(testClock, streamMediator, responseStreamObserver,
    // testContext);
    //
    //        assertThrows(IllegalArgumentException.class, () -> consumerBlockItemObserver.onEvent(objectEvent, 0,
    // true));
    //    }
}
