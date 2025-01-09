// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.grpc.impl;

import static com.hedera.block.simulator.TestUtils.findFreePort;
import static com.hedera.block.simulator.TestUtils.getTestMetrics;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.block.simulator.grpc.ConsumerStreamGrpcClient;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.metrics.MetricsServiceImpl;
import com.hedera.hapi.block.protoc.BlockItemSet;
import com.hedera.hapi.block.protoc.BlockStreamServiceGrpc;
import com.hedera.hapi.block.protoc.SubscribeStreamRequest;
import com.hedera.hapi.block.protoc.SubscribeStreamResponse;
import com.hedera.hapi.block.protoc.SubscribeStreamResponseCode;
import com.hedera.hapi.block.stream.output.protoc.BlockHeader;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hedera.hapi.block.stream.protoc.BlockProof;
import com.swirlds.config.api.Configuration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConsumerStreamGrpcClientImplTest {
    @Mock
    private GrpcConfig grpcConfig;

    @Mock
    private BlockStreamConfig blockStreamConfig;

    private ConsumerStreamGrpcClient consumerStreamGrpcClientImpl;
    private Server server;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        final int serverPort = findFreePort();
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
        when(blockStreamConfig.lastKnownStatusesCapacity()).thenReturn(10);

        final Configuration config = TestUtils.getTestConfiguration();
        final MetricsService metricsService = new MetricsServiceImpl(getTestMetrics(config));
        consumerStreamGrpcClientImpl = new ConsumerStreamGrpcClientImpl(grpcConfig, blockStreamConfig, metricsService);
        consumerStreamGrpcClientImpl.init();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        consumerStreamGrpcClientImpl.completeStreaming();
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
}
