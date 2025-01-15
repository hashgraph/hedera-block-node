// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.grpc.impl;

import static com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter.LiveBlocksProcessed;
import static java.lang.System.Logger.Level.ERROR;
import static java.util.Objects.requireNonNull;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.block.simulator.grpc.PublishStreamGrpcServer;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.hapi.block.protoc.BlockStreamServiceGrpc;
import com.hedera.hapi.block.protoc.PublishStreamRequest;
import com.hedera.hapi.block.protoc.PublishStreamResponse;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.inject.Inject;

public class PublishStreamGrpcServerImpl implements PublishStreamGrpcServer {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // gRPC Components
    private Server server;
    private PublishStreamServerObserver publishStreamServerObserver;

    // Configuration
    private final BlockStreamConfig blockStreamConfig;
    private final GrpcConfig grpcConfig;

    // Service dependencies
    private final MetricsService metricsService;

    // State
    private final int lastKnownStatusesCapacity;
    private final Deque<String> lastKnownStatuses;

    @Inject
    public PublishStreamGrpcServerImpl(
            @NonNull final GrpcConfig grpcConfig,
            @NonNull final BlockStreamConfig blockStreamConfig,
            @NonNull final MetricsService metricsService) {
        this.grpcConfig = requireNonNull(grpcConfig);
        this.metricsService = requireNonNull(metricsService);
        this.blockStreamConfig = requireNonNull(blockStreamConfig);

        this.lastKnownStatusesCapacity = blockStreamConfig.lastKnownStatusesCapacity();
        lastKnownStatuses = new ArrayDeque<>(this.lastKnownStatusesCapacity);
    }

    @Override
    public void init() {
        server = ServerBuilder.forPort(grpcConfig.port())
                .addService(new BlockStreamServiceGrpc.BlockStreamServiceImplBase() {
                    @Override
                    public StreamObserver<PublishStreamRequest> publishBlockStream(
                            StreamObserver<PublishStreamResponse> responseObserver) {
                        publishStreamServerObserver = new PublishStreamServerObserver(responseObserver);
                        return publishStreamServerObserver;
                    }
                })
                .build();
        try {
            server.start();
        } catch (IOException e) {
            LOGGER.log(ERROR, e);
        }
    }

    /**
     * Gets the number of processed blocks.
     *
     * @return the number of published blocks
     */
    @Override
    public long getProcessedBlocks() {
        return metricsService.get(LiveBlocksProcessed).get();
    }

    /**
     * Gets the last known statuses.
     *
     * @return the last known statuses
     */
    @Override
    public List<String> getLastKnownStatuses() {
        return List.copyOf(lastKnownStatuses);
    }

    /**
     * Sends a onCompleted message to the client and waits for a short period of
     * time to ensure the message is sent.
     */
    @Override
    public void completeStreaming() {
        publishStreamServerObserver.onCompleted();
    }

    /**
     * Shutdowns the channel.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public void shutdown() throws InterruptedException {
        completeStreaming();
        server.shutdown();
    }
}
