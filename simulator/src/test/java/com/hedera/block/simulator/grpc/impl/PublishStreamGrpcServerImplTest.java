// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.grpc.impl;

import static com.hedera.block.simulator.TestUtils.findFreePort;
import static com.hedera.block.simulator.TestUtils.getTestMetrics;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.block.simulator.grpc.PublishStreamGrpcServer;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.metrics.MetricsServiceImpl;
import com.hedera.hapi.block.protoc.BlockItemSet;
import com.hedera.hapi.block.protoc.BlockStreamServiceGrpc;
import com.hedera.hapi.block.protoc.PublishStreamRequest;
import com.hedera.hapi.block.protoc.PublishStreamResponse;
import com.hedera.hapi.block.stream.output.protoc.BlockHeader;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hedera.hapi.block.stream.protoc.BlockProof;
import com.swirlds.config.api.Configuration;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PublishStreamGrpcServerImplTest {

    @Mock
    private GrpcConfig grpcConfig;

    private BlockStreamConfig blockStreamConfig;
    private MetricsService metricsService;
    private PublishStreamGrpcServer publishStreamGrpcServer;
    private ManagedChannel channel;
    private int serverPort;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        serverPort = findFreePort();
        when(grpcConfig.port()).thenReturn(serverPort);

        final Configuration config = TestUtils.getTestConfiguration();
        blockStreamConfig = config.getConfigData(BlockStreamConfig.class);
        metricsService = new MetricsServiceImpl(getTestMetrics(config));

        publishStreamGrpcServer = new PublishStreamGrpcServerImpl(grpcConfig, blockStreamConfig, metricsService);
        publishStreamGrpcServer.init();
        publishStreamGrpcServer.start();

        channel = ManagedChannelBuilder.forAddress("localhost", serverPort)
                .usePlaintext()
                .build();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (publishStreamGrpcServer != null) {
            publishStreamGrpcServer.shutdown();
        }
    }

    @Test
    void testInit() {
        assertTrue(publishStreamGrpcServer.getLastKnownStatuses().isEmpty());
        assertEquals(0, publishStreamGrpcServer.getProcessedBlocks());
    }

    @Test
    void testPublishBlockStream() throws InterruptedException {
        // Create a latch to wait for the stream to complete
        CountDownLatch latch = new CountDownLatch(1);

        // Create the blocking stub
        BlockStreamServiceGrpc.BlockStreamServiceStub stub = BlockStreamServiceGrpc.newStub(channel);

        // Create response observer
        StreamObserver<PublishStreamResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(PublishStreamResponse response) {
                // Verify response contains acknowledgement
                assertTrue(response.hasAcknowledgement());
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        // Start the stream
        StreamObserver<PublishStreamRequest> requestObserver = stub.publishBlockStream(responseObserver);

        // Send some test blocks
        for (int i = 0; i < 3; i++) {
            BlockItem blockItemHeader = BlockItem.newBuilder()
                    .setBlockHeader(BlockHeader.newBuilder().setNumber(i).build())
                    .build();

            BlockItem blockItemProof = BlockItem.newBuilder()
                    .setBlockProof(BlockProof.newBuilder().setBlock(i).build())
                    .build();

            BlockItemSet blockItems = BlockItemSet.newBuilder()
                    .addBlockItems(blockItemHeader)
                    .addBlockItems(blockItemProof)
                    .build();

            PublishStreamRequest request =
                    PublishStreamRequest.newBuilder().setBlockItems(blockItems).build();

            requestObserver.onNext(request);
        }

        // Complete the stream
        requestObserver.onCompleted();

        // Wait for the stream to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify the server state
        List<String> statuses = publishStreamGrpcServer.getLastKnownStatuses();
        assertFalse(statuses.isEmpty());
        assertEquals(3, publishStreamGrpcServer.getProcessedBlocks());
    }

    @Test
    void testServerShutdown() throws InterruptedException {
        publishStreamGrpcServer.shutdown();
        // Verify channel is terminated after shutdown
        channel.shutdown();
        assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
    }
}
