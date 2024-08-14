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

import static com.hedera.block.protos.BlockStreamService.*;
import static com.hedera.block.protos.BlockStreamService.PublishStreamResponse.ItemAcknowledgement;
import static com.hedera.block.server.producer.Util.getFakeHash;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.google.protobuf.ByteString;
import com.hedera.block.protos.BlockStreamService;
import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.ServiceStatusImpl;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.config.BlockNodeContextFactory;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.consumer.ConsumerStreamResponseObserver;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.LiveStreamMediatorBuilder;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.InstantSource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProducerBlockItemObserverTest {

    @Mock private ItemAckBuilder itemAckBuilder;
    @Mock private StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> streamMediator;
    @Mock private StreamObserver<PublishStreamResponse> publishStreamResponseObserver;

    @Mock private BlockWriter<BlockItem> blockWriter;

    @Mock private StreamObserver<SubscribeStreamResponse> streamObserver1;
    @Mock private StreamObserver<SubscribeStreamResponse> streamObserver2;
    @Mock private StreamObserver<SubscribeStreamResponse> streamObserver3;

    @Mock private ServiceStatus serviceStatus;
    @Mock private InstantSource testClock;

    @Test
    public void testProducerOnNext() throws IOException, NoSuchAlgorithmException {

        final List<BlockItem> blockItems = generateBlockItems(1);
        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        streamMediator,
                        publishStreamResponseObserver,
                        new ItemAckBuilder(),
                        serviceStatus);

        when(serviceStatus.isRunning()).thenReturn(true);

        final BlockItem blockHeader = blockItems.getFirst();
        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().setBlockItem(blockHeader).build();
        producerBlockItemObserver.onNext(publishStreamRequest);

        verify(streamMediator, timeout(50).times(1)).publish(blockHeader);

        final ItemAcknowledgement itemAck =
                ItemAcknowledgement.newBuilder()
                        .setItemAck(ByteString.copyFrom(getFakeHash(blockHeader)))
                        .build();
        final BlockStreamService.PublishStreamResponse publishStreamResponse =
                BlockStreamService.PublishStreamResponse.newBuilder()
                        .setAcknowledgement(itemAck)
                        .build();
        verify(publishStreamResponseObserver, timeout(50).times(1)).onNext(publishStreamResponse);

        // Helidon will call onCompleted after onNext
        producerBlockItemObserver.onCompleted();

        verify(publishStreamResponseObserver, timeout(50).times(1)).onCompleted();
    }

    @Test
    public void testProducerWithManyConsumers() throws IOException {

        final BlockNodeContext blockNodeContext = BlockNodeContextFactory.create();
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(
                                blockWriter, blockNodeContext, new ServiceStatusImpl())
                        .build();

        // Mock a clock with 2 different return values in response to anticipated
        // millis() calls. Here the second call will always be inside the timeout window.
        ConsumerConfig consumerConfig = new ConsumerConfig(100);
        long TEST_TIME = 1_719_427_664_950L;
        when(testClock.millis())
                .thenReturn(TEST_TIME, TEST_TIME + consumerConfig.timeoutThresholdMillis());

        final var concreteObserver1 =
                new ConsumerStreamResponseObserver(
                        consumerConfig, testClock, streamMediator, streamObserver1);

        final var concreteObserver2 =
                new ConsumerStreamResponseObserver(
                        consumerConfig, testClock, streamMediator, streamObserver2);

        final var concreteObserver3 =
                new ConsumerStreamResponseObserver(
                        consumerConfig, testClock, streamMediator, streamObserver3);

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

        final BlockHeader blockHeader = BlockHeader.newBuilder().setBlockNumber(1).build();
        final BlockItem blockItem = BlockItem.newBuilder().setHeader(blockHeader).build();
        final SubscribeStreamResponse subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().setBlockItem(blockItem).build();

        when(serviceStatus.isRunning()).thenReturn(true);

        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        streamMediator,
                        publishStreamResponseObserver,
                        new ItemAckBuilder(),
                        serviceStatus);

        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().setBlockItem(blockItem).build();
        producerBlockItemObserver.onNext(publishStreamRequest);

        // Confirm the block item counter was incremented
        assertEquals(1, blockNodeContext.metricsService().liveBlockItems.get());

        // Confirm each subscriber was notified of the new block
        verify(streamObserver1, timeout(50).times(1)).onNext(subscribeStreamResponse);
        verify(streamObserver2, timeout(50).times(1)).onNext(subscribeStreamResponse);
        verify(streamObserver3, timeout(50).times(1)).onNext(subscribeStreamResponse);

        // Confirm the BlockStorage write method was
        // called despite the absence of subscribers
        verify(blockWriter).write(blockItem);
    }

    @Test
    public void testOnError() {
        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        streamMediator,
                        publishStreamResponseObserver,
                        new ItemAckBuilder(),
                        serviceStatus);

        final Throwable t = new Throwable("Test error");
        producerBlockItemObserver.onError(t);
        verify(publishStreamResponseObserver).onError(t);
    }

    @Test
    public void testItemAckBuilderExceptionTest() throws IOException, NoSuchAlgorithmException {

        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        streamMediator,
                        publishStreamResponseObserver,
                        itemAckBuilder,
                        serviceStatus);

        when(serviceStatus.isRunning()).thenReturn(true);
        when(itemAckBuilder.buildAck(any()))
                .thenThrow(new NoSuchAlgorithmException("Test exception"));

        final List<BlockItem> blockItems = generateBlockItems(1);
        final BlockItem blockHeader = blockItems.getFirst();
        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().setBlockItem(blockHeader).build();
        producerBlockItemObserver.onNext(publishStreamRequest);

        final PublishStreamResponse.EndOfStream endOfStream =
                PublishStreamResponse.EndOfStream.newBuilder()
                        .setStatus(
                                PublishStreamResponse.PublishStreamResponseCode
                                        .STREAM_ITEMS_UNKNOWN)
                        .build();
        final PublishStreamResponse errorResponse =
                PublishStreamResponse.newBuilder().setStatus(endOfStream).build();
        verify(publishStreamResponseObserver, timeout(50).times(1)).onNext(errorResponse);
    }
}
