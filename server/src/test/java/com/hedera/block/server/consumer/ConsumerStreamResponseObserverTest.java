// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemSetUnparsed;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.input.EventHeader;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
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
    private StreamMediator<BlockItemUnparsed, List<BlockItemUnparsed>> streamMediator;

    @Mock
    private Pipeline<? super SubscribeStreamResponseUnparsed> responseStreamObserver;

    @Mock
    private ObjectEvent<List<BlockItemUnparsed>> objectEvent;

    @Mock
    private InstantSource testClock;

    final BlockNodeContext testContext;

    private CompletionService<Void> completionService;

    @BeforeEach
    public void setUp() {
        completionService = new ExecutorCompletionService<>(Executors.newSingleThreadExecutor());
    }

    public ConsumerStreamResponseObserverTest() throws IOException {
        this.testContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY, String.valueOf(TIMEOUT_THRESHOLD_MILLIS)));
    }

    @Test
    public void testProducerTimeoutWithinWindow() throws Exception {

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var consumerBlockItemObserver = LiveStreamEventHandlerBuilder.build(
                completionService,
                testClock,
                streamMediator,
                responseStreamObserver,
                testContext.metricsService(),
                testContext.configuration());

        final BlockHeader blockHeader = BlockHeader.newBuilder().number(1).build();
        final BlockItemUnparsed blockItem = BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(blockHeader))
                .build();

        List<BlockItemUnparsed> blockItems = List.of(blockItem);
        when(objectEvent.get()).thenReturn(blockItems);

        consumerBlockItemObserver.onEvent(objectEvent, 0, true);

        final BlockItemSetUnparsed blockItemSet =
                BlockItemSetUnparsed.newBuilder().blockItems(blockItems).build();
        final SubscribeStreamResponseUnparsed subscribeStreamResponse = SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(blockItemSet)
                .build();

        // verify the observer is called with the next BlockItem
        verify(responseStreamObserver, timeout(testTimeout)).onNext(subscribeStreamResponse);

        // verify the mediator is NOT called to unsubscribe the observer
        verify(streamMediator, timeout(testTimeout).times(0)).unsubscribe(consumerBlockItemObserver);
    }

    @Test
    public void testProducerTimeoutOutsideWindow() throws Exception {

        // Mock a clock with 2 different return values in response to anticipated
        // millis() calls. Here the second call will always be outside the timeout window.
        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS + 1);

        final var consumerBlockItemObserver = LiveStreamEventHandlerBuilder.build(
                completionService,
                testClock,
                streamMediator,
                responseStreamObserver,
                testContext.metricsService(),
                testContext.configuration());

        final List<BlockItemUnparsed> blockItems =
                List.of(BlockItemUnparsed.newBuilder().build());
        final ObjectEvent<List<BlockItemUnparsed>> objectEvent = new ObjectEvent<>();
        objectEvent.set(blockItems);
        consumerBlockItemObserver.onEvent(objectEvent, 0, true);
        verify(streamMediator, timeout(testTimeout)).unsubscribe(consumerBlockItemObserver);
    }

    @Test
    public void testConsumerNotToSendBeforeBlockHeader() throws Exception {

        // Mock a clock with 2 different return values in response to anticipated
        // millis() calls. Here the second call will always be inside the timeout window.
        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var consumerBlockItemObserver = LiveStreamEventHandlerBuilder.build(
                completionService,
                testClock,
                streamMediator,
                responseStreamObserver,
                testContext.metricsService(),
                testContext.configuration());

        // Send non-header BlockItems to validate that the observer does not send them
        for (int i = 1; i <= 10; i++) {

            if (i % 2 == 0) {
                final Bytes eventHeader =
                        EventHeader.PROTOBUF.toBytes(EventHeader.newBuilder().build());
                final BlockItemUnparsed blockItem =
                        BlockItemUnparsed.newBuilder().eventHeader(eventHeader).build();
                lenient().when(objectEvent.get()).thenReturn(List.of(blockItem));
            } else {
                final Bytes blockProof = BlockProof.PROTOBUF.toBytes(
                        BlockProof.newBuilder().block(i).build());
                final BlockItemUnparsed blockItem =
                        BlockItemUnparsed.newBuilder().blockProof(blockProof).build();
                when(objectEvent.get()).thenReturn(List.of(blockItem));
            }

            consumerBlockItemObserver.onEvent(objectEvent, 0, true);
        }

        final BlockItemUnparsed blockItem = BlockItemUnparsed.newBuilder().build();
        final BlockItemSetUnparsed blockItemSet =
                BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build();
        final SubscribeStreamResponseUnparsed subscribeStreamResponse = SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(blockItemSet)
                .build();

        // Confirm that the observer was called with the next BlockItem
        // since we never send a BlockItem with a Header to start the stream.
        verify(responseStreamObserver, timeout(testTimeout).times(0)).onNext(subscribeStreamResponse);
    }

    @Test
    public void testUncheckedIOExceptionException() throws Exception {
        final BlockHeader blockHeader = BlockHeader.newBuilder().number(1).build();
        final BlockItemUnparsed blockItem = BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(blockHeader))
                .build();

        when(objectEvent.get()).thenReturn(List.of(blockItem));

        final BlockItemSetUnparsed blockItemSet =
                BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build();
        final SubscribeStreamResponseUnparsed subscribeStreamResponse = SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(blockItemSet)
                .build();
        doThrow(UncheckedIOException.class).when(responseStreamObserver).onNext(subscribeStreamResponse);

        final var consumerBlockItemObserver = LiveStreamEventHandlerBuilder.build(
                completionService,
                testClock,
                streamMediator,
                responseStreamObserver,
                testContext.metricsService(),
                testContext.configuration());

        // This call will throw an exception but, because of the async
        // service executor, the exception will not get caught until the
        // next call.
        consumerBlockItemObserver.onEvent(objectEvent, 0, true);
        Thread.sleep(testTimeout);

        // This second call will throw the exception.
        consumerBlockItemObserver.onEvent(objectEvent, 0, true);

        verify(streamMediator, timeout(testTimeout).times(1)).unsubscribe(any());
    }

    @Test
    public void testRuntimeException() throws Exception {
        final BlockHeader blockHeader = BlockHeader.newBuilder().number(1).build();
        final BlockItemUnparsed blockItem = BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(blockHeader))
                .build();
        final BlockItemSetUnparsed blockItemSet =
                BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build();
        final SubscribeStreamResponseUnparsed subscribeStreamResponse = SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(blockItemSet)
                .build();
        when(objectEvent.get()).thenReturn(List.of(blockItem));
        doThrow(RuntimeException.class).when(responseStreamObserver).onNext(subscribeStreamResponse);

        final var consumerBlockItemObserver = LiveStreamEventHandlerBuilder.build(
                completionService,
                testClock,
                streamMediator,
                responseStreamObserver,
                testContext.metricsService(),
                testContext.configuration());

        // This call will throw an exception but, because of the async
        // service executor, the exception will not get caught until the
        // next call.
        consumerBlockItemObserver.onEvent(objectEvent, 0, true);
        Thread.sleep(testTimeout);

        // This second call will throw the exception.
        consumerBlockItemObserver.onEvent(objectEvent, 0, true);

        verify(streamMediator, timeout(testTimeout).times(1)).unsubscribe(any());
    }
}
