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

import static com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter.LiveBlocksConsumed;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.hapi.block.protoc.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Implementation of StreamObserver that handles responses from the block stream subscription.
 * This class processes incoming blocks and status messages, updating metrics accordingly.
 */
public class ConsumerStreamObserver implements StreamObserver<SubscribeStreamResponse> {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // Service dependencies
    private final MetricsService metricsService;

    // State
    private final CountDownLatch streamLatch;
    private final int lastKnownStatusesCapacity;
    private final Deque<String> lastKnownStatuses;

    /**
     * Constructs a new ConsumerStreamObserver.
     *
     * @param metricsService The service for recording consumption metrics
     * @param streamLatch A latch used to coordinate stream completion
     * @param lastKnownStatuses List to store the most recent status messages
     * @param lastKnownStatusesCapacity the capacity of the last known statuses
     * @throws NullPointerException if any parameter is null
     */
    public ConsumerStreamObserver(
            @NonNull final MetricsService metricsService,
            @NonNull final CountDownLatch streamLatch,
            @NonNull final Deque<String> lastKnownStatuses,
            final int lastKnownStatusesCapacity) {
        this.metricsService = requireNonNull(metricsService);
        this.streamLatch = requireNonNull(streamLatch);
        this.lastKnownStatuses = requireNonNull(lastKnownStatuses);
        this.lastKnownStatusesCapacity = lastKnownStatusesCapacity;
    }

    /**
     * Processes incoming stream responses, handling both status messages and block items.
     *
     * @param subscribeStreamResponse The response received from the server
     * @throws IllegalArgumentException if an unknown response type is received
     */
    @Override
    public void onNext(SubscribeStreamResponse subscribeStreamResponse) {
        final SubscribeStreamResponse.ResponseCase responseType = subscribeStreamResponse.getResponseCase();
        if (lastKnownStatuses.size() == lastKnownStatusesCapacity) {
            lastKnownStatuses.pollFirst();
        }
        lastKnownStatuses.add(subscribeStreamResponse.toString());

        switch (responseType) {
            case STATUS -> LOGGER.log(INFO, "Received Response: " + subscribeStreamResponse);
            case BLOCK_ITEMS -> processBlockItems(
                    subscribeStreamResponse.getBlockItems().getBlockItemsList());
            default -> throw new IllegalArgumentException("Unknown response type: " + responseType);
        }
    }

    /**
     * Handles stream errors by logging the error and releasing the stream latch.
     *
     * @param streamError The error that occurred during streaming
     */
    @Override
    public void onError(Throwable streamError) {
        Status status = Status.fromThrowable(streamError);
        lastKnownStatuses.add(status.toString());
        LOGGER.log(ERROR, "Error %s with status %s.".formatted(streamError, status), streamError);
        streamLatch.countDown();
    }

    /**
     * Handles stream completion by logging the event and releasing the stream latch.
     */
    @Override
    public void onCompleted() {
        LOGGER.log(INFO, "Subscribe request completed.");
        streamLatch.countDown();
    }

    private void processBlockItems(List<BlockItem> blockItems) {
        blockItems.stream().filter(BlockItem::hasBlockProof).forEach(blockItem -> {
            metricsService.get(LiveBlocksConsumed).increment();
            LOGGER.log(
                    INFO, "Received block number: " + blockItem.getBlockProof().getBlock());
        });
    }
}
