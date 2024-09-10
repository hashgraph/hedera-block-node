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
import static com.hedera.block.server.producer.Util.getFakeHash;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static com.hedera.block.server.util.PersistTestUtils.reverseByteArray;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.ServiceStatusImpl;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.Publisher;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.ItemAcknowledgement;
import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProducerBlockItemObserverTest {

    @Mock private InstantSource testClock;
    @Mock private Publisher<BlockItem> publisher;
    @Mock private SubscriptionHandler<PublishStreamResponse> subscriptionHandler;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
            publishStreamResponseObserver;

    @Mock
    private ServerCallStreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
            serverCallStreamObserver;

    @Mock private ServiceStatus serviceStatus;
    @Mock private ObjectEvent<PublishStreamResponse> objectEvent;

    private final long TIMEOUT_THRESHOLD_MILLIS = 50L;
    private static final int testTimeout = 1000;

    BlockNodeContext testContext;

    @BeforeEach
    public void setUp() throws IOException {
        this.testContext =
                TestConfigUtil.getTestBlockNodeContext(
                        Map.of(
                                TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY,
                                String.valueOf(TIMEOUT_THRESHOLD_MILLIS)));
    }

    @Test
    public void testOnError() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        publishStreamResponseObserver,
                        blockNodeContext,
                        serviceStatus);

        final Throwable t = new Throwable("Test error");
        producerBlockItemObserver.onError(t);
        verify(publishStreamResponseObserver).onError(t);
    }

    @Test
    public void testBlockItemThrowsParseException() throws IOException {

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
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

    @Test
    public void testResponseNotPermittedAfterCancel() throws NoSuchAlgorithmException {

        final TestProducerBlockItemObserver producerStreamResponseObserver =
                new TestProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        serverCallStreamObserver,
                        testContext,
                        serviceStatus);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final ItemAcknowledgement itemAck =
                ItemAcknowledgement.newBuilder()
                        .itemHash(Bytes.wrap(getFakeHash(blockItems.getLast())))
                        .build();
        final PublishStreamResponse publishStreamResponse =
                PublishStreamResponse.newBuilder()
                        .acknowledgement(Acknowledgement.newBuilder().itemAck(itemAck).build())
                        .build();
        when(objectEvent.get()).thenReturn(publishStreamResponse);

        // Confirm that the observer is called with the first BlockItem
        producerStreamResponseObserver.onEvent(objectEvent, 0, true);

        // Cancel the observer
        producerStreamResponseObserver.cancel();

        // Attempt to send another BlockItem
        producerStreamResponseObserver.onEvent(objectEvent, 0, true);

        // Confirm that canceling the observer allowed only 1 response to be sent.
        verify(serverCallStreamObserver, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));
    }

    @Test
    public void testResponseNotPermittedAfterClose() throws NoSuchAlgorithmException {

        final TestProducerBlockItemObserver producerBlockItemObserver =
                new TestProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        serverCallStreamObserver,
                        testContext,
                        serviceStatus);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final ItemAcknowledgement itemAck =
                ItemAcknowledgement.newBuilder()
                        .itemHash(Bytes.wrap(getFakeHash(blockItems.getLast())))
                        .build();
        final PublishStreamResponse publishStreamResponse =
                PublishStreamResponse.newBuilder()
                        .acknowledgement(Acknowledgement.newBuilder().itemAck(itemAck).build())
                        .build();
        when(objectEvent.get()).thenReturn(publishStreamResponse);

        // Confirm that the observer is called with the first BlockItem
        producerBlockItemObserver.onEvent(objectEvent, 0, true);

        // Cancel the observer
        producerBlockItemObserver.close();

        // Attempt to send another BlockItem
        producerBlockItemObserver.onEvent(objectEvent, 0, true);

        // Confirm that closing the observer allowed only 1 response to be sent.
        verify(serverCallStreamObserver, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));
    }

    @Test
    public void testOnlyErrorStreamResponseAllowedAfterStatusChange()
            throws NoSuchAlgorithmException {

        final ServiceStatus serviceStatus = new ServiceStatusImpl();

        final ProducerBlockItemObserver producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        serverCallStreamObserver,
                        testContext,
                        serviceStatus);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().blockItem(blockItems.getFirst()).build();

        // Confirm that the observer is called with the first BlockItem
        producerBlockItemObserver.onNext(fromPbj(publishStreamRequest));

        // Change the status of the service
        serviceStatus.stopRunning(getClass().getName());

        // Confirm that the observer is called with the first BlockItem
        producerBlockItemObserver.onNext(fromPbj(publishStreamRequest));

        // Confirm that closing the observer allowed only 1 response to be sent.
        verify(serverCallStreamObserver, timeout(testTimeout).times(1)).onNext(any());
    }

    private static class TestProducerBlockItemObserver extends ProducerBlockItemObserver {
        public TestProducerBlockItemObserver(
                @NonNull final InstantSource clock,
                @NonNull final Publisher<BlockItem> publisher,
                @NonNull final SubscriptionHandler<PublishStreamResponse> subscriptionHandler,
                @NonNull
                        final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
                                publishStreamResponseObserver,
                @NonNull final BlockNodeContext blockNodeContext,
                @NonNull final ServiceStatus serviceStatus) {
            super(
                    clock,
                    publisher,
                    subscriptionHandler,
                    publishStreamResponseObserver,
                    blockNodeContext,
                    serviceStatus);
        }

        public void cancel() {
            onCancel.run();
        }

        public void close() {
            onClose.run();
        }
    }
}
