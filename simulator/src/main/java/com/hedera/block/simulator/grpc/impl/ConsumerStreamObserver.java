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

package com.hedera.block.simulator.grpc.impl;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.hapi.block.protoc.SubscribeStreamResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;

public class ConsumerStreamObserver implements StreamObserver<SubscribeStreamResponse> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final CountDownLatch streamLatch;

    public ConsumerStreamObserver(CountDownLatch streamLatch) {
        this.streamLatch = streamLatch;
    }

    @Override
    public void onNext(SubscribeStreamResponse subscribeStreamResponse) {
        LOGGER.log(INFO, "Received Response: " + subscribeStreamResponse.toString());
        // WIP,
        // metricService.get(LiveBlocksConsumed).increment() when a whole block is received.
        // we still need to do something with those blocks
    }

    @Override
    public void onError(Throwable streamError) {
        Status status = Status.fromThrowable(streamError);
        LOGGER.log(ERROR, "Stream error: {0}, status: {1}.", streamError, status);
        streamLatch.countDown();
    }

    @Override
    public void onCompleted() {
        LOGGER.log(INFO, "Subscribe request completed.");
        streamLatch.countDown();
    }
}
