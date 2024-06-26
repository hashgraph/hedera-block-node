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

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.mediator.StreamMediator;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LiveStreamObserverImplTest {

    @Mock
    private StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator;

    @Mock
    private StreamObserver<BlockStreamServiceGrpcProto.Block> responseStreamObserver;


    @Test
    public void testConsumerTimeoutWithinWindow() {
        final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver = new LiveStreamObserverImpl(
                50,
                Clock.systemDefaultZone(),
                Clock.systemDefaultZone(),
                streamMediator,
                responseStreamObserver);
        BlockStreamServiceGrpcProto.Block newBlock = BlockStreamServiceGrpcProto.Block.newBuilder().build();
        liveStreamObserver.notify(newBlock);

        // verify the observer is called with the next
        // block and the stream mediator is not unsubscribed
        verify(responseStreamObserver).onNext(newBlock);
        verify(streamMediator, never()).unsubscribe(liveStreamObserver);
    }

    @Test
    public void testConsumerTimeoutOutsideWindow() throws InterruptedException {
        final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver = new LiveStreamObserverImpl(
                50,
                Clock.systemDefaultZone(),
                Clock.systemDefaultZone(),
                streamMediator,
                responseStreamObserver);

        Thread.sleep(51);
        BlockStreamServiceGrpcProto.Block newBlock = BlockStreamServiceGrpcProto.Block.newBuilder().build();
        when(streamMediator.isSubscribed(liveStreamObserver)).thenReturn(true);
        liveStreamObserver.notify(newBlock);
        verify(streamMediator).unsubscribe(liveStreamObserver);
    }

    @Test
    public void testProducerTimeoutWithinWindow() {
        final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver = new LiveStreamObserverImpl(
                50,
                Clock.systemDefaultZone(),
                Clock.systemDefaultZone(),
                streamMediator,
                responseStreamObserver);

        BlockStreamServiceGrpcProto.BlockResponse blockResponse = BlockStreamServiceGrpcProto.BlockResponse.newBuilder().build();
        liveStreamObserver.onNext(blockResponse);

        // verify the mediator is NOT called to unsubscribe the observer
        verify(streamMediator, never()).unsubscribe(liveStreamObserver);
    }

    @Test
    public void testProducerTimeoutOutsideWindow() throws InterruptedException {
        final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver = new LiveStreamObserverImpl(
                50,
                Clock.systemDefaultZone(),
                Clock.systemDefaultZone(),
                streamMediator,
                responseStreamObserver);

        Thread.sleep(51);
        BlockStreamServiceGrpcProto.BlockResponse blockResponse = BlockStreamServiceGrpcProto.BlockResponse.newBuilder().build();
        liveStreamObserver.onNext(blockResponse);

        verify(streamMediator).unsubscribe(liveStreamObserver);
    }
}
