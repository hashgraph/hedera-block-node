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
import static com.hedera.block.server.BlockStreamService.fromPbjSingleBlockSuccessResponse;
import static com.hedera.block.server.Constants.CLIENT_STREAMING_METHOD_NAME;
import static com.hedera.block.server.Constants.SERVER_STREAMING_METHOD_NAME;
import static com.hedera.block.server.Constants.SINGLE_BLOCK_METHOD_NAME;
import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static com.hedera.block.server.util.PersistTestUtils.reverseByteArray;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Descriptors;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.block.server.verifier.StreamVerifierBuilder;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Mock private Notifier notifier;

    @Mock private StreamObserver<com.hedera.hapi.block.protoc.SingleBlockResponse> responseObserver;

    @Mock private LiveStreamMediator streamMediator;

    @Mock private BlockReader<Block> blockReader;

    @Mock private BlockWriter<BlockItem> blockWriter;

    @Mock private ServiceStatus serviceStatus;

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private static final String TEMP_DIR = "block-node-unit-test-dir";

    private static final int testTimeout = 1000;

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
    public void testServiceName() throws IOException {

        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        // Verify the service name
        assertEquals(Constants.SERVICE_NAME, blockStreamService.serviceName());

        // Verify other methods not invoked
        verify(streamMediator, timeout(testTimeout).times(0)).publish(any(BlockItem.class));
    }

    @Test
    public void testProto() {

        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);
        Descriptors.FileDescriptor fileDescriptor = blockStreamService.proto();

        // Verify the current rpc methods
        assertEquals(5, fileDescriptor.getServices().getFirst().getMethods().size());

        // Verify other methods not invoked
        verify(streamMediator, timeout(testTimeout).times(0)).publish(any(BlockItem.class));
    }

    @Test
    void testSingleBlockHappyPath() throws IOException, ParseException {

        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockReader<Block> blockReader = BlockAsDirReaderBuilder.newBuilder(config).build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

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
                fromPbjSingleBlockSuccessResponse(blockOpt.get());

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
    void testSingleBlockNotFoundPath() throws IOException, ParseException {

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
        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        // Enable the serviceStatus
        when(serviceStatus.isRunning()).thenReturn(true);

        blockStreamService.protocSingleBlock(singleBlockRequest, responseObserver);
        verify(responseObserver, times(1)).onNext(expectedNotFound);
    }

    @Test
    void testSingleBlockServiceNotAvailable() {

        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

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
    public void testSingleBlockIOExceptionPath() throws IOException, ParseException {
        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

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
    public void testSingleBlockParseExceptionPath() throws IOException, ParseException {
        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        when(serviceStatus.isRunning()).thenReturn(true);
        when(blockReader.read(1)).thenThrow(new ParseException("Test exception"));

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

        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        GrpcService.Routing routing = mock(GrpcService.Routing.class);
        blockStreamService.update(routing);

        verify(routing, timeout(testTimeout).times(1))
                .bidi(eq(CLIENT_STREAMING_METHOD_NAME), any(ServerCalls.BidiStreamingMethod.class));
        verify(routing, timeout(testTimeout).times(1))
                .serverStream(
                        eq(SERVER_STREAMING_METHOD_NAME),
                        any(ServerCalls.ServerStreamingMethod.class));
        verify(routing, timeout(testTimeout).times(1))
                .unary(eq(SINGLE_BLOCK_METHOD_NAME), any(ServerCalls.UnaryMethod.class));
    }

    @Test
    public void testProtocParseExceptionHandling() {
        // TODO: We might be able to remove this test once we can remove the Translator class

        final var blockNodeEventHandler =
                StreamVerifierBuilder.newBuilder(
                                notifier, blockWriter, blockNodeContext, serviceStatus)
                        .build();
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

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
        verify(responseObserver, times(1)).onNext(fromPbj(expectedSingleBlockErrorResponse));
    }
}
