/*
 * Hedera Block Node
 *
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

import java.util.logging.Logger;

/**
 * The LiveStreamObserverImpl class implements the LiveStreamObserver interface to pass blocks to the downstream consumer
 * via the notify method and manage the bidirectional stream to the consumer via the onNext, onError, and onCompleted methods.
 *
 */
public class LiveStreamObserverImpl implements LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> {

    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    private final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> mediator;
    private final StreamObserver<BlockStreamServiceGrpcProto.Block> responseStreamObserver;

    private long consumerLivenessMillis;
    private long producerLivenessMillis;
    private final long timeoutThresholdMillis;

    /**
     * Constructor for the LiveStreamObserverImpl class.
     *
     * @param mediator the mediator
     * @param responseStreamObserver the response stream observer
     *
     */
    public LiveStreamObserverImpl(long timeoutThresholdMillis,
            StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> mediator,
            StreamObserver<BlockStreamServiceGrpcProto.Block> responseStreamObserver) {

        this.mediator = mediator;
        this.responseStreamObserver = responseStreamObserver;

        this.timeoutThresholdMillis = timeoutThresholdMillis;
        this.consumerLivenessMillis = System.currentTimeMillis();
        this.producerLivenessMillis = System.currentTimeMillis();
    }

    /**
     * Pass the block to the observer provided by Helidon
     *
     * @param block - the block to be passed to the observer
     */
    @Override
    public void notify(BlockStreamServiceGrpcProto.Block block) {

        if (System.currentTimeMillis() - this.consumerLivenessMillis > timeoutThresholdMillis) {
            if (mediator.isSubscribed(this)) {
                LOGGER.info("Consumer timeout threshold exceeded.  Unsubscribing observer.");
                mediator.unsubscribe(this);
            }
        } else {
            this.producerLivenessMillis = System.currentTimeMillis();
            this.responseStreamObserver.onNext(block);
        }
    }

    /**
     * The onNext() method is triggered by Helidon when the consumer sends a blockResponse via the bidirectional stream.
     *
     * @param blockResponse - the BlockResponse passed to the server via the bidirectional stream to the downstream consumer
     */
    @Override
    public void onNext(BlockStreamServiceGrpcProto.BlockResponse blockResponse) {

        if (System.currentTimeMillis() - this.producerLivenessMillis > timeoutThresholdMillis) {
            if (mediator.isSubscribed(this)) {
                LOGGER.info("Producer timeout threshold exceeded.  Unsubscribing observer.");
                mediator.unsubscribe(this);
            }
        } else {
            LOGGER.finer("Received response block " + blockResponse);
            this.consumerLivenessMillis = System.currentTimeMillis();
        }
    }

    /**
     * The onError() method is triggered by Helidon when an error occurs on the bidirectional stream to the downstream consumer.
     * Unsubscribe the observer from the mediator.
     *
     * @param t the error occurred on the stream
     */
    @Override
    public void onError(Throwable t) {
        LOGGER.severe("onError: " + t.getMessage());
        mediator.unsubscribe(this);
    }

    /**
     * The onCompleted() method is triggered by Helidon when the bidirectional stream to the downstream consumer is completed.
     * Unsubscribe the observer from the mediator.
     *
     */
    @Override
    public void onCompleted() {
        LOGGER.finer("gRPC connection completed.  Unsubscribing observer.");
        mediator.unsubscribe(this);
        LOGGER.finer("Unsubscribed observer.");
    }
}
