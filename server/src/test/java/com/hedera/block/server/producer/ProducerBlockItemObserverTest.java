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

package com.hedera.block.server.producer;

import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItems;
import static com.hedera.block.server.producer.Util.getFakeHash;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static com.hedera.block.server.util.PersistTestUtils.reverseByteArray;
import static com.hedera.block.server.util.TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.ServiceStatusImpl;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.consumer.ConsumerStreamResponseObserver;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.LiveStreamMediatorBuilder;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.ItemAcknowledgement;
import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProducerBlockItemObserverTest {

    @Mock private SubscriptionHandler<ObjectEvent<PublishStreamResponse>> subscriptionHandler;

    @Mock private StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> streamMediator;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
            publishStreamResponseObserver;

    @Mock private BlockWriter<BlockItem> blockWriter;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse> streamObserver1;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse> streamObserver2;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse> streamObserver3;

    @Mock private ServiceStatus serviceStatus;
    @Mock private InstantSource testClock;
    private static final int testTimeout = 1000;

    @Test
    @Disabled
    public void testProducerOnNext() throws IOException, NoSuchAlgorithmException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final List<BlockItem> blockItems = generateBlockItems(1);
        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        testClock,
                        streamMediator,
                        subscriptionHandler,
                        publishStreamResponseObserver,
                        blockNodeContext,
                        serviceStatus);

        when(serviceStatus.isRunning()).thenReturn(true);

        final BlockItem blockHeader = blockItems.getFirst();
        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().blockItem(blockHeader).build();
        producerBlockItemObserver.onNext(fromPbj(publishStreamRequest));

        verify(streamMediator, timeout(testTimeout).times(1)).publish(blockHeader);

        final Acknowledgement ack = buildAck(blockHeader);
        final PublishStreamResponse publishStreamResponse =
                PublishStreamResponse.newBuilder().acknowledgement(ack).build();

        verify(publishStreamResponseObserver, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));

        // Helidon will call onCompleted after onNext
        producerBlockItemObserver.onCompleted();

        verify(publishStreamResponseObserver, timeout(testTimeout).times(1)).onCompleted();
    }

    @Test
    @Disabled
    public void testProducerWithManyConsumers() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, new ServiceStatusImpl())
                        .build();

        // Mock a clock with 2 different return values in response to anticipated
        // millis() calls. Here the second call will always be inside the timeout window.
        final BlockNodeContext testContext =
                TestConfigUtil.getTestBlockNodeContext(
                        Map.of(CONSUMER_TIMEOUT_THRESHOLD_KEY, "100"));
        final ConsumerConfig consumerConfig =
                testContext.configuration().getConfigData(ConsumerConfig.class);

        long TEST_TIME = 1_719_427_664_950L;
        when(testClock.millis())
                .thenReturn(TEST_TIME, TEST_TIME + consumerConfig.timeoutThresholdMillis());

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

        when(serviceStatus.isRunning()).thenReturn(true);

        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        testClock,
                        streamMediator,
                        subscriptionHandler,
                        publishStreamResponseObserver,
                        blockNodeContext,
                        serviceStatus);

        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().blockItem(blockItem).build();
        producerBlockItemObserver.onNext(fromPbj(publishStreamRequest));

        // Confirm the block item counter was incremented
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        // Confirm each subscriber was notified of the new block
        verify(streamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(streamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(streamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));

        // Confirm the BlockStorage write method was
        // called despite the absence of subscribers
        verify(blockWriter).write(blockItem);
    }

    @Test
    public void testOnError() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        testClock,
                        streamMediator,
                        subscriptionHandler,
                        publishStreamResponseObserver,
                        blockNodeContext,
                        serviceStatus);

        final Throwable t = new Throwable("Test error");
        producerBlockItemObserver.onError(t);
        verify(publishStreamResponseObserver).onError(t);
    }

    //    @Test
    //    public void testItemAckBuilderExceptionTest() throws IOException {
    //
    //        when(serviceStatus.isRunning()).thenReturn(true);
    //
    //        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    //        final ProducerBlockItemObserver testProducerBlockItemObserver =
    //                new TestProducerBlockItemObserver(
    //                        streamMediator,
    //                        publishStreamResponseObserver,
    //                        blockNodeContext,
    //                        serviceStatus);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(1);
    //        final BlockItem blockHeader = blockItems.getFirst();
    //        final PublishStreamRequest publishStreamRequest =
    //                PublishStreamRequest.newBuilder().blockItem(blockHeader).build();
    //        testProducerBlockItemObserver.onNext(fromPbj(publishStreamRequest));
    //
    //        final EndOfStream endOfStream =
    //                EndOfStream.newBuilder()
    //                        .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
    //                        .build();
    //        final PublishStreamResponse errorResponse =
    //                PublishStreamResponse.newBuilder().status(endOfStream).build();
    //        verify(publishStreamResponseObserver, timeout(testTimeout).times(1))
    //                .onNext(fromPbj(errorResponse));
    //    }

    @Test
    public void testBlockItemThrowsParseException()
            throws IOException, InvalidProtocolBufferException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        testClock,
                        streamMediator,
                        subscriptionHandler,
                        publishStreamResponseObserver,
                        blockNodeContext,
                        serviceStatus);

        // Create a pbj block item
        final List<BlockItem> blockItems = generateBlockItems(1);
        final BlockItem blockHeader = blockItems.getFirst();

        // Convert the block item to a protoc and add a spy to reverse the bytes to
        // provoke a ParseException
        final byte[] pbjBytes = BlockItem.PROTOBUF.toBytes(blockHeader).toByteArray();
        final com.hedera.hapi.block.stream.protoc.BlockItem protocBlockItem =
                spy(com.hedera.hapi.block.stream.protoc.BlockItem.parseFrom(pbjBytes));

        // set up the spy to pass the reversed bytes when called
        final byte[] reversedBytes = reverseByteArray(protocBlockItem.toByteArray());
        when(protocBlockItem.toByteArray()).thenReturn(reversedBytes);

        // create the PublishStreamRequest with the spy block item
        final com.hedera.hapi.block.protoc.PublishStreamRequest protocPublishStreamRequest =
                com.hedera.hapi.block.protoc.PublishStreamRequest.newBuilder()
                        .setBlockItem(protocBlockItem)
                        .build();

        // call the producerBlockItemObserver
        producerBlockItemObserver.onNext(protocPublishStreamRequest);

        // TODO: Replace this with a real error enum.
        final EndOfStream endOfStream =
                EndOfStream.newBuilder()
                        .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                        .build();
        fromPbj(PublishStreamResponse.newBuilder().status(endOfStream).build());

        // verify the ProducerBlockItemObserver has sent an error response
        verify(publishStreamResponseObserver, timeout(testTimeout).times(1))
                .onNext(fromPbj(PublishStreamResponse.newBuilder().status(endOfStream).build()));

        verify(serviceStatus, timeout(testTimeout).times(1)).stopWebServer();
    }

    //    private static class TestProducerBlockItemObserver extends ProducerBlockItemObserver {
    //        public TestProducerBlockItemObserver(
    //                final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>>
    //                        streamMediator,
    //                final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
    //                        publishStreamResponseObserver,
    //                final BlockNodeContext blockNodeContext,
    //                final ServiceStatus serviceStatus) {
    //            super(streamMediator, publishStreamResponseObserver, blockNodeContext,
    // serviceStatus);
    //        }
    //    }

    @NonNull
    private static Acknowledgement buildAck(@NonNull final BlockItem blockItem)
            throws NoSuchAlgorithmException {
        ItemAcknowledgement itemAck =
                ItemAcknowledgement.newBuilder()
                        .itemHash(Bytes.wrap(getFakeHash(blockItem)))
                        .build();

        return Acknowledgement.newBuilder().itemAck(itemAck).build();
    }
}
