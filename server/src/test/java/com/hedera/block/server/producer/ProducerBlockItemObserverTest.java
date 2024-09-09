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
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static com.hedera.block.server.util.PersistTestUtils.reverseByteArray;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.InstantSource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProducerBlockItemObserverTest {

    @Mock private SubscriptionHandler<PublishStreamResponse> subscriptionHandler;

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

    @Test
    public void testBlockItemThrowsParseException() throws IOException {

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
}
