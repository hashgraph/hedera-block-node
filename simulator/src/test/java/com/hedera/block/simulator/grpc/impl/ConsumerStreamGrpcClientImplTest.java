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

import static com.hedera.block.simulator.TestUtils.getTestMetrics;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.block.simulator.grpc.ConsumerStreamGrpcClient;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.metrics.MetricsServiceImpl;
import com.hedera.hapi.block.protoc.*;
import com.hedera.hapi.block.stream.output.protoc.BlockHeader;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hedera.hapi.block.stream.protoc.BlockProof;
import com.swirlds.config.api.Configuration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConsumerStreamGrpcClientImplTest {
    @Mock
    private GrpcConfig grpcConfig;

    private MetricsService metricsService;
    private ConsumerStreamGrpcClient consumerStreamGrpcClientImpl;
    private Server server;
    private int serverPort;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        serverPort = findFreePort();
        server = ServerBuilder.forPort(serverPort)
                .addService(new BlockStreamServiceGrpc.BlockStreamServiceImplBase() {
                    @Override
                    public void subscribeBlockStream(
                            SubscribeStreamRequest request, StreamObserver<SubscribeStreamResponse> responseObserver) {

                        // Simulate streaming blocks
                        long startBlock = request.getStartBlockNumber();
                        long endBlock = request.getEndBlockNumber();

                        for (long i = startBlock; i < endBlock; i++) {
                            // Simulate block items
                            BlockItem blockItemHeader = BlockItem.newBuilder()
                                    .setBlockHeader(BlockHeader.newBuilder()
                                            .setNumber(i)
                                            .build())
                                    .build();
                            BlockItem blockItemProof = BlockItem.newBuilder()
                                    .setBlockProof(
                                            BlockProof.newBuilder().setBlock(i).build())
                                    .build();

                            BlockItemSet blockItems = BlockItemSet.newBuilder()
                                    .addBlockItems(blockItemHeader)
                                    .addBlockItems(blockItemProof)
                                    .build();

                            responseObserver.onNext(SubscribeStreamResponse.newBuilder()
                                    .setBlockItems(blockItems)
                                    .build());
                        }

                        // Send success status code at the end
                        responseObserver.onNext(SubscribeStreamResponse.newBuilder()
                                .setStatus(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                                .build());
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();

        when(grpcConfig.serverAddress()).thenReturn("localhost");
        when(grpcConfig.port()).thenReturn(serverPort);

        Configuration config = TestUtils.getTestConfiguration();
        metricsService = new MetricsServiceImpl(getTestMetrics(config));
        consumerStreamGrpcClientImpl = new ConsumerStreamGrpcClientImpl(grpcConfig, metricsService);
        consumerStreamGrpcClientImpl.init();
    }

    @AfterEach
    public void tearDown() {
        consumerStreamGrpcClientImpl.shutdown();
        server.shutdownNow();
    }

    @Test
    public void testInit() {
        assertTrue(consumerStreamGrpcClientImpl.getLastKnownStatuses().isEmpty());
    }

    @Test
    void requestBlocks_Success() throws InterruptedException {
        final long startBlock = 0;
        final long endBlock = 5;

        assertEquals(startBlock, consumerStreamGrpcClientImpl.getConsumedBlocks());
        assertTrue(consumerStreamGrpcClientImpl.getLastKnownStatuses().isEmpty());

        consumerStreamGrpcClientImpl.requestBlocks(startBlock, endBlock);

        // We check if the final status matches what we have send from the server.
        final String lastStatus =
                consumerStreamGrpcClientImpl.getLastKnownStatuses().getLast();
        assertTrue(lastStatus.contains("status: %s".formatted(SubscribeStreamResponseCode.READ_STREAM_SUCCESS.name())));

        assertEquals(endBlock, consumerStreamGrpcClientImpl.getConsumedBlocks());
    }

    @Test
    void requestBlocks_InvalidStartBlock() {
        final long startBlock = -1;
        final long endBlock = 5;

        assertThrows(
                IllegalArgumentException.class, () -> consumerStreamGrpcClientImpl.requestBlocks(startBlock, endBlock));
    }

    @Test
    void requestBlocks_InvalidEndBlock() {
        final long startBlock = 0;
        final long endBlock = -1;

        assertThrows(
                IllegalArgumentException.class, () -> consumerStreamGrpcClientImpl.requestBlocks(startBlock, endBlock));
    }

    @Test
    void completeStreaming_Success() throws InterruptedException {
        final long startBlock = 0;
        final long endBlock = 5;

        consumerStreamGrpcClientImpl.requestBlocks(startBlock, endBlock);
        consumerStreamGrpcClientImpl.completeStreaming();
    }

    @Test
    void shutdown() throws InterruptedException {
        final long startBlock = 0;
        final long endBlock = 5;

        consumerStreamGrpcClientImpl.shutdown();
        consumerStreamGrpcClientImpl.requestBlocks(startBlock, endBlock);

        // We check if the first status is UNAVAILABLE, because should fail immediately
        final String firstStatus =
                consumerStreamGrpcClientImpl.getLastKnownStatuses().getFirst();
        assertEquals(
                Status.UNAVAILABLE
                        .augmentDescription("Channel shutdown invoked")
                        .toString(),
                firstStatus);
    }

    private int findFreePort() throws IOException {
        // Find a free port
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
