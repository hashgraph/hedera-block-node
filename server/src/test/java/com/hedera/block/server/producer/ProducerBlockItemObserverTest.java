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

import static com.hedera.block.server.Translator.*;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static com.hedera.block.server.util.TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.*;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.output.BlockHeader;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProducerBlockItemObserverTest {

    @Mock private AckBuilder ackBuilder;
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

    @Test
    public void testProducerOnNext() throws IOException, NoSuchAlgorithmException {

        final List<BlockItem> blockItems = generateBlockItems(1);
        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        streamMediator,
                        publishStreamResponseObserver,
                        new AckBuilder(),
                        serviceStatus);

        when(serviceStatus.isRunning()).thenReturn(true);

        final BlockItem blockHeader = blockItems.getFirst();
        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().blockItem(blockHeader).build();
        producerBlockItemObserver.onNext(toProtocPublishStreamRequest(publishStreamRequest));

        verify(streamMediator, timeout(50).times(1)).publish(blockHeader);

        final Acknowledgement ack = new AckBuilder().buildAck(blockHeader);
        final PublishStreamResponse publishStreamResponse =
                PublishStreamResponse.newBuilder().acknowledgement(ack).build();

        verify(publishStreamResponseObserver, timeout(50).times(1))
                .onNext(toProtocPublishStreamResponse(publishStreamResponse));

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

        when(serviceStatus.isRunning()).thenReturn(true);

        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        streamMediator,
                        publishStreamResponseObserver,
                        new AckBuilder(),
                        serviceStatus);

        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().blockItem(blockItem).build();
        producerBlockItemObserver.onNext(toProtocPublishStreamRequest(publishStreamRequest));

        // Confirm the block item counter was incremented
        assertEquals(1, blockNodeContext.metricsService().liveBlockItems.get());

        // Confirm each subscriber was notified of the new block
        verify(streamObserver1, timeout(50).times(1))
                .onNext(toProtocSubscribeStreamResponse(subscribeStreamResponse));
        verify(streamObserver2, timeout(50).times(1))
                .onNext(toProtocSubscribeStreamResponse(subscribeStreamResponse));
        verify(streamObserver3, timeout(50).times(1))
                .onNext(toProtocSubscribeStreamResponse(subscribeStreamResponse));

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
                        new AckBuilder(),
                        serviceStatus);

        final Throwable t = new Throwable("Test error");
        producerBlockItemObserver.onError(t);
        verify(publishStreamResponseObserver).onError(t);
    }

    @Test
    public void testItemAckBuilderExceptionTest() throws IOException, NoSuchAlgorithmException {

        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        streamMediator, publishStreamResponseObserver, ackBuilder, serviceStatus);

        when(serviceStatus.isRunning()).thenReturn(true);
        when(ackBuilder.buildAck(any())).thenThrow(new NoSuchAlgorithmException("Test exception"));

        final List<BlockItem> blockItems = generateBlockItems(1);
        final BlockItem blockHeader = blockItems.getFirst();
        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().blockItem(blockHeader).build();
        producerBlockItemObserver.onNext(toProtocPublishStreamRequest(publishStreamRequest));

        final EndOfStream endOfStream =
                EndOfStream.newBuilder()
                        .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                        .build();
        final PublishStreamResponse errorResponse =
                PublishStreamResponse.newBuilder().status(endOfStream).build();
        verify(publishStreamResponseObserver, timeout(50).times(1))
                .onNext(toProtocPublishStreamResponse(errorResponse));
    }
}
