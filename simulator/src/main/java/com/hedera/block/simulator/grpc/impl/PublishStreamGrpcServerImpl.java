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

/**
 * Implementation of {@link PublishStreamGrpcServer} that handles incoming block stream publications
 * via gRPC streaming. This implementation manages the server setup, handles incoming block streams,
 * tracks processed blocks, and maintains a history of stream statuses. It provides functionality
 * to start, monitor, and shutdown the gRPC server.
 */
public class PublishStreamGrpcServerImpl implements PublishStreamGrpcServer {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // gRPC Components
    private Server server;
    private PublishStreamServerObserver publishStreamServerObserver;

    // Configuration
    private final GrpcConfig grpcConfig;

    // Service dependencies
    private final MetricsService metricsService;

    // State
    private final int lastKnownStatusesCapacity;
    private final Deque<String> lastKnownStatuses;

    /**
     * Constructs a new PublishStreamGrpcServerImpl.
     *
     * @param grpcConfig Configuration for the gRPC server settings
     * @param blockStreamConfig Configuration for the block stream settings
     * @param metricsService Service for tracking metrics
     * @throws NullPointerException if any of the parameters are null
     */
    @Inject
    public PublishStreamGrpcServerImpl(
            @NonNull final GrpcConfig grpcConfig,
            @NonNull final BlockStreamConfig blockStreamConfig,
            @NonNull final MetricsService metricsService) {
        this.grpcConfig = requireNonNull(grpcConfig);
        this.metricsService = requireNonNull(metricsService);

        this.lastKnownStatusesCapacity = blockStreamConfig.lastKnownStatusesCapacity();
        lastKnownStatuses = new ArrayDeque<>(this.lastKnownStatusesCapacity);
    }

    /**
     * Initialize, opens a gRPC channel and creates the needed services with the passed configuration.
     */
    @Override
    public void init() {
        server = ServerBuilder.forPort(grpcConfig.port())
                .addService(new BlockStreamServiceGrpc.BlockStreamServiceImplBase() {
                    @Override
                    public StreamObserver<PublishStreamRequest> publishBlockStream(
                            StreamObserver<PublishStreamResponse> responseObserver) {
                        publishStreamServerObserver = new PublishStreamServerObserver(
                                responseObserver, lastKnownStatuses, lastKnownStatusesCapacity);
                        return publishStreamServerObserver;
                    }
                })
                .build();
    }

    /**
     * Starts the gRPC server.
     */
    @Override
    public void start() {
        try {
            server.start();
        } catch (IOException e) {
            LOGGER.log(ERROR, "Something went wrong, while trying to start the gRPC server. Error: %s".formatted(e));
        }
    }

    /**
     * Gets the number of processed blocks.
     *
     * @return the number of processed blocks
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
     * Sends a onCompleted message to the server.
     */
    @Override
    public void completeStreaming() {
        publishStreamServerObserver.onCompleted();
    }

    /**
     * Shutdowns the server.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public void shutdown() throws InterruptedException {
        completeStreaming();
        server.shutdown();
    }
}
