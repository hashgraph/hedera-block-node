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

package com.hedera.block.server.mediator;

import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.metrics.BlockNodeMetricNames.Counter.LiveBlockItems;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.ServiceStatusImpl;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerStreamResponseObserver;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.lmax.disruptor.EventHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LiveStreamMediatorImplTest {

    @Mock private EventHandler<ObjectEvent<SubscribeStreamResponse>> observer1;
    @Mock private EventHandler<ObjectEvent<SubscribeStreamResponse>> observer2;
    @Mock private EventHandler<ObjectEvent<SubscribeStreamResponse>> observer3;

    @Mock private BlockWriter<BlockItem> blockWriter;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse> streamObserver1;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse> streamObserver2;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse> streamObserver3;

    @Mock
    private ServerCallStreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
            serverCallStreamObserver;

    @Mock private InstantSource testClock;

    private final long TIMEOUT_THRESHOLD_MILLIS = 100L;
    private final long TEST_TIME = 1_719_427_664_950L;

    private static final int testTimeout = 1000;

    private final BlockNodeContext testContext;

    public LiveStreamMediatorImplTest() throws IOException {
        this.testContext =
                TestConfigUtil.getTestBlockNodeContext(
                        Map.of(
                                TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY,
                                String.valueOf(TIMEOUT_THRESHOLD_MILLIS)));
    }

    @Test
    public void testUnsubscribeEach() throws InterruptedException, IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediatorBuilder =
                LiveStreamMediatorBuilder.newBuilder(
                        blockWriter, blockNodeContext, new ServiceStatusImpl());
        final var streamMediator = streamMediatorBuilder.build();

        // Set up the subscribers
        streamMediator.subscribe(observer1);
        streamMediator.subscribe(observer2);
        streamMediator.subscribe(observer3);

        assertTrue(
                streamMediator.isSubscribed(observer1),
                "Expected the mediator to have observer1 subscribed");
        assertTrue(
                streamMediator.isSubscribed(observer2),
                "Expected the mediator to have observer2 subscribed");
        assertTrue(
                streamMediator.isSubscribed(observer3),
                "Expected the mediator to have observer3 subscribed");

        Thread.sleep(testTimeout);

        streamMediator.unsubscribe(observer1);
        assertFalse(
                streamMediator.isSubscribed(observer1),
                "Expected the mediator to have unsubscribed observer1");

        streamMediator.unsubscribe(observer2);
        assertFalse(
                streamMediator.isSubscribed(observer2),
                "Expected the mediator to have unsubscribed observer2");

        streamMediator.unsubscribe(observer3);
        assertFalse(
                streamMediator.isSubscribed(observer3),
                "Expected the mediator to have unsubscribed observer3");

        // Confirm the counter was never incremented
        assertEquals(0, blockNodeContext.metricsService().get(LiveBlockItems).get());
    }

    @Test
    public void testMediatorPersistenceWithoutSubscribers() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(
                                blockWriter, blockNodeContext, new ServiceStatusImpl())
                        .build();
        final BlockItem blockItem = BlockItem.newBuilder().build();

        // Acting as a producer, notify the mediator of a new block
        streamMediator.publish(blockItem);

        // Verify the counter was incremented
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Confirm the BlockStorage write method was
        // called despite the absence of subscribers
        verify(blockWriter, timeout(testTimeout).times(1)).write(blockItem);
    }

    @Test
    public void testMediatorPublishEventToSubscribers() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(
                                blockWriter, blockNodeContext, new ServiceStatusImpl())
                        .build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var concreteObserver1 =
                new ConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, streamObserver1);

        final var concreteObserver2 =
                new ConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, streamObserver2);

        final var concreteObserver3 =
                new ConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, streamObserver3);

        // Set up the subscribers
        streamMediator.subscribe(concreteObserver1);
        streamMediator.subscribe(concreteObserver2);
        streamMediator.subscribe(concreteObserver3);

        assertTrue(
                streamMediator.isSubscribed(concreteObserver1),
                "Expected the mediator to have observer1 subscribed");
        assertTrue(
                streamMediator.isSubscribed(concreteObserver2),
                "Expected the mediator to have observer2 subscribed");
        assertTrue(
                streamMediator.isSubscribed(concreteObserver3),
                "Expected the mediator to have observer3 subscribed");

        final BlockHeader blockHeader = BlockHeader.newBuilder().number(1).build();
        final BlockItem blockItem = BlockItem.newBuilder().blockHeader(blockHeader).build();
        final SubscribeStreamResponse subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().blockItem(blockItem).build();

        // Acting as a producer, notify the mediator of a new block
        streamMediator.publish(blockItem);

        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Confirm each subscriber was notified of the new block
        verify(streamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(streamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(streamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));

        // Confirm the BlockStorage write method was called
        verify(blockWriter).write(blockItem);
    }

    @Test
    public void testSubAndUnsubHandling() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(
                                blockWriter, blockNodeContext, new ServiceStatusImpl())
                        .build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var concreteObserver1 =
                new ConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, streamObserver1);

        final var concreteObserver2 =
                new ConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, streamObserver2);

        final var concreteObserver3 =
                new ConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, streamObserver3);

        // Set up the subscribers
        streamMediator.subscribe(concreteObserver1);
        streamMediator.subscribe(concreteObserver2);
        streamMediator.subscribe(concreteObserver3);

        streamMediator.unsubscribe(concreteObserver1);
        streamMediator.unsubscribe(concreteObserver2);
        streamMediator.unsubscribe(concreteObserver3);

        // Confirm the counter was never incremented
        assertEquals(0, blockNodeContext.metricsService().get(LiveBlockItems).get());
    }

    @Test
    public void testOnCancelSubscriptionHandling() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(
                                blockWriter, blockNodeContext, new ServiceStatusImpl())
                        .build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var testConsumerBlockItemObserver =
                new TestConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, serverCallStreamObserver);

        streamMediator.subscribe(testConsumerBlockItemObserver);
        assertTrue(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Simulate the producer notifying the mediator of a new block
        final List<BlockItem> blockItems = generateBlockItems(1);
        streamMediator.publish(blockItems.getFirst());

        // Simulate the consumer cancelling the stream
        testConsumerBlockItemObserver.getOnCancel().run();

        // Verify the block item incremented the counter
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Verify the event made it to the consumer
        verify(serverCallStreamObserver, timeout(testTimeout).times(1)).setOnCancelHandler(any());

        // Confirm the mediator unsubscribed the consumer
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));
    }

    @Test
    public void testOnCloseSubscriptionHandling() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(
                                blockWriter, blockNodeContext, new ServiceStatusImpl())
                        .build();

        // testClock configured to be outside the timeout window
        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS + 1);

        final var testConsumerBlockItemObserver =
                new TestConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, serverCallStreamObserver);

        streamMediator.subscribe(testConsumerBlockItemObserver);
        assertTrue(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Simulate the producer notifying the mediator of a new block
        final List<BlockItem> blockItems = generateBlockItems(1);
        streamMediator.publish(blockItems.getFirst());

        // Simulate the consumer completing the stream
        testConsumerBlockItemObserver.getOnClose().run();

        // Verify the block item incremented the counter
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Verify the event made it to the consumer
        verify(serverCallStreamObserver, timeout(testTimeout).times(1)).setOnCancelHandler(any());

        // Confirm the mediator unsubscribed the consumer
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));
    }

    @Test
    public void testMediatorBlocksPublishAfterException() throws IOException {
        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(
                                blockWriter, blockNodeContext, new ServiceStatusImpl())
                        .build();

        final List<BlockItem> blockItems = generateBlockItems(1);
        final BlockItem firstBlockItem = blockItems.getFirst();

        // Right now, only a single producer calls publishEvent. In
        // that case, they will get an IOException bubbled up to them.
        // However, we will need to support multiple producers in the
        // future. In that case, we need to make sure a second producer
        // is not able to publish a block after the first producer fails.
        doThrow(new IOException()).when(blockWriter).write(firstBlockItem);
        try {
            streamMediator.publish(firstBlockItem);
            fail("Expected an IOException to be thrown");
        } catch (IOException e) {

            final BlockItem secondBlockItem = blockItems.get(1);
            streamMediator.publish(secondBlockItem);

            // Confirm the counter was incremented only once
            assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

            // Confirm the BlockPersistenceHandler write method was only called
            // once despite the second block being published.
            verify(blockWriter, timeout(testTimeout).times(1)).write(firstBlockItem);
        }
    }

    @Test
    public void testUnsubscribeWhenNotSubscribed() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(
                                blockWriter, blockNodeContext, new ServiceStatusImpl())
                        .build();
        final var testConsumerBlockItemObserver =
                new TestConsumerStreamResponseObserver(
                        testContext, testClock, streamMediator, serverCallStreamObserver);

        // Confirm the observer is not subscribed
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Attempt to unsubscribe the observer
        streamMediator.unsubscribe(testConsumerBlockItemObserver);

        // Confirm the observer is still not subscribed
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));
    }

    private static class TestConsumerStreamResponseObserver extends ConsumerStreamResponseObserver {
        public TestConsumerStreamResponseObserver(
                BlockNodeContext context,
                final InstantSource producerLivenessClock,
                final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>>
                        streamMediator,
                final StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
                        responseStreamObserver) {
            super(context, producerLivenessClock, streamMediator, responseStreamObserver);
        }

        @NonNull
        public Runnable getOnCancel() {
            return onCancel;
        }

        @NonNull
        public Runnable getOnClose() {
            return onClose;
        }
    }
}
