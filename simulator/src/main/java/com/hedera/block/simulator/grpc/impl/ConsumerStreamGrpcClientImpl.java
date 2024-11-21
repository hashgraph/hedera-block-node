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

import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.block.simulator.grpc.ConsumerStreamGrpcClient;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.hapi.block.protoc.BlockStreamServiceGrpc;
import com.hedera.hapi.block.protoc.SubscribeStreamRequest;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.inject.Inject;

public class ConsumerStreamGrpcClientImpl implements ConsumerStreamGrpcClient {
    private final GrpcConfig grpcConfig;
    private ManagedChannel channel;
    private BlockStreamServiceGrpc.BlockStreamServiceStub stub;
    private final MetricsService metricsService;
    private final List<String> lastKnownStatuses = new ArrayList<>();

    @Inject
    public ConsumerStreamGrpcClientImpl(
            @NonNull final GrpcConfig grpcConfig, @NonNull final MetricsService metricsService) {
        this.grpcConfig = requireNonNull(grpcConfig);
        this.metricsService = requireNonNull(metricsService);
    }

    public void init() {
        channel = ManagedChannelBuilder.forAddress(grpcConfig.serverAddress(), grpcConfig.port())
                .usePlaintext()
                .build();
        stub = BlockStreamServiceGrpc.newStub(channel);
        lastKnownStatuses.clear();
    }

    public void requestBlocks(long startBlock, long endBlock) throws InterruptedException {
        CountDownLatch streamLatch = new CountDownLatch(1);
        SubscribeStreamRequest request = SubscribeStreamRequest.newBuilder()
                .setStartBlockNumber(startBlock)
                .setEndBlockNumber(endBlock)
                .setAllowUnverified(true)
                .build();
        stub.subscribeBlockStream(request, new ConsumerStreamObserver(streamLatch));

        streamLatch.await();
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
