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

package com.hedera.block.server;

import static com.hedera.block.server.BlockStreamService.buildSingleBlockNotAvailableResponse;
import static com.hedera.block.server.BlockStreamService.buildSingleBlockNotFoundResponse;
import static com.hedera.block.server.Constants.*;
import static com.hedera.block.server.Translator.toProtocSingleBlockResponse;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static com.hedera.block.server.util.PersistTestUtils.reverseByteArray;
import static java.lang.System.Logger.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BlockStreamServiceTest {

    @Mock private StreamObserver<com.hedera.hapi.block.protoc.SingleBlockResponse> responseObserver;

    @Mock private StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> streamMediator;

    @Mock private BlockReader<Block> blockReader;

    @Mock private ServiceStatus serviceStatus;

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private static final String TEMP_DIR = "block-node-unit-test-dir";

    private Path testPath;
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig config;

    @BeforeEach
    public void setUp() throws IOException {
        testPath = Files.createTempDirectory(TEMP_DIR);
        LOGGER.log(INFO, "Created temp directory: " + testPath.toString());

        blockNodeContext =
                TestConfigUtil.getTestBlockNodeContext(
                        Map.of("persistence.storage.rootPath", testPath.toString()));
        config = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    }

    @AfterEach
    public void tearDown() {
        TestUtils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testServiceName() throws IOException, NoSuchAlgorithmException {

        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        // Verify the service name
        assertEquals(Constants.SERVICE_NAME, blockStreamService.serviceName());

        // Verify other methods not invoked
        verify(streamMediator, never()).publish(any(BlockItem.class));
    }

    @Test
    public void testProto() throws IOException, NoSuchAlgorithmException {
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);
        Descriptors.FileDescriptor fileDescriptor = blockStreamService.proto();

        // Verify the current rpc methods
        assertEquals(5, fileDescriptor.getServices().getFirst().getMethods().size());

        // Verify other methods not invoked
        verify(streamMediator, never()).publish(any(BlockItem.class));
    }

    @Test
    void testSingleBlockHappyPath() throws IOException {

        final BlockReader<Block> blockReader = BlockAsDirReaderBuilder.newBuilder(config).build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        // Enable the serviceStatus
        when(serviceStatus.isRunning()).thenReturn(true);

        // Generate and persist a block
        final BlockWriter<BlockItem> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
        final List<BlockItem> blockItems = generateBlockItems(1);
        for (BlockItem blockItem : blockItems) {
            blockWriter.write(blockItem);
        }

        // Get the block so we can verify the response payload
        final Optional<Block> blockOpt = blockReader.read(1);
        if (blockOpt.isEmpty()) {
            fail("Block 1 should be present");
            return;
        }

        // Build a response to verify what's passed to the response observer
        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedSingleBlockResponse =
                toProtocSingleBlockResponse(blockOpt.get());

        // Build a request to invoke the service
        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
                        .setBlockNumber(1)
                        .build();

        // Call the service
        blockStreamService.protocSingleBlock(singleBlockRequest, responseObserver);
        verify(responseObserver, times(1)).onNext(expectedSingleBlockResponse);
    }

    @Test
    void testSingleBlockNotFoundPath() throws IOException {

        // Get the block so we can verify the response payload
        when(blockReader.read(1)).thenReturn(Optional.empty());

        // Build a response to verify what's passed to the response observer
        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedNotFound =
                buildSingleBlockNotFoundResponse();

        // Build a request to invoke the service
        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
                        .setBlockNumber(1)
                        .build();

        // Call the service
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        // Enable the serviceStatus
        when(serviceStatus.isRunning()).thenReturn(true);

        blockStreamService.protocSingleBlock(singleBlockRequest, responseObserver);
        verify(responseObserver, times(1)).onNext(expectedNotFound);
    }

    @Test
    void testSingleBlockServiceNotAvailable() throws InvalidProtocolBufferException {

        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        // Set the service status to not running
        when(serviceStatus.isRunning()).thenReturn(false);

        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedNotAvailable =
                buildSingleBlockNotAvailableResponse();

        // Build a request to invoke the service
        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
                        .setBlockNumber(1)
                        .build();
        blockStreamService.protocSingleBlock(singleBlockRequest, responseObserver);
        verify(responseObserver, times(1)).onNext(expectedNotAvailable);
    }

    @Test
    public void testSingleBlockIOExceptionPath() throws IOException {
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        // Set the service status to not running
        when(serviceStatus.isRunning()).thenReturn(true);
        when(blockReader.read(1)).thenThrow(new IOException("Test exception"));

        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedNotAvailable =
                buildSingleBlockNotAvailableResponse();

        // Build a request to invoke the service
        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
                        .setBlockNumber(1)
                        .build();
        blockStreamService.protocSingleBlock(singleBlockRequest, responseObserver);
        verify(responseObserver, times(1)).onNext(expectedNotAvailable);
    }

    @Test
    public void testUpdateInvokesRoutingWithLambdas() {

        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        GrpcService.Routing routing = mock(GrpcService.Routing.class);
        blockStreamService.update(routing);

        verify(routing, timeout(50).times(1))
                .bidi(eq(CLIENT_STREAMING_METHOD_NAME), any(ServerCalls.BidiStreamingMethod.class));
        verify(routing, timeout(50).times(1))
                .serverStream(
                        eq(SERVER_STREAMING_METHOD_NAME),
                        any(ServerCalls.ServerStreamingMethod.class));
        verify(routing, timeout(50).times(1))
                .unary(eq(SINGLE_BLOCK_METHOD_NAME), any(ServerCalls.UnaryMethod.class));
    }

    @Test
    public void testProtocParseExceptionHandling() throws IOException {
        // TODO: We might be able to remove this test once we can remove the Translator class

        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        // Build a request to invoke the service
        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
                spy(
                        com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
                                .setBlockNumber(1)
                                .build());

        // Create a corrupted set of bytes to provoke a parse exception
        byte[] okBytes = singleBlockRequest.toByteArray();
        when(singleBlockRequest.toByteArray()).thenReturn(reverseByteArray(okBytes));

        final SingleBlockResponse expectedSingleBlockErrorResponse =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();
        // Call the service
        blockStreamService.protocSingleBlock(singleBlockRequest, responseObserver);
        verify(responseObserver, times(1))
                .onNext(toProtocSingleBlockResponse(expectedSingleBlockErrorResponse));
    }
}
