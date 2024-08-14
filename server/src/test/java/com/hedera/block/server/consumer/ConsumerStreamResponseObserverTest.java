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

import static com.hedera.block.protos.BlockStreamService.*;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static org.mockito.Mockito.*;

import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.time.InstantSource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConsumerStreamResponseObserverTest {

    private final long TIMEOUT_THRESHOLD_MILLIS = 50L;
    private final long TEST_TIME = 1_719_427_664_950L;

    @Mock private StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> streamMediator;
    @Mock private StreamObserver<SubscribeStreamResponse> responseStreamObserver;
    @Mock private ObjectEvent<SubscribeStreamResponse> objectEvent;

    @Mock private ServerCallStreamObserver<SubscribeStreamResponse> serverCallStreamObserver;
    @Mock private InstantSource testClock;

    final ConsumerConfig consumerConfig = new ConsumerConfig(TIMEOUT_THRESHOLD_MILLIS);

    @Test
    public void testProducerTimeoutWithinWindow() {

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var consumerBlockItemObserver =
                new ConsumerStreamResponseObserver(
                        consumerConfig, testClock, streamMediator, responseStreamObserver);

        final BlockHeader blockHeader = BlockHeader.newBuilder().setBlockNumber(1).build();
        final BlockItem blockItem = BlockItem.newBuilder().setHeader(blockHeader).build();
        final SubscribeStreamResponse subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().setBlockItem(blockItem).build();

        when(objectEvent.get()).thenReturn(subscribeStreamResponse);

        consumerBlockItemObserver.onEvent(objectEvent, 0, true);

        // verify the observer is called with the next BlockItem
        verify(responseStreamObserver).onNext(subscribeStreamResponse);

        // verify the mediator is NOT called to unsubscribe the observer
        verify(streamMediator, never()).unsubscribe(consumerBlockItemObserver);
    }

    @Test
    public void testProducerTimeoutOutsideWindow() throws InterruptedException {

        // Mock a clock with 2 different return values in response to anticipated
        // millis() calls. Here the second call will always be outside the timeout window.
        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS + 1);

        final var consumerBlockItemObserver =
                new ConsumerStreamResponseObserver(
                        consumerConfig, testClock, streamMediator, responseStreamObserver);

        consumerBlockItemObserver.onEvent(objectEvent, 0, true);
        verify(streamMediator).unsubscribe(consumerBlockItemObserver);
    }

    @Test
    public void testHandlersSetOnObserver() throws InterruptedException {

        // Mock a clock with 2 different return values in response to anticipated
        // millis() calls. Here the second call will always be inside the timeout window.
        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        new ConsumerStreamResponseObserver(
                consumerConfig, testClock, streamMediator, serverCallStreamObserver);

        verify(serverCallStreamObserver, timeout(50).times(1)).setOnCloseHandler(any());
        verify(serverCallStreamObserver, timeout(50).times(1)).setOnCancelHandler(any());
    }

    @Test
    public void testResponseNotPermittedAfterCancel() {

        final TestConsumerStreamResponseObserver consumerStreamResponseObserver =
                new TestConsumerStreamResponseObserver(
                        consumerConfig, testClock, streamMediator, serverCallStreamObserver);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final SubscribeStreamResponse subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().setBlockItem(blockItems.getFirst()).build();
        when(objectEvent.get()).thenReturn(subscribeStreamResponse);

        // Confirm that the observer is called with the first BlockItem
        consumerStreamResponseObserver.onEvent(objectEvent, 0, true);

        // Cancel the observer
        consumerStreamResponseObserver.cancel();

        // Attempt to send another BlockItem
        consumerStreamResponseObserver.onEvent(objectEvent, 0, true);

        // Confirm that canceling the observer allowed only 1 response to be sent.
        verify(serverCallStreamObserver, timeout(50).times(1)).onNext(subscribeStreamResponse);
    }

    @Test
    public void testResponseNotPermittedAfterClose() {

        final TestConsumerStreamResponseObserver consumerStreamResponseObserver =
                new TestConsumerStreamResponseObserver(
                        consumerConfig, testClock, streamMediator, serverCallStreamObserver);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final SubscribeStreamResponse subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().setBlockItem(blockItems.getFirst()).build();
        when(objectEvent.get()).thenReturn(subscribeStreamResponse);

        // Confirm that the observer is called with the first BlockItem
        consumerStreamResponseObserver.onEvent(objectEvent, 0, true);

        // Close the observer
        consumerStreamResponseObserver.close();

        // Attempt to send another BlockItem
        consumerStreamResponseObserver.onEvent(objectEvent, 0, true);

        // Confirm that canceling the observer allowed only 1 response to be sent.
        verify(serverCallStreamObserver, timeout(50).times(1)).onNext(subscribeStreamResponse);
    }

    @Test
    public void testConsumerNotToSendBeforeBlockHeader() {

        // Mock a clock with 2 different return values in response to anticipated
        // millis() calls. Here the second call will always be inside the timeout window.
        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var consumerBlockItemObserver =
                new ConsumerStreamResponseObserver(
                        consumerConfig, testClock, streamMediator, responseStreamObserver);

        // Send non-header BlockItems to validate that the observer does not send them
        for (int i = 1; i <= 10; i++) {

            if (i % 2 == 0) {
                final EventMetadata eventMetadata =
                        EventMetadata.newBuilder().setCreatorId(i).build();
                final BlockItem blockItem =
                        BlockItem.newBuilder().setStartEvent(eventMetadata).build();
                final SubscribeStreamResponse subscribeStreamResponse =
                        SubscribeStreamResponse.newBuilder().setBlockItem(blockItem).build();
                when(objectEvent.get()).thenReturn(subscribeStreamResponse);
            } else {
                final BlockProof blockProof = BlockProof.newBuilder().setBlock(i).build();
                final BlockItem blockItem =
                        BlockItem.newBuilder().setStateProof(blockProof).build();
                final SubscribeStreamResponse subscribeStreamResponse =
                        SubscribeStreamResponse.newBuilder().setBlockItem(blockItem).build();
                when(objectEvent.get()).thenReturn(subscribeStreamResponse);
            }

            consumerBlockItemObserver.onEvent(objectEvent, 0, true);
        }

        final BlockItem blockItem = BlockItem.newBuilder().build();
        final SubscribeStreamResponse subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().setBlockItem(blockItem).build();

        // Confirm that the observer was called with the next BlockItem
        // since we never send a BlockItem with a Header to start the stream.
        verify(responseStreamObserver, timeout(50).times(0)).onNext(subscribeStreamResponse);
    }

    private static class TestConsumerStreamResponseObserver extends ConsumerStreamResponseObserver {

        public TestConsumerStreamResponseObserver(
                ConsumerConfig consumerConfig,
                InstantSource producerLivenessClock,
                StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> subscriptionHandler,
                StreamObserver<SubscribeStreamResponse> subscribeStreamResponseObserver) {
            super(
                    consumerConfig,
                    producerLivenessClock,
                    subscriptionHandler,
                    subscribeStreamResponseObserver);
        }

        public void cancel() {
            onCancel.run();
        }

        public void close() {
            onClose.run();
        }
    }
}
