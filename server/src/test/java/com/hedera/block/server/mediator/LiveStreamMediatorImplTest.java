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
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItems;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockStreamMediatorError;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerStreamResponseObserver;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.notifier.NotifierBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.service.ServiceStatusImpl;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.verifier.StreamVerifierBuilder;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.output.BlockHeader;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.InstantSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LiveStreamMediatorImplTest {

    @Mock private BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> observer1;
    @Mock private BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> observer2;
    @Mock private BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> observer3;

    @Mock private BlockWriter<BlockItem> blockWriter;
    @Mock private Notifier notifier;

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
        Map<String, String> properties = new HashMap<>();
        properties.put(
                TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY,
                String.valueOf(TIMEOUT_THRESHOLD_MILLIS));
        properties.put(TestConfigUtil.MEDIATOR_RING_BUFFER_SIZE_KEY, String.valueOf(1024));

        this.testContext = TestConfigUtil.getTestBlockNodeContext(properties);
    }

    @Test
    public void testUnsubscribeEach() throws InterruptedException, IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediatorBuilder =
                LiveStreamMediatorBuilder.newBuilder(
                        blockNodeContext, new ServiceStatusImpl(blockNodeContext));
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

        Thread.sleep(100);

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
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();
        final BlockItem blockItem = BlockItem.newBuilder().build();

        // register the stream validator
        when(blockWriter.write(blockItem)).thenReturn(Optional.empty());
        final var streamValidator =
                StreamVerifierBuilder.newBuilder(blockWriter, blockNodeContext, serviceStatus)
                        .notifier(notifier)
                        .subscriptionHandler(streamMediator)
                        .build();
        streamMediator.subscribe(streamValidator);

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
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var concreteObserver1 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver1, testContext);

        final var concreteObserver2 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver2, testContext);

        final var concreteObserver3 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver3, testContext);

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

        // register the stream validator
        when(blockWriter.write(blockItem)).thenReturn(Optional.empty());
        final var streamValidator =
                StreamVerifierBuilder.newBuilder(blockWriter, blockNodeContext, serviceStatus)
                        .notifier(notifier)
                        .subscriptionHandler(streamMediator)
                        .build();
        streamMediator.subscribe(streamValidator);

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
        verify(blockWriter, timeout(testTimeout).times(1)).write(blockItem);
    }

    @Test
    public void testSubAndUnsubHandling() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var concreteObserver1 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver1, testContext);

        final var concreteObserver2 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver2, testContext);

        final var concreteObserver3 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver3, testContext);

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
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final List<BlockItem> blockItems = generateBlockItems(1);

        // register the stream validator
        when(blockWriter.write(blockItems.getFirst())).thenReturn(Optional.empty());
        final var streamValidator =
                StreamVerifierBuilder.newBuilder(blockWriter, blockNodeContext, serviceStatus)
                        .notifier(notifier)
                        .subscriptionHandler(streamMediator)
                        .build();
        streamMediator.subscribe(streamValidator);

        // register the test observer
        final var testConsumerBlockItemObserver =
                new TestConsumerStreamResponseObserver(
                        testClock, streamMediator, serverCallStreamObserver, testContext);

        streamMediator.subscribe(testConsumerBlockItemObserver);
        assertTrue(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Simulate the producer notifying the mediator of a new block
        streamMediator.publish(blockItems.getFirst());

        // Simulate the consumer cancelling the stream
        testConsumerBlockItemObserver.getOnCancel().run();

        // Verify the block item incremented the counter
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Verify the event made it to the consumer
        verify(serverCallStreamObserver, timeout(testTimeout).times(1)).setOnCancelHandler(any());

        // Confirm the mediator unsubscribed the consumer
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Confirm the BlockStorage write method was called
        verify(blockWriter, timeout(testTimeout).times(1)).write(blockItems.getFirst());

        // Confirm the stream validator is still subscribed
        assertTrue(streamMediator.isSubscribed(streamValidator));
    }

    @Test
    public void testOnCloseSubscriptionHandling() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();

        // testClock configured to be outside the timeout window
        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS + 1);

        final List<BlockItem> blockItems = generateBlockItems(1);

        // register the stream validator
        when(blockWriter.write(blockItems.getFirst())).thenReturn(Optional.empty());
        final var streamValidator =
                StreamVerifierBuilder.newBuilder(blockWriter, blockNodeContext, serviceStatus)
                        .notifier(notifier)
                        .subscriptionHandler(streamMediator)
                        .build();
        streamMediator.subscribe(streamValidator);

        final var testConsumerBlockItemObserver =
                new TestConsumerStreamResponseObserver(
                        testClock, streamMediator, serverCallStreamObserver, testContext);

        streamMediator.subscribe(testConsumerBlockItemObserver);
        assertTrue(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Simulate the producer notifying the mediator of a new block
        streamMediator.publish(blockItems.getFirst());

        // Simulate the consumer completing the stream
        testConsumerBlockItemObserver.getOnClose().run();

        // Verify the block item incremented the counter
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Verify the event made it to the consumer
        verify(serverCallStreamObserver, timeout(testTimeout).times(1)).setOnCancelHandler(any());

        // Confirm the mediator unsubscribed the consumer
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Confirm the BlockStorage write method was called
        verify(blockWriter, timeout(testTimeout).times(1)).write(blockItems.getFirst());

        // Confirm the stream validator is still subscribed
        assertTrue(streamMediator.isSubscribed(streamValidator));
    }

    @Test
    public void testMediatorBlocksPublishAfterException() throws IOException, InterruptedException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();

        final var concreteObserver1 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver1, testContext);

        final var concreteObserver2 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver2, testContext);

        final var concreteObserver3 =
                new ConsumerStreamResponseObserver(
                        testClock, streamMediator, streamObserver3, testContext);

        // Set up the subscribers
        streamMediator.subscribe(concreteObserver1);
        streamMediator.subscribe(concreteObserver2);
        streamMediator.subscribe(concreteObserver3);

        final Notifier notifier =
                NotifierBuilder.newBuilder(streamMediator, blockNodeContext, serviceStatus).build();
        final var streamValidator =
                StreamVerifierBuilder.newBuilder(blockWriter, blockNodeContext, serviceStatus)
                        .notifier(notifier)
                        .subscriptionHandler(streamMediator)
                        .build();

        // Set up the stream verifier
        streamMediator.subscribe(streamValidator);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final BlockItem firstBlockItem = blockItems.getFirst();

        // Right now, only a single producer calls publishEvent. In
        // that case, they will get an IOException bubbled up to them.
        // However, we will need to support multiple producers in the
        // future. In that case, we need to make sure a second producer
        // is not able to publish a block after the first producer fails.
        doThrow(new IOException()).when(blockWriter).write(firstBlockItem);

        streamMediator.publish(firstBlockItem);

        Thread.sleep(testTimeout);

        // Confirm the counter was incremented only once
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Confirm the error counter was incremented
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockStreamMediatorError).get());

        // Send another block item after the exception
        streamMediator.publish(blockItems.get(1));

        final var subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().blockItem(firstBlockItem).build();
        verify(streamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(streamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(streamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));

        // TODO: Replace READ_STREAM_SUCCESS (2) with a generic error code?
        final SubscribeStreamResponse endOfStreamResponse =
                SubscribeStreamResponse.newBuilder()
                        .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                        .build();
        verify(streamObserver1, timeout(testTimeout).times(1)).onNext(fromPbj(endOfStreamResponse));
        verify(streamObserver2, timeout(testTimeout).times(1)).onNext(fromPbj(endOfStreamResponse));
        verify(streamObserver3, timeout(testTimeout).times(1)).onNext(fromPbj(endOfStreamResponse));

        // verify write method only called once despite the second block being published.
        verify(blockWriter, timeout(testTimeout).times(1)).write(firstBlockItem);
    }

    @Test
    public void testUnsubscribeWhenNotSubscribed() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();

        // register the stream validator
        final var streamValidator =
                StreamVerifierBuilder.newBuilder(blockWriter, blockNodeContext, serviceStatus)
                        .notifier(notifier)
                        .subscriptionHandler(streamMediator)
                        .build();
        streamMediator.subscribe(streamValidator);

        final var testConsumerBlockItemObserver =
                new TestConsumerStreamResponseObserver(
                        testClock, streamMediator, serverCallStreamObserver, testContext);

        // Confirm the observer is not subscribed
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Attempt to unsubscribe the observer
        streamMediator.unsubscribe(testConsumerBlockItemObserver);

        // Confirm the observer is still not subscribed
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Confirm the stream validator is still subscribed
        assertTrue(streamMediator.isSubscribed(streamValidator));
    }

    private static class TestConsumerStreamResponseObserver extends ConsumerStreamResponseObserver {
        public TestConsumerStreamResponseObserver(
                @NonNull final InstantSource producerLivenessClock,
                @NonNull final StreamMediator<BlockItem, SubscribeStreamResponse> streamMediator,
                @NonNull
                        final StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
                                responseStreamObserver,
                @NonNull final BlockNodeContext blockNodeContext) {
            super(producerLivenessClock, streamMediator, responseStreamObserver, blockNodeContext);
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
