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

import static com.hedera.block.server.Constants.CLIENT_STREAMING_METHOD_NAME;
import static com.hedera.block.server.Constants.SERVER_STREAMING_METHOD_NAME;
import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.util.PersistTestUtils.reverseByteArray;
import static java.lang.System.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Descriptors;
import com.hedera.block.server.Constants;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.StreamPersistenceHandlerImpl;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BlockStreamServiceTest {

    @Mock private Notifier notifier;

    @Mock private StreamObserver<com.hedera.hapi.block.protoc.SingleBlockResponse> responseObserver;

    @Mock private LiveStreamMediator streamMediator;

    @Mock private BlockReader<Block> blockReader;

    @Mock private BlockWriter<List<BlockItem>> blockWriter;

    @Mock private ServiceStatus serviceStatus;

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private static final int testTimeout = 1000;

    @TempDir private Path testPath;
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig config;

    @BeforeEach
    public void setUp() throws IOException {
        blockNodeContext =
                TestConfigUtil.getTestBlockNodeContext(
                        Map.of("persistence.storage.rootPath", testPath.toString()));
        config = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    }

    @Test
    public void testServiceName() {

        final var blockNodeEventHandler =
                new StreamPersistenceHandlerImpl(
                        streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        // Verify the service name
        Assertions.assertEquals(
                Constants.SERVICE_NAME_BLOCK_STREAM, blockStreamService.serviceName());

        // Verify other methods not invoked
        verify(streamMediator, timeout(testTimeout).times(0)).publish(any());
    }

    @Test
    public void testProto() {

        final var blockNodeEventHandler =
                new StreamPersistenceHandlerImpl(
                        streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);
        Descriptors.FileDescriptor fileDescriptor = blockStreamService.proto();

        // Verify the current rpc methods on
        Descriptors.ServiceDescriptor blockStreamServiceDescriptor =
                fileDescriptor.getServices().stream()
                        .filter(
                                service ->
                                        service.getName()
                                                .equals(Constants.SERVICE_NAME_BLOCK_STREAM))
                        .findFirst()
                        .orElse(null);

        Assertions.assertNotNull(
                blockStreamServiceDescriptor,
                "Service descriptor not found for: " + Constants.SERVICE_NAME_BLOCK_STREAM);
        assertEquals(2, blockStreamServiceDescriptor.getMethods().size());

        Descriptors.ServiceDescriptor blockAccessServiceDescriptor =
                fileDescriptor.getServices().stream()
                        .filter(
                                service ->
                                        service.getName()
                                                .equals(Constants.SERVICE_NAME_BLOCK_ACCESS))
                        .findFirst()
                        .orElse(null);
        Assertions.assertNotNull(
                blockAccessServiceDescriptor,
                "Service descriptor not found for: " + Constants.SERVICE_NAME_BLOCK_ACCESS);
        assertEquals(1, blockAccessServiceDescriptor.getMethods().size());

        // Verify other methods not invoked
        verify(streamMediator, timeout(testTimeout).times(0))
                .publish(Mockito.<List<BlockItem>>any());
    }

    @Test
    public void testUpdateInvokesRoutingWithLambdas() {

        final BlockStreamService blockStreamService = getBlockStreamService();

        GrpcService.Routing routing = mock(GrpcService.Routing.class);
        blockStreamService.update(routing);

        verify(routing, timeout(testTimeout).times(1))
                .bidi(eq(CLIENT_STREAMING_METHOD_NAME), any(ServerCalls.BidiStreamingMethod.class));
        verify(routing, timeout(testTimeout).times(1))
                .serverStream(
                        eq(SERVER_STREAMING_METHOD_NAME),
                        any(ServerCalls.ServerStreamingMethod.class));
    }

    private BlockStreamService getBlockStreamService() {
        final var blockNodeEventHandler =
                new StreamPersistenceHandlerImpl(
                        streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);
        return blockStreamService;
    }

    @Test
    public void testProtocParseExceptionHandling() {
        // TODO: We might be able to remove this test once we can remove the Translator class
        final BlockAccessService blockAccessService =
                new BlockAccessService(
                        serviceStatus, blockReader, blockNodeContext.metricsService());

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
        blockAccessService.protocSingleBlock(singleBlockRequest, responseObserver);
        verify(responseObserver, times(1)).onNext(fromPbj(expectedSingleBlockErrorResponse));
    }
}
