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

import java.time.Clock;
import java.time.InstantSource;

/**
 * The LiveStreamObserverImpl class implements the LiveStreamObserver interface to pass blocks to the downstream consumer
 * via the notify method and manage the bidirectional stream to the consumer via the onNext, onError, and onCompleted methods.
 */
public class LiveStreamObserverImpl implements LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> mediator;
    private final StreamObserver<BlockStreamServiceGrpcProto.Block> responseStreamObserver;

    private final long timeoutThresholdMillis;

    private final InstantSource producerLivenessClock;
    private long producerLivenessMillis;

    private final InstantSource consumerLivenessClock;
    private long consumerLivenessMillis;

    /**
     * Constructor for the LiveStreamObserverImpl class.
     *
     * @param mediator the mediator
     * @param responseStreamObserver the response stream observer
     */
    public LiveStreamObserverImpl(
            final long timeoutThresholdMillis,
            final InstantSource producerLivenessClock,
            final InstantSource consumerLivenessClock,
            final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> mediator,
            final StreamObserver<BlockStreamServiceGrpcProto.Block> responseStreamObserver) {

        this.timeoutThresholdMillis = timeoutThresholdMillis;
        this.producerLivenessClock = producerLivenessClock;
        this.consumerLivenessClock = consumerLivenessClock;
        this.mediator = mediator;
        this.responseStreamObserver = responseStreamObserver;

        this.producerLivenessMillis = producerLivenessClock.millis();
        this.consumerLivenessMillis = consumerLivenessClock.millis();
    }

    /**
     * Pass the block to the observer provided by Helidon
     *
     * @param block the block to be passed to the observer
     */
    @Override
    public void notify(final BlockStreamServiceGrpcProto.Block block) {

        // Check if the consumer has timed out.  If so, unsubscribe the observer from the mediator.
        if (consumerLivenessClock.millis() - consumerLivenessMillis > timeoutThresholdMillis) {
            if (mediator.isSubscribed(this)) {
                LOGGER.log(System.Logger.Level.DEBUG, "Consumer timeout threshold exceeded.  Unsubscribing observer.");
                mediator.unsubscribe(this);
            }
        } else {
            // Refresh the producer liveness and pass the block to the observer.
            producerLivenessMillis = producerLivenessClock.millis();
            responseStreamObserver.onNext(block);
        }
    }

    /**
     * The onNext() method is triggered by Helidon when a consumer sends a blockResponse via the bidirectional stream.
     *
     * @param blockResponse the BlockResponse passed back to the server via the bidirectional stream to the downstream consumer.
     */
    @Override
    public void onNext(final BlockStreamServiceGrpcProto.BlockResponse blockResponse) {

        if (producerLivenessClock.millis() - producerLivenessMillis > timeoutThresholdMillis) {
            LOGGER.log(System.Logger.Level.DEBUG, "Producer timeout threshold exceeded.  Unsubscribing observer.");
            mediator.unsubscribe(this);
        } else {
            LOGGER.log(System.Logger.Level.DEBUG, "Received response block " + blockResponse);
            consumerLivenessMillis = consumerLivenessClock.millis();
        }
    }

    /**
     * The onError() method is triggered by Helidon when an error occurs on the bidirectional stream to the downstream consumer.
     * Unsubscribe the observer from the mediator.
     *
     * @param t the error occurred on the stream
     */
    @Override
    public void onError(final Throwable t) {
        LOGGER.log(System.Logger.Level.ERROR, t);
        mediator.unsubscribe(this);
    }

    /**
     * The onCompleted() method is triggered by Helidon when the bidirectional stream to the downstream consumer is completed.
     * This implementation will then unsubscribe the observer from the mediator.
     */
    @Override
    public void onCompleted() {
        LOGGER.log(System.Logger.Level.DEBUG, "gRPC connection completed.  Unsubscribing observer.");
        mediator.unsubscribe(this);
        LOGGER.log(System.Logger.Level.DEBUG, "Unsubscribed observer.");
    }
}
