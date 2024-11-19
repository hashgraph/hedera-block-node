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
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.pbj.PbjBlockAccessService;
import com.hedera.block.server.pbj.PbjBlockAccessServiceProxy;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SingleBlockResponseUnparsed;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private Flow.Subscriber<? super Bytes> responseObserver;

    @Mock
    private BlockReader<BlockUnparsed> blockReader;

    @Mock
    private BlockWriter<List<BlockItemUnparsed>> blockWriter;

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

    @Test
    void testSingleBlockHappyPath() throws IOException, ParseException {

        final BlockReader<BlockUnparsed> blockReader =
                BlockAsDirReaderBuilder.newBuilder(config).build();

        final PbjBlockAccessService blockAccessService =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);

        // Enable the serviceStatus
        when(serviceStatus.isRunning()).thenReturn(true);

        // Generate and persist a block
        final BlockWriter<List<BlockItemUnparsed>> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(1);
        blockWriter.write(blockItems);

        // Get the block so we can verify the response payload
        final Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        if (blockOpt.isEmpty()) {
            fail("Block 1 should be present");
            return;
        }

        // Build a response to verify what's passed to the response observer
        final SingleBlockResponseUnparsed expectedSingleBlockResponse = SingleBlockResponseUnparsed.newBuilder()
                .block(blockOpt.get())
                .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                .build();

        // Build a request to invoke the service
        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();

        final Pipeline<? super Bytes> pipeline =
                blockAccessService.open(PbjBlockAccessService.BlockAccessMethod.singleBlock, null, responseObserver);

        // Call the service
        pipeline.onNext(SingleBlockRequest.PROTOBUF.toBytes(singleBlockRequest));
        verify(responseObserver, times(1))
                .onNext(SingleBlockResponseUnparsed.PROTOBUF.toBytes(expectedSingleBlockResponse));
    }

    @Test
    void testSingleBlockNotFoundPath() throws IOException, ParseException {

        // Get the block so we can verify the response payload
        when(blockReader.read(1)).thenReturn(Optional.empty());

        // Build a response to verify what's passed to the response observer
        final SingleBlockResponseUnparsed expectedNotFound = SingleBlockResponseUnparsed.newBuilder()
                .status(SingleBlockResponseCode.READ_BLOCK_NOT_FOUND)
                .build();

        // Build a request to invoke the service
        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();

        final PbjBlockAccessService blockAccessService =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);

        // Enable the serviceStatus
        when(serviceStatus.isRunning()).thenReturn(true);

        final Pipeline<? super Bytes> pipeline =
                blockAccessService.open(PbjBlockAccessService.BlockAccessMethod.singleBlock, null, responseObserver);

        // Call the service
        pipeline.onNext(SingleBlockRequest.PROTOBUF.toBytes(singleBlockRequest));
        verify(responseObserver, times(1)).onNext(SingleBlockResponseUnparsed.PROTOBUF.toBytes(expectedNotFound));
    }

    @Test
    void testSingleBlockServiceNotAvailable() {

        final PbjBlockAccessService blockAccessService =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);

        // Set the service status to not running
        when(serviceStatus.isRunning()).thenReturn(false);

        final SingleBlockResponseUnparsed expectedNotAvailable = SingleBlockResponseUnparsed.newBuilder()
                .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                .build();

        // Build a request to invoke the service
        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();

        final Pipeline<? super Bytes> pipeline =
                blockAccessService.open(PbjBlockAccessService.BlockAccessMethod.singleBlock, null, responseObserver);

        // Call the service
        pipeline.onNext(SingleBlockRequest.PROTOBUF.toBytes(singleBlockRequest));
        verify(responseObserver, times(1)).onNext(SingleBlockResponseUnparsed.PROTOBUF.toBytes(expectedNotAvailable));
    }

    @Test
    public void testSingleBlockIOExceptionPath() throws IOException, ParseException {
        final PbjBlockAccessService blockAccessService =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);

        when(serviceStatus.isRunning()).thenReturn(true);
        when(blockReader.read(1)).thenThrow(new IOException("Test exception"));

        final SingleBlockResponseUnparsed expectedNotAvailable = SingleBlockResponseUnparsed.newBuilder()
                .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                .build();

        // Build a request to invoke the service
        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();

        final Pipeline<? super Bytes> pipeline =
                blockAccessService.open(PbjBlockAccessService.BlockAccessMethod.singleBlock, null, responseObserver);

        // Call the service
        pipeline.onNext(SingleBlockRequest.PROTOBUF.toBytes(singleBlockRequest));
        verify(responseObserver, times(1)).onNext(SingleBlockResponseUnparsed.PROTOBUF.toBytes(expectedNotAvailable));
    }

    @Test
    public void testSingleBlockParseExceptionPath() throws IOException, ParseException {
        final PbjBlockAccessService blockAccessService =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);

        when(serviceStatus.isRunning()).thenReturn(true);
        when(blockReader.read(1)).thenThrow(new ParseException("Test exception"));

        final SingleBlockResponseUnparsed expectedNotAvailable = SingleBlockResponseUnparsed.newBuilder()
                .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                .build();

        // Build a request to invoke the service
        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();

        final Pipeline<? super Bytes> pipeline =
                blockAccessService.open(PbjBlockAccessService.BlockAccessMethod.singleBlock, null, responseObserver);

        // Call the service
        pipeline.onNext(SingleBlockRequest.PROTOBUF.toBytes(singleBlockRequest));
        verify(responseObserver, times(1)).onNext(SingleBlockResponseUnparsed.PROTOBUF.toBytes(expectedNotAvailable));
    }
}
