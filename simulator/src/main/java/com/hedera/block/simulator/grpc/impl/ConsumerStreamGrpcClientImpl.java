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
import static java.util.Objects.requireNonNull;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.block.simulator.grpc.ConsumerStreamGrpcClient;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.hapi.block.protoc.BlockStreamServiceGrpc;
import com.hedera.hapi.block.protoc.SubscribeStreamRequest;
import com.hedera.hapi.block.protoc.SubscribeStreamResponse;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.inject.Inject;

/**
 * Implementation of {@link ConsumerStreamGrpcClient} that handles the consumption of blocks
 * via gRPC streaming. This implementation manages the connection to the server and tracks
 * metrics related to block consumption.
 */
public class ConsumerStreamGrpcClientImpl implements ConsumerStreamGrpcClient {
    // Configuration
    private final GrpcConfig grpcConfig;

    // Service dependencies
    private final MetricsService metricsService;

    // gRPC components
    private ManagedChannel channel;
    private BlockStreamServiceGrpc.BlockStreamServiceStub stub;
    private StreamObserver<SubscribeStreamResponse> consumerStreamObserver;

    // State
    private final List<String> lastKnownStatuses;

    /**
     * Constructs a new ConsumerStreamGrpcClientImpl with the specified configuration and metrics service.
     *
     * @param grpcConfig The configuration for gRPC connection settings
     * @param metricsService The service for recording consumption metrics
     * @throws NullPointerException if any parameter is null
     */
    @Inject
    public ConsumerStreamGrpcClientImpl(
            @NonNull final GrpcConfig grpcConfig, @NonNull final MetricsService metricsService) {
        this.grpcConfig = requireNonNull(grpcConfig);
        this.metricsService = requireNonNull(metricsService);
        this.lastKnownStatuses = new ArrayList<>();
    }

    @Override
    public void init() {
        channel = ManagedChannelBuilder.forAddress(grpcConfig.serverAddress(), grpcConfig.port())
                .usePlaintext()
                .build();
        stub = BlockStreamServiceGrpc.newStub(channel);
        lastKnownStatuses.clear();
    }

    public void requestBlocks(long startBlock, long endBlock) throws InterruptedException {
        Preconditions.requireWhole(startBlock);
        Preconditions.requireWhole(endBlock);

        CountDownLatch streamLatch = new CountDownLatch(1);
        consumerStreamObserver = new ConsumerStreamObserver(metricsService, streamLatch);

        SubscribeStreamRequest request = SubscribeStreamRequest.newBuilder()
                .setStartBlockNumber(startBlock)
                .setEndBlockNumber(endBlock)
                .setAllowUnverified(true)
                .build();
        stub.subscribeBlockStream(request, consumerStreamObserver);

        streamLatch.await();
    }

    public void completeStreaming() throws InterruptedException {
        consumerStreamObserver.onCompleted();
        // todo(352) Find a suitable solution for removing the sleep
        Thread.sleep(100);
    }

    @Override
    public long getConsumedBlocks() {
        return metricsService.get(LiveBlocksConsumed).get();
    }

    @Override
    public List<String> getLastKnownStatuses() {
        return List.copyOf(lastKnownStatuses);
    }

    public void shutdown() {
        channel.shutdown();
    }
}
