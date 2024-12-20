// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.mediator;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItems;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockStreamMediatorError;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerStreamResponseObserver;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.notifier.NotifierImpl;
import com.hedera.block.server.persistence.StreamPersistenceHandlerImpl;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.service.ServiceStatusImpl;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemSetUnparsed;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.swirlds.metrics.api.LongGauge;
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

    @Mock
    private BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> observer1;

    @Mock
    private BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> observer2;

    @Mock
    private BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> observer3;

    @Mock
    private BlockWriter<List<BlockItemUnparsed>> blockWriter;

    @Mock
    private Notifier notifier;

    @Mock
    private Pipeline<? super SubscribeStreamResponseUnparsed> helidonSubscribeStreamObserver1;

    @Mock
    private Pipeline<? super SubscribeStreamResponseUnparsed> helidonSubscribeStreamObserver2;

    @Mock
    private Pipeline<? super SubscribeStreamResponseUnparsed> helidonSubscribeStreamObserver3;

    @Mock
    private InstantSource testClock;

    private final long TIMEOUT_THRESHOLD_MILLIS = 100L;
    private final long TEST_TIME = 1_719_427_664_950L;

    private static final int testTimeout = 1000;

    private final BlockNodeContext testContext;

    public LiveStreamMediatorImplTest() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put(TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY, String.valueOf(TIMEOUT_THRESHOLD_MILLIS));
        properties.put(TestConfigUtil.MEDIATOR_RING_BUFFER_SIZE_KEY, String.valueOf(1024));

        this.testContext = TestConfigUtil.getTestBlockNodeContext(properties);
    }

    @Test
    public void testUnsubscribeEach() throws InterruptedException, IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediatorBuilder =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, new ServiceStatusImpl(blockNodeContext));
        final var streamMediator = streamMediatorBuilder.build();

        // Set up the subscribers
        streamMediator.subscribe(observer1);
        streamMediator.subscribe(observer2);
        streamMediator.subscribe(observer3);

        assertTrue(streamMediator.isSubscribed(observer1), "Expected the mediator to have observer1 subscribed");
        assertTrue(streamMediator.isSubscribed(observer2), "Expected the mediator to have observer2 subscribed");
        assertTrue(streamMediator.isSubscribed(observer3), "Expected the mediator to have observer3 subscribed");

        Thread.sleep(100);

        streamMediator.unsubscribe(observer1);
        assertFalse(streamMediator.isSubscribed(observer1), "Expected the mediator to have unsubscribed observer1");

        streamMediator.unsubscribe(observer2);
        assertFalse(streamMediator.isSubscribed(observer2), "Expected the mediator to have unsubscribed observer2");

        streamMediator.unsubscribe(observer3);
        assertFalse(streamMediator.isSubscribed(observer3), "Expected the mediator to have unsubscribed observer3");

        // Confirm the counter was never incremented
        assertEquals(0, blockNodeContext.metricsService().get(LiveBlockItems).get());
    }

    @Test
    public void testMediatorPersistenceWithoutSubscribers() throws IOException, ParseException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .build();

        final BlockItemUnparsed blockItem = BlockItemUnparsed.newBuilder().build();

        // register the stream validator
        when(blockWriter.write(List.of(blockItem))).thenReturn(Optional.empty());
        final var handler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        streamMediator.subscribe(handler);

        // Acting as a producer, notify the mediator of a new block
        streamMediator.publish(List.of(blockItem));

        // Verify the counter was incremented
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Confirm the BlockStorage write method was
        // called despite the absence of subscribers
        verify(blockWriter, timeout(testTimeout).times(1)).write(List.of(blockItem));
    }

    @Test
    public void testMediatorPublishEventToSubscribers() throws IOException, ParseException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var concreteObserver1 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver1, testContext);

        final var concreteObserver2 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver2, testContext);

        final var concreteObserver3 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver3, testContext);

        // Set up the subscribers
        streamMediator.subscribe(concreteObserver1);
        streamMediator.subscribe(concreteObserver2);
        streamMediator.subscribe(concreteObserver3);

        assertTrue(
                streamMediator.isSubscribed(concreteObserver1), "Expected the mediator to have observer1 subscribed");
        assertTrue(
                streamMediator.isSubscribed(concreteObserver2), "Expected the mediator to have observer2 subscribed");
        assertTrue(
                streamMediator.isSubscribed(concreteObserver3), "Expected the mediator to have observer3 subscribed");

        final BlockItemUnparsed blockItem = BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(
                        BlockHeader.newBuilder().number(1).build()))
                .build();
        final SubscribeStreamResponseUnparsed subscribeStreamResponse = SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(
                        BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build())
                .build();

        // register the stream validator
        when(blockWriter.write(List.of(blockItem))).thenReturn(Optional.empty());
        final var handler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        streamMediator.subscribe(handler);

        // Acting as a producer, notify the mediator of a new block
        streamMediator.publish(List.of(blockItem));

        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Confirm each subscriber was notified of the new block
        verify(helidonSubscribeStreamObserver1, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
        verify(helidonSubscribeStreamObserver2, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
        verify(helidonSubscribeStreamObserver3, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);

        // Confirm the BlockStorage write method was called
        verify(blockWriter, timeout(testTimeout).times(1)).write(List.of(blockItem));
    }

    @Test
    public void testSubAndUnsubHandling() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var concreteObserver1 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver1, testContext);

        final var concreteObserver2 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver2, testContext);

        final var concreteObserver3 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver3, testContext);

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
    public void testSubscribeWhenHandlerAlreadySubscribed() throws IOException {
        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final LongGauge consumersGauge = blockNodeContext.metricsService().get(BlockNodeMetricTypes.Gauge.Consumers);
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .build();

        final var concreteObserver1 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver1, testContext);

        streamMediator.subscribe(concreteObserver1);
        assertTrue(streamMediator.isSubscribed(concreteObserver1));
        assertEquals(1L, consumersGauge.get());

        // Attempt to "re-subscribe" the observer
        // Should not increment the counter or change the implementation
        streamMediator.subscribe(concreteObserver1);
        assertTrue(streamMediator.isSubscribed(concreteObserver1));
        assertEquals(1L, consumersGauge.get());

        streamMediator.unsubscribe(concreteObserver1);
        assertFalse(streamMediator.isSubscribed(concreteObserver1));

        // Confirm the counter was decremented
        assertEquals(0L, consumersGauge.get());
    }

    //        @Test
    //        public void testOnCancelSubscriptionHandling() throws IOException {
    //
    //            final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    //            final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
    //            final var streamMediator =
    //                    LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();
    //
    //            when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);
    //
    //            final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(1);
    //
    //            // register the stream validator
    //            when(blockWriter.write(List.of(blockItems.getFirst()))).thenReturn(Optional.empty());
    //            final var streamValidator =
    //                    new StreamPersistenceHandlerImpl(
    //                            streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
    //            streamMediator.subscribe(streamValidator);
    //
    //            // register the test observer
    //            final var testConsumerBlockItemObserver =
    //                    new TestConsumerStreamResponseObserver(
    //                            testClock, streamMediator, serverCallStreamObserver, testContext);
    //
    //            streamMediator.subscribe(testConsumerBlockItemObserver);
    //            assertTrue(streamMediator.isSubscribed(testConsumerBlockItemObserver));
    //
    //            // Simulate the producer notifying the mediator of a new block
    //            streamMediator.publish(blockItems.getFirst());
    //
    //            // Simulate the consumer cancelling the stream
    //            testConsumerBlockItemObserver.getOnCancel().run();
    //
    //            // Verify the block item incremented the counter
    //            assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());
    //
    //            // Verify the event made it to the consumer
    //            verify(serverCallStreamObserver,
    //     timeout(testTimeout).times(1)).setOnCancelHandler(any());
    //
    //            // Confirm the mediator unsubscribed the consumer
    //            assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));
    //
    //            // Confirm the BlockStorage write method was called
    //            verify(blockWriter, timeout(testTimeout).times(1)).write(blockItems.getFirst());
    //
    //            // Confirm the stream validator is still subscribed
    //            assertTrue(streamMediator.isSubscribed(streamValidator));
    //        }

    //    @Test
    //    public void testOnCloseSubscriptionHandling() throws IOException {
    //
    //        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    //        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
    //        final var streamMediator =
    //                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();
    //
    //        // testClock configured to be outside the timeout window
    //        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS +
    // 1);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(1);
    //
    //        // register the stream validator
    //        when(blockWriter.write(blockItems.getFirst())).thenReturn(Optional.empty());
    //        final var streamValidator =
    //                new StreamPersistenceHandlerImpl(
    //                        streamMediator, notifier, blockWriter, blockNodeContext,
    // serviceStatus);
    //        streamMediator.subscribe(streamValidator);
    //
    //        final var testConsumerBlockItemObserver =
    //                new TestConsumerStreamResponseObserver(
    //                        testClock, streamMediator, serverCallStreamObserver, testContext);
    //
    //        streamMediator.subscribe(testConsumerBlockItemObserver);
    //        assertTrue(streamMediator.isSubscribed(testConsumerBlockItemObserver));
    //
    //        // Simulate the producer notifying the mediator of a new block
    //        streamMediator.publish(blockItems.getFirst());
    //
    //        // Simulate the consumer completing the stream
    //        testConsumerBlockItemObserver.getOnClose().run();
    //
    //        // Verify the block item incremented the counter
    //        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());
    //
    //        // Verify the event made it to the consumer
    //        verify(serverCallStreamObserver,
    // timeout(testTimeout).times(1)).setOnCancelHandler(any());
    //
    //        // Confirm the mediator unsubscribed the consumer
    //        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));
    //
    //        // Confirm the BlockStorage write method was called
    //        verify(blockWriter, timeout(testTimeout).times(1)).write(blockItems.getFirst());
    //
    //        // Confirm the stream validator is still subscribed
    //        assertTrue(streamMediator.isSubscribed(streamValidator));
    //    }

    @Test
    public void testMediatorBlocksPublishAfterException() throws IOException, InterruptedException, ParseException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .build();

        final var concreteObserver1 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver1, testContext);

        final var concreteObserver2 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver2, testContext);

        final var concreteObserver3 = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver3, testContext);

        // Set up the subscribers
        streamMediator.subscribe(concreteObserver1);
        streamMediator.subscribe(concreteObserver2);
        streamMediator.subscribe(concreteObserver3);

        final Notifier notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final var handler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);

        // Set up the stream verifier
        streamMediator.subscribe(handler);

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(1);
        final BlockItemUnparsed firstBlockItem = blockItems.getFirst();

        // Right now, only a single producer calls publishEvent. In
        // that case, they will get an IOException bubbled up to them.
        // However, we will need to support multiple producers in the
        // future. In that case, we need to make sure a second producer
        // is not able to publish a block after the first producer fails.
        doThrow(new IOException()).when(blockWriter).write(List.of(firstBlockItem));

        streamMediator.publish(List.of(firstBlockItem));

        Thread.sleep(testTimeout);

        // Confirm the counter was incremented only once
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Confirm the error counter was incremented
        assertEquals(
                1,
                blockNodeContext
                        .metricsService()
                        .get(LiveBlockStreamMediatorError)
                        .get());

        // Send another block item after the exception
        streamMediator.publish(List.of(blockItems.get(1)));
        final BlockItemSetUnparsed blockItemSet =
                BlockItemSetUnparsed.newBuilder().blockItems(firstBlockItem).build();
        final var subscribeStreamResponse = SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(blockItemSet)
                .build();
        verify(helidonSubscribeStreamObserver1, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
        verify(helidonSubscribeStreamObserver2, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
        verify(helidonSubscribeStreamObserver3, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);

        // TODO: Replace READ_STREAM_SUCCESS (2) with a generic error code?
        final var endOfStreamResponse = SubscribeStreamResponseUnparsed.newBuilder()
                .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                .build();
        verify(helidonSubscribeStreamObserver1, timeout(testTimeout).times(1)).onNext(endOfStreamResponse);
        verify(helidonSubscribeStreamObserver2, timeout(testTimeout).times(1)).onNext(endOfStreamResponse);
        verify(helidonSubscribeStreamObserver3, timeout(testTimeout).times(1)).onNext(endOfStreamResponse);

        // verify write method only called once despite the second block being published.
        verify(blockWriter, timeout(testTimeout).times(1)).write(List.of(firstBlockItem));
    }

    @Test
    public void testUnsubscribeWhenNotSubscribed() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .build();

        // register the stream validator
        final var handler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        streamMediator.subscribe(handler);

        final var testConsumerBlockItemObserver = new ConsumerStreamResponseObserver(
                testClock, streamMediator, helidonSubscribeStreamObserver1, testContext);

        // Confirm the observer is not subscribed
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Attempt to unsubscribe the observer
        streamMediator.unsubscribe(testConsumerBlockItemObserver);

        // Confirm the observer is still not subscribed
        assertFalse(streamMediator.isSubscribed(testConsumerBlockItemObserver));

        // Confirm the stream validator is still subscribed
        assertTrue(streamMediator.isSubscribed(handler));
    }

    //        private static class TestConsumerStreamResponseObserver extends ConsumerStreamResponseObserver {
    //            public TestConsumerStreamResponseObserver(
    //                    @NonNull final InstantSource producerLivenessClock,
    //                    @NonNull final StreamMediator<List<BlockItemUnparsed>, SubscribeStreamResponseUnparsed>
    // streamMediator,
    //                    @NonNull final Pipeline<SubscribeStreamResponseUnparsed> responseStreamObserver,
    //                    @NonNull final BlockNodeContext blockNodeContext) {
    //                super(producerLivenessClock, streamMediator, responseStreamObserver, blockNodeContext);
    //            }
    //        }
}
