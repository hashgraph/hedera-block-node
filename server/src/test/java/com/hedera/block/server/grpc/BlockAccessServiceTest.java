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

package com.hedera.block.server.grpc;

import static com.hedera.block.server.Constants.FULL_SERVICE_NAME_BLOCK_ACCESS;
import static com.hedera.block.server.Constants.SERVICE_NAME_BLOCK_ACCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.pbj.PbjBlockAccessService;
import com.hedera.block.server.pbj.PbjBlockAccessServiceProxy;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.protoc.SingleBlockResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockAccessServiceTest {

    @Mock
    private Flow.Subscriber<SingleBlockResponse> responseObserver;

    @Mock
    private BlockReader<Block> blockReader;

    @Mock
    private BlockWriter<List<BlockItem>> blockWriter;

    @Mock
    private ServiceStatus serviceStatus;

    private static final int testTimeout = 1000;

    @TempDir
    private Path testPath;

    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig config;
    private PbjBlockAccessService blockAccessService;

    @BeforeEach
    public void setUp() throws IOException {

        blockNodeContext =
                TestConfigUtil.getTestBlockNodeContext(Map.of("persistence.storage.rootPath", testPath.toString()));
        config = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);

        blockAccessService = new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);
    }

    @Test
    public void testServiceName() {
        assertEquals(SERVICE_NAME_BLOCK_ACCESS, blockAccessService.serviceName());
    }

    @Test
    public void testFullName() {
        assertEquals(FULL_SERVICE_NAME_BLOCK_ACCESS, blockAccessService.fullName());
    }

    @Test
    public void testMethods() {
        assertEquals(1, blockAccessService.methods().size());
    }

    //    @Test
    //    void testProto() {
    //        BlockAccessService blockAccessService =
    //                new BlockAccessService(
    //                        serviceStatus, blockReader, blockNodeContext.metricsService());
    //        Descriptors.FileDescriptor fileDescriptor = blockAccessService.proto();
    //        // Verify the current rpc methods on
    //        Descriptors.ServiceDescriptor blockAccessServiceDescriptor =
    //                fileDescriptor.getServices().stream()
    //                        .filter(
    //                                service ->
    //                                        service.getName()
    //                                                .equals(Constants.SERVICE_NAME_BLOCK_ACCESS))
    //                        .findFirst()
    //                        .orElse(null);
    //
    //        Assertions.assertNotNull(
    //                blockAccessServiceDescriptor,
    //                "Service descriptor not found for: " + Constants.SERVICE_NAME_BLOCK_ACCESS);
    //        assertEquals(1, blockAccessServiceDescriptor.getMethods().size());
    //
    //        assertNotNull(blockAccessServiceDescriptor.getName(), blockAccessService.serviceName());
    //
    //        // Verify the current rpc methods on the service
    //        Descriptors.MethodDescriptor singleBlockMethod =
    //                blockAccessServiceDescriptor.getMethods().stream()
    //                        .filter(method -> method.getName().equals(SINGLE_BLOCK_METHOD_NAME))
    //                        .findFirst()
    //                        .orElse(null);
    //
    //        assertEquals(SINGLE_BLOCK_METHOD_NAME, singleBlockMethod.getName());
    //    }
    //
    //    @Test
    //    void testSingleBlockHappyPath() throws IOException, ParseException {
    //
    //        final BlockReader<Block> blockReader = BlockAsDirReaderBuilder.newBuilder(config).build();
    //
    //        final BlockAccessService blockAccessService =
    //                new BlockAccessService(
    //                        serviceStatus, blockReader, blockNodeContext.metricsService());
    //
    //        // Enable the serviceStatus
    //        when(serviceStatus.isRunning()).thenReturn(true);
    //
    //        // Generate and persist a block
    //        final BlockWriter<List<BlockItem>> blockWriter =
    //                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
    //        final List<BlockItem> blockItems = generateBlockItems(1);
    //        blockWriter.write(blockItems);
    //
    //        // Get the block so we can verify the response payload
    //        final Optional<Block> blockOpt = blockReader.read(1);
    //        if (blockOpt.isEmpty()) {
    //            fail("Block 1 should be present");
    //            return;
    //        }
    //
    //        // Build a response to verify what's passed to the response observer
    //        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedSingleBlockResponse =
    //                fromPbjSingleBlockSuccessResponse(blockOpt.get());
    //
    //        // Build a request to invoke the service
    //        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
    //                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
    //                        .setBlockNumber(1)
    //                        .build();
    //
    //        // Call the service
    //        blockAccessService.protocSingleBlock(singleBlockRequest, responseObserver);
    //        verify(responseObserver, times(1)).onNext(expectedSingleBlockResponse);
    //    }
    //
    //    @Test
    //    void testSingleBlockNotFoundPath() throws IOException, ParseException {
    //
    //        // Get the block so we can verify the response payload
    //        when(blockReader.read(1)).thenReturn(Optional.empty());
    //
    //        // Build a response to verify what's passed to the response observer
    //        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedNotFound =
    //                buildSingleBlockNotFoundResponse();
    //
    //        // Build a request to invoke the service
    //        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
    //                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
    //                        .setBlockNumber(1)
    //                        .build();
    //
    //        final BlockAccessService blockAccessService =
    //                new BlockAccessService(
    //                        serviceStatus, blockReader, blockNodeContext.metricsService());
    //
    //        // Enable the serviceStatus
    //        when(serviceStatus.isRunning()).thenReturn(true);
    //
    //        blockAccessService.protocSingleBlock(singleBlockRequest, responseObserver);
    //        verify(responseObserver, times(1)).onNext(expectedNotFound);
    //    }
    //
    //    @Test
    //    void testSingleBlockServiceNotAvailable() {
    //
    //        final BlockAccessService blockAccessService =
    //                new BlockAccessService(
    //                        serviceStatus, blockReader, blockNodeContext.metricsService());
    //
    //        // Set the service status to not running
    //        when(serviceStatus.isRunning()).thenReturn(false);
    //
    //        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedNotAvailable =
    //                buildSingleBlockNotAvailableResponse();
    //
    //        // Build a request to invoke the service
    //        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
    //                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
    //                        .setBlockNumber(1)
    //                        .build();
    //        blockAccessService.protocSingleBlock(singleBlockRequest, responseObserver);
    //        verify(responseObserver, times(1)).onNext(expectedNotAvailable);
    //    }
    //
    //    @Test
    //    public void testSingleBlockIOExceptionPath() throws IOException, ParseException {
    //        final BlockAccessService blockAccessService =
    //                new BlockAccessService(
    //                        serviceStatus, blockReader, blockNodeContext.metricsService());
    //
    //        when(serviceStatus.isRunning()).thenReturn(true);
    //        when(blockReader.read(1)).thenThrow(new IOException("Test exception"));
    //
    //        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedNotAvailable =
    //                buildSingleBlockNotAvailableResponse();
    //
    //        // Build a request to invoke the service
    //        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
    //                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
    //                        .setBlockNumber(1)
    //                        .build();
    //        blockAccessService.protocSingleBlock(singleBlockRequest, responseObserver);
    //        verify(responseObserver, times(1)).onNext(expectedNotAvailable);
    //    }
    //
    //    @Test
    //    public void testSingleBlockParseExceptionPath() throws IOException, ParseException {
    //        final BlockAccessService blockAccessService =
    //                new BlockAccessService(
    //                        serviceStatus, blockReader, blockNodeContext.metricsService());
    //
    //        when(serviceStatus.isRunning()).thenReturn(true);
    //        when(blockReader.read(1)).thenThrow(new ParseException("Test exception"));
    //
    //        final com.hedera.hapi.block.protoc.SingleBlockResponse expectedNotAvailable =
    //                buildSingleBlockNotAvailableResponse();
    //
    //        // Build a request to invoke the service
    //        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
    //                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
    //                        .setBlockNumber(1)
    //                        .build();
    //        blockAccessService.protocSingleBlock(singleBlockRequest, responseObserver);
    //        verify(responseObserver, times(1)).onNext(expectedNotAvailable);
    //    }
    //
    //    @Test
    //    public void testUpdateInvokesRoutingWithLambdas() {
    //
    //        final BlockAccessService blockAccessService =
    //                new BlockAccessService(
    //                        serviceStatus, blockReader, blockNodeContext.metricsService());
    //
    //        GrpcService.Routing routing = mock(GrpcService.Routing.class);
    //        blockAccessService.update(routing);
    //
    //        verify(routing, timeout(testTimeout).times(1))
    //                .unary(eq(SINGLE_BLOCK_METHOD_NAME), any(ServerCalls.UnaryMethod.class));
    //    }
}
