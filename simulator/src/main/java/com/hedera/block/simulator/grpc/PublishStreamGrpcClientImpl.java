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

package com.hedera.block.simulator.grpc;

import static com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter.LiveBlockItemsSent;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.block.common.utils.ChunkUtils;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.hapi.block.protoc.BlockItemSet;
import com.hedera.hapi.block.protoc.BlockStreamServiceGrpc;
import com.hedera.hapi.block.protoc.PublishStreamRequest;
import com.hedera.hapi.block.stream.protoc.Block;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/**
 * The PublishStreamGrpcClientImpl class provides the methods to stream the
 * block and block item.
 */
public class PublishStreamGrpcClientImpl implements PublishStreamGrpcClient {

        private final System.Logger LOGGER = System.getLogger(getClass().getName());

        private StreamObserver<PublishStreamRequest> requestStreamObserver;
        private final BlockStreamConfig blockStreamConfig;
        private final GrpcConfig grpcConfig;
        private final AtomicBoolean streamEnabled;
        private ManagedChannel channel;
        private final MetricsService metricsService;
        private final List<String> lastKnownStatuses;
        private int publishedBlocks;

        /**
         * Creates a new PublishStreamGrpcClientImpl instance.
         *
         * @param grpcConfig        the gRPC configuration
         * @param blockStreamConfig the block stream configuration
         * @param metricsService    the metrics service
         * @param streamEnabled     the flag responsible for enabling and disabling of
         *                          the streaming
         */
        @Inject
        public PublishStreamGrpcClientImpl(
                        @NonNull final GrpcConfig grpcConfig,
                        @NonNull final BlockStreamConfig blockStreamConfig,
                        @NonNull final MetricsService metricsService,
                        @NonNull final AtomicBoolean streamEnabled) {
                this.grpcConfig = requireNonNull(grpcConfig);
                this.blockStreamConfig = requireNonNull(blockStreamConfig);
                this.metricsService = requireNonNull(metricsService);
                this.streamEnabled = requireNonNull(streamEnabled);

                lastKnownStatuses = new ArrayList<>();
                publishedBlocks = 0;
        }

        /**
         * Initialize the channel and stub for publishBlockStream with the desired
         * configuration.
         */
        @Override
        public void init() {
                channel = ManagedChannelBuilder.forAddress(grpcConfig.serverAddress(), grpcConfig.port())
                                .usePlaintext()
                                .build();
                BlockStreamServiceGrpc.BlockStreamServiceStub stub = BlockStreamServiceGrpc.newStub(channel);
                PublishStreamObserver publishStreamObserver = new PublishStreamObserver(streamEnabled,
                                lastKnownStatuses);
                requestStreamObserver = stub.publishBlockStream(publishStreamObserver);
        }

        /**
         * The PublishStreamObserver class implements the StreamObserver interface to
         * observe the
         * stream.
         */
        @Override
        public boolean streamBlockItem(List<BlockItem> blockItems) {

                if (streamEnabled.get()) {
                        requestStreamObserver.onNext(PublishStreamRequest.newBuilder()
                                        .setBlockItems(BlockItemSet.newBuilder()
                                                        .addAllBlockItems(blockItems)
                                                        .build())
                                        .build());

                        metricsService.get(LiveBlockItemsSent).add(blockItems.size());
                        LOGGER.log(
                                        INFO,
                                        "Number of block items sent: "
                                                        + metricsService.get(LiveBlockItemsSent).get());
                } else {
                        LOGGER.log(ERROR, "Not allowed to send next batch of block items");
                }

                return streamEnabled.get();
        }

        /**
         * The PublishStreamObserver class implements the StreamObserver interface to
         * observe the
         * stream.
         */
        @Override
        public boolean streamBlock(Block block) {

                List<List<BlockItem>> streamingBatches = ChunkUtils.chunkify(block.getItemsList(),
                                blockStreamConfig.blockItemsBatchSize());
                for (List<BlockItem> streamingBatch : streamingBatches) {
                        if (streamEnabled.get()) {
                                requestStreamObserver.onNext(PublishStreamRequest.newBuilder()
                                                .setBlockItems(BlockItemSet.newBuilder()
                                                                .addAllBlockItems(streamingBatch)
                                                                .build())
                                                .build());
                                metricsService.get(LiveBlockItemsSent).add(streamingBatch.size());
                                LOGGER.log(
                                                DEBUG,
                                                "Number of block items sent: "
                                                                + metricsService.get(LiveBlockItemsSent).get());
                        } else {
                                LOGGER.log(ERROR, "Not allowed to send next batch of block items");
                                break;
                        }
                }
                publishedBlocks++;
                return streamEnabled.get();
        }

        /**
         * Sends a onCompleted message to the server and waits for a short period of
         * time to ensure the message is sent.
         *
         * @throws InterruptedException if the thread is interrupted
         */
        @Override
        public void completeStreaming() throws InterruptedException {
                requestStreamObserver.onCompleted();
                Thread.sleep(100);
        }

        /**
         * Gets the number of published blocks.
         *
         * @return the number of published blocks
         */
        @Override
        public int getPublishedBlocks() {
                return publishedBlocks;
        }

        /**
         * Gets the last known statuses.
         *
         * @return the last known statuses
         */
        @Override
        public List<String> getLastKnownStatuses() {
                return lastKnownStatuses;
        }

        /**
         * Shutdowns the channel.
         */
        @Override
        public void shutdown() {
                channel.shutdown();
        }
}
