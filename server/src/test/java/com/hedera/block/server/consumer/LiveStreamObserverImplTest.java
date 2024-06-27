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
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LiveStreamObserverImplTest {

    private final long TIMEOUT_THRESHOLD_MILLIS = 50L;
    private final long TEST_TIME = 1719427664950L;

    @Mock
    private StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator;

    @Mock
    private StreamObserver<BlockStreamServiceGrpcProto.Block> responseStreamObserver;


    @Test
    public void testConsumerTimeoutWithinWindow() {
        final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver = new LiveStreamObserverImpl(
                TIMEOUT_THRESHOLD_MILLIS,
                buildClockInsideWindow(TEST_TIME, TIMEOUT_THRESHOLD_MILLIS),
                buildClockInsideWindow(TEST_TIME, TIMEOUT_THRESHOLD_MILLIS),
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
                TIMEOUT_THRESHOLD_MILLIS,
                buildClockOutsideWindow(TEST_TIME, TIMEOUT_THRESHOLD_MILLIS),
                buildClockOutsideWindow(TEST_TIME, TIMEOUT_THRESHOLD_MILLIS),
                streamMediator,
                responseStreamObserver);

        final BlockStreamServiceGrpcProto.Block newBlock = BlockStreamServiceGrpcProto.Block.newBuilder().build();
        when(streamMediator.isSubscribed(liveStreamObserver)).thenReturn(true);
        liveStreamObserver.notify(newBlock);
        verify(streamMediator).unsubscribe(liveStreamObserver);
    }

    @Test
    public void testProducerTimeoutWithinWindow() {
        final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver = new LiveStreamObserverImpl(
                TIMEOUT_THRESHOLD_MILLIS,
                buildClockInsideWindow(TEST_TIME, TIMEOUT_THRESHOLD_MILLIS),
                buildClockInsideWindow(TEST_TIME, TIMEOUT_THRESHOLD_MILLIS),
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
                TIMEOUT_THRESHOLD_MILLIS,
                buildClockOutsideWindow(TEST_TIME, TIMEOUT_THRESHOLD_MILLIS),
                buildClockOutsideWindow(TEST_TIME, TIMEOUT_THRESHOLD_MILLIS),
                streamMediator,
                responseStreamObserver);

        Thread.sleep(51);
        BlockStreamServiceGrpcProto.BlockResponse blockResponse = BlockStreamServiceGrpcProto.BlockResponse.newBuilder().build();
        liveStreamObserver.onNext(blockResponse);

        verify(streamMediator).unsubscribe(liveStreamObserver);
    }

    private static InstantSource buildClockInsideWindow(long testTime, long timeoutThresholdMillis) {
        return new TestClock(testTime, testTime + timeoutThresholdMillis - 1);
    }

    private static InstantSource buildClockOutsideWindow(long testTime, long timeoutThresholdMillis) {
        return new TestClock(testTime, testTime + timeoutThresholdMillis + 1);
    }

    static class TestClock implements InstantSource {

        private int index;
        private final Long[] millis;

        TestClock(Long... millis) {
            this.millis = millis;
        }

        @Override
        public long millis() {
            long value = millis[index];

            // cycle through the provided millis
            // and wrap around if necessary
            index = index > millis.length - 1 ? 0 : index + 1;
            return value;
        }

        @Override
        public Instant instant() {
            return null;
        }
    }
}
