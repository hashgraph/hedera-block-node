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

package com.hedera.block.server.pbj;

import static com.hedera.block.server.util.PbjProtoTestUtils.buildAck;
import static com.hedera.block.server.util.PbjProtoTestUtils.buildEmptyPublishStreamRequest;
import static com.hedera.block.server.util.PbjProtoTestUtils.buildEmptySubscribeStreamRequest;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static java.lang.System.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.mediator.LiveStreamMediatorBuilder;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.notifier.NotifierImpl;
import com.hedera.block.server.persistence.StreamPersistenceHandlerImpl;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.write.BlockAsLocalDirWriter;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.service.ServiceStatusImpl;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.BlockItemSetUnparsed;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.PublishStreamRequestUnparsed;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SingleBlockResponseUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.grpc.ServiceInterface;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.lmax.disruptor.BatchEventProcessor;
import io.helidon.webserver.WebServer;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PbjBlockStreamServiceIntegrationTest {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    @Mock
    private Notifier notifier;

    @Mock
    private Pipeline<? super Bytes> helidonPublishStreamObserver1;

    @Mock
    private Pipeline<? super Bytes> helidonPublishStreamObserver2;

    @Mock
    private Pipeline<? super Bytes> helidonPublishStreamObserver3;

    @Mock
    private Pipeline<? super Bytes> subscribeStreamObserver1;

    @Mock
    private Pipeline<? super Bytes> subscribeStreamObserver2;

    @Mock
    private Pipeline<? super Bytes> subscribeStreamObserver3;

    @Mock
    private Pipeline<? super Bytes> subscribeStreamObserver4;

    @Mock
    private Pipeline<? super Bytes> subscribeStreamObserver5;

    @Mock
    private Pipeline<? super Bytes> subscribeStreamObserver6;

    @Mock
    private WebServer webServer;

    @Mock
    private BlockWriter<List<BlockItemUnparsed>> blockWriter;

    @Mock
    private BlockReader<BlockUnparsed> blockReader;

    @Mock
    private ServiceInterface.RequestOptions options;

    @Mock
    private BlockRemover blockRemoverMock;

    @Mock
    private BlockPathResolver blockPathResolverMock;

    @TempDir
    private Path testPath;

    private BlockNodeContext blockNodeContext;

    private static final int testTimeout = 2000;

    @BeforeEach
    public void setUp() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("persistence.storage.liveRootPath", testPath.toString());
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(properties);
    }

    @AfterEach
    public void tearDown() {
        TestUtils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testPublishBlockStreamRegistrationAndExecution()
            throws IOException, NoSuchAlgorithmException, ParseException {

        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = buildBlockStreamService(blockWriter);

        // Register 3 producers - Opening a pipeline is not enough to register a producer.
        // pipeline.onNext() must be invoked to register the producer at the Helidon PBJ layer.
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver1);
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.publishBlockStream,
                        options,
                        helidonPublishStreamObserver2)
                .onNext(buildEmptyPublishStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.publishBlockStream,
                        options,
                        helidonPublishStreamObserver3)
                .onNext(buildEmptyPublishStreamRequest());

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver3)
                .onNext(buildEmptySubscribeStreamRequest());

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(1);
        for (int i = 0; i < blockItems.size(); i++) {
            if (i == 9) {
                when(blockWriter.write(List.of(blockItems.get(i)))).thenReturn(Optional.of(List.of(blockItems.get(i))));
            } else {
                when(blockWriter.write(List.of(blockItems.get(i)))).thenReturn(Optional.empty());
            }
        }

        for (BlockItemUnparsed blockItem : blockItems) {
            // Calling onNext() as Helidon does
            final BlockItemSetUnparsed blockItemSet =
                    BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build();
            final PublishStreamRequestUnparsed publishStreamRequest = PublishStreamRequestUnparsed.newBuilder()
                    .blockItems(blockItemSet)
                    .build();
            producerPipeline.onNext(PublishStreamRequestUnparsed.PROTOBUF.toBytes(publishStreamRequest));
        }

        // Verify all 10 BlockItems were sent to each of the 3 consumers
        verify(subscribeStreamObserver1, timeout(testTimeout).times(1))
                .onNext(buildSubscribeStreamResponse(blockItems.getFirst()));
        verify(subscribeStreamObserver1, timeout(testTimeout).times(8))
                .onNext(buildSubscribeStreamResponse(blockItems.get(1)));
        verify(subscribeStreamObserver1, timeout(testTimeout).times(1))
                .onNext(buildSubscribeStreamResponse(blockItems.get(9)));

        verify(subscribeStreamObserver2, timeout(testTimeout).times(1))
                .onNext(buildSubscribeStreamResponse(blockItems.getFirst()));
        verify(subscribeStreamObserver2, timeout(testTimeout).times(8))
                .onNext(buildSubscribeStreamResponse(blockItems.get(1)));
        verify(subscribeStreamObserver2, timeout(testTimeout).times(1))
                .onNext(buildSubscribeStreamResponse(blockItems.get(9)));

        verify(subscribeStreamObserver3, timeout(testTimeout).times(1))
                .onNext(buildSubscribeStreamResponse(blockItems.getFirst()));
        verify(subscribeStreamObserver3, timeout(testTimeout).times(8))
                .onNext(buildSubscribeStreamResponse(blockItems.get(1)));
        verify(subscribeStreamObserver3, timeout(testTimeout).times(1))
                .onNext(buildSubscribeStreamResponse(blockItems.get(9)));

        // Only 1 response is expected per block sent
        final Acknowledgement itemAck = buildAck(List.of(blockItems.get(9)));
        final PublishStreamResponse publishStreamResponse =
                PublishStreamResponse.newBuilder().acknowledgement(itemAck).build();

        // Verify all 3 producers received the response
        final Bytes responseBytes = PublishStreamResponse.PROTOBUF.toBytes(publishStreamResponse);
        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(1)).onNext(responseBytes);
        verify(helidonPublishStreamObserver2, timeout(testTimeout).times(1)).onNext(responseBytes);
        verify(helidonPublishStreamObserver3, timeout(testTimeout).times(1)).onNext(responseBytes);

        // Close the stream as Helidon does
        helidonPublishStreamObserver1.onComplete();

        // verify the onCompleted() method is invoked on the wrapped StreamObserver
        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(1)).onComplete();
    }

    @Test
    public void testFullProducerConsumerHappyPath() throws IOException {
        int numberOfBlocks = 100;

        // Use a real BlockWriter to test the full integration
        final BlockWriter<List<BlockItemUnparsed>> blockWriter = BlockAsLocalDirWriter.of(
                blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class), null);
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = buildBlockStreamService(blockWriter);

        // Register 3 producers - Opening a pipeline is not enough to register a producer.
        // pipeline.onNext() must be invoked to register the producer at the Helidon PBJ layer.
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver1);
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.publishBlockStream,
                        options,
                        helidonPublishStreamObserver2)
                .onNext(buildEmptyPublishStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.publishBlockStream,
                        options,
                        helidonPublishStreamObserver3)
                .onNext(buildEmptyPublishStreamRequest());

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver3)
                .onNext(buildEmptySubscribeStreamRequest());

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(numberOfBlocks);
        for (BlockItemUnparsed blockItem : blockItems) {
            // Calling onNext() as Helidon does
            final BlockItemSetUnparsed blockItemSet =
                    BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build();
            final PublishStreamRequestUnparsed publishStreamRequest = PublishStreamRequestUnparsed.newBuilder()
                    .blockItems(blockItemSet)
                    .build();
            producerPipeline.onNext(PublishStreamRequestUnparsed.PROTOBUF.toBytes(publishStreamRequest));
        }

        // Verify the subscribers received the data
        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver1, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver2, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver3, blockItems);

        // Verify the producers received all the responses
        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(100)).onNext(any());
        verify(helidonPublishStreamObserver2, timeout(testTimeout).times(100)).onNext(any());
        verify(helidonPublishStreamObserver3, timeout(testTimeout).times(100)).onNext(any());
    }

    @Test
    public void testFullWithSubscribersAddedDynamically() {
        int numberOfBlocks = 100;

        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = buildBlockStreamService(blockWriter);

        // Register a producer
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver1);

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver3)
                .onNext(buildEmptySubscribeStreamRequest());

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(numberOfBlocks);
        for (int i = 0; i < blockItems.size(); i++) {
            final PublishStreamRequestUnparsed publishStreamRequest = PublishStreamRequestUnparsed.newBuilder()
                    .blockItems(new BlockItemSetUnparsed(List.of(blockItems.get(i))))
                    .build();

            // Add a new subscriber
            if (i == 51) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                options,
                                subscribeStreamObserver4)
                        .onNext(buildEmptySubscribeStreamRequest());
            }

            // Transmit the BlockItem
            producerPipeline.onNext(PublishStreamRequestUnparsed.PROTOBUF.toBytes(publishStreamRequest));

            // Add a new subscriber
            if (i == 76) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                options,
                                subscribeStreamObserver5)
                        .onNext(buildEmptySubscribeStreamRequest());
            }

            // Add a new subscriber
            if (i == 88) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                options,
                                subscribeStreamObserver6)
                        .onNext(buildEmptySubscribeStreamRequest());
            }
        }

        // Verify subscribers who were listening before the stream started
        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver1, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver2, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver3, blockItems);

        // Verify subscribers added while the stream was in progress.
        // The Helidon-provided StreamObserver onNext() method will only
        // be called once a Header BlockItem is reached. So, pass in
        // the number of BlockItems to wait to verify that the method
        // was called.
        verifySubscribeStreamResponse(numberOfBlocks, 59, numberOfBlocks, subscribeStreamObserver4, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 79, numberOfBlocks, subscribeStreamObserver5, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 89, numberOfBlocks, subscribeStreamObserver6, blockItems);
    }

    @Test
    public void testSubAndUnsubWhileStreaming() throws InterruptedException {
        int numberOfBlocks = 100;
        final LinkedHashMap<
                        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>,
                        BatchEventProcessor<ObjectEvent<SubscribeStreamResponseUnparsed>>>
                consumers = new LinkedHashMap<>();

        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = buildStreamMediator(consumers, serviceStatus);
        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = new PbjBlockStreamServiceProxy(
                streamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext);

        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver1);

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver3)
                .onNext(buildEmptySubscribeStreamRequest());

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(numberOfBlocks);
        for (int i = 0; i < blockItems.size(); i++) {

            // Transmit the BlockItem
            final PublishStreamRequestUnparsed publishStreamRequest = PublishStreamRequestUnparsed.newBuilder()
                    .blockItems(new BlockItemSetUnparsed(List.of(blockItems.get(i))))
                    .build();
            producerPipeline.onNext(PublishStreamRequestUnparsed.PROTOBUF.toBytes(publishStreamRequest));

            // Remove 1st subscriber
            if (i == 10) {
                // Pause here to ensure the last sent block item is received.
                // This makes the test deterministic.
                Thread.sleep(50);
                final var k = consumers.firstEntry().getKey();
                streamMediator.unsubscribe(k);
            }

            // Remove 2nd subscriber
            if (i == 60) {
                // Pause here to ensure the last sent block item is received.
                // This makes the test deterministic.
                Thread.sleep(50);
                final var k = consumers.firstEntry().getKey();
                streamMediator.unsubscribe(k);
            }

            // Add a new subscriber
            if (i == 51) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                options,
                                subscribeStreamObserver4)
                        .onNext(buildEmptySubscribeStreamRequest());
            }

            // Remove 3rd subscriber
            if (i == 70) {
                // Pause here to ensure the last sent block item is received.
                // This makes the test deterministic.
                Thread.sleep(50);
                final var k = consumers.firstEntry().getKey();
                streamMediator.unsubscribe(k);
            }

            // Add a new subscriber
            if (i == 76) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                options,
                                subscribeStreamObserver5)
                        .onNext(buildEmptySubscribeStreamRequest());
            }

            // Add a new subscriber
            if (i == 88) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                options,
                                subscribeStreamObserver6)
                        .onNext(buildEmptySubscribeStreamRequest());
            }
        }

        // Verify subscribers who were listening before the stream started
        verifySubscribeStreamResponse(numberOfBlocks, 0, 10, subscribeStreamObserver1, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 0, 60, subscribeStreamObserver2, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 0, 70, subscribeStreamObserver3, blockItems);

        // Verify subscribers added while the stream was in progress.
        // The Helidon-provided StreamObserver onNext() method will only
        // be called once a Header BlockItem is reached. So, pass in
        // the number of BlockItems to wait to verify that the method
        // was called.
        verifySubscribeStreamResponse(numberOfBlocks, 59, numberOfBlocks, subscribeStreamObserver4, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 79, numberOfBlocks, subscribeStreamObserver5, blockItems);
        verifySubscribeStreamResponse(numberOfBlocks, 89, numberOfBlocks, subscribeStreamObserver6, blockItems);

        producerPipeline.onComplete();
    }

    @Test
    public void testMediatorExceptionHandlingWhenPersistenceFailure() throws IOException, ParseException {
        final ConcurrentHashMap<
                        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>,
                        BatchEventProcessor<ObjectEvent<SubscribeStreamResponseUnparsed>>>
                consumers = new ConcurrentHashMap<>();

        // Use a spy to use the real object but also verify the behavior.
        final ServiceStatus serviceStatus = spy(new ServiceStatusImpl(blockNodeContext));
        doCallRealMethod().when(serviceStatus).setWebServer(webServer);
        doCallRealMethod().when(serviceStatus).isRunning();
        doCallRealMethod().when(serviceStatus).stopWebServer(any());
        serviceStatus.setWebServer(webServer);

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(1);

        // Use a spy to make sure the write() method throws an IOException
        final BlockWriter<List<BlockItemUnparsed>> blockWriter = spy(BlockAsLocalDirWriter.of(
                blockNodeContext, mock(BlockRemover.class), mock(BlockPathResolver.class), null));
        doThrow(IOException.class).when(blockWriter).write(blockItems);

        final var streamMediator = buildStreamMediator(consumers, serviceStatus);
        final var notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = new PbjBlockStreamServiceProxy(
                streamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext);

        // Register a producer
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver1);

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver3)
                .onNext(buildEmptySubscribeStreamRequest());

        // 3 subscribers + 1 streamPersistenceHandler
        assertEquals(4, consumers.size());

        // Transmit a BlockItem
        final Bytes publishStreamRequest =
                PublishStreamRequestUnparsed.PROTOBUF.toBytes(PublishStreamRequestUnparsed.newBuilder()
                        .blockItems(new BlockItemSetUnparsed(blockItems))
                        .build());
        producerPipeline.onNext(publishStreamRequest);

        // Use verify to make sure the serviceStatus.stopRunning() method is called
        // before the next block is transmitted.
        verify(serviceStatus, timeout(testTimeout).times(2)).stopRunning(any());

        // Simulate another producer attempting to connect to the Block Node after the exception.
        // Later, verify they received a response indicating the stream is closed.
        final Pipeline<? super Bytes> expectedNoOpProducerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver2);

        expectedNoOpProducerPipeline.onNext(publishStreamRequest);

        verify(helidonPublishStreamObserver2, timeout(testTimeout).times(1)).onNext(buildEndOfStreamResponse());

        // Build a request to invoke the singleBlock service
        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();

        final PbjBlockAccessServiceProxy pbjBlockAccessServiceProxy =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);

        // Simulate a consumer attempting to connect to the Block Node after the exception.
        final SingleBlockResponseUnparsed singleBlockResponse =
                pbjBlockAccessServiceProxy.singleBlock(singleBlockRequest);

        // Build a request to invoke the subscribeBlockStream service
        // Simulate a consumer attempting to connect to the Block Node after the exception.
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, options, subscribeStreamObserver4)
                .onNext(buildEmptySubscribeStreamRequest());

        // The BlockItem expected to pass through since it was published
        // before the IOException was thrown.
        final BlockItemSetUnparsed blockItemSet =
                BlockItemSetUnparsed.newBuilder().blockItems(blockItems).build();

        final Bytes subscribeStreamResponse =
                SubscribeStreamResponseUnparsed.PROTOBUF.toBytes(SubscribeStreamResponseUnparsed.newBuilder()
                        .blockItems(blockItemSet)
                        .build());
        verify(subscribeStreamObserver1, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
        verify(subscribeStreamObserver2, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
        verify(subscribeStreamObserver3, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);

        // Verify all the consumers received the end of stream response
        // TODO: Fix the response code when it's available
        final Bytes endStreamResponse =
                SubscribeStreamResponseUnparsed.PROTOBUF.toBytes(SubscribeStreamResponseUnparsed.newBuilder()
                        .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                        .build());
        verify(subscribeStreamObserver1, timeout(testTimeout).times(1)).onNext(endStreamResponse);
        verify(subscribeStreamObserver2, timeout(testTimeout).times(1)).onNext(endStreamResponse);
        verify(subscribeStreamObserver3, timeout(testTimeout).times(1)).onNext(endStreamResponse);

        // Verify the publishBlockStream service returned the expected
        // error code indicating the service is not available.
        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(1)).onNext(buildEndOfStreamResponse());

        // Adding extra time to allow the service to stop given
        // the built-in delay.
        verify(webServer, timeout(testTimeout).times(1)).stop();

        assertEquals(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE, singleBlockResponse.status());

        // TODO: Fix the response code when it's available
        final Bytes expectedSubscriberStreamNotAvailable =
                SubscribeStreamResponseUnparsed.PROTOBUF.toBytes(SubscribeStreamResponseUnparsed.newBuilder()
                        .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                        .build());

        verify(subscribeStreamObserver4, timeout(testTimeout).times(1)).onNext(expectedSubscriberStreamNotAvailable);
    }

    private static void verifySubscribeStreamResponse(
            int numberOfBlocks,
            int blockItemsToWait,
            int blockItemsToSkip,
            Pipeline<? super Bytes> pipeline,
            List<BlockItemUnparsed> blockItems) {

        // Each block has 10 BlockItems. Verify all the BlockItems
        // in a given block per iteration.
        for (int block = 0; block < numberOfBlocks; block += 10) {

            if (block < blockItemsToWait || block >= blockItemsToSkip) {
                continue;
            }

            final BlockItemUnparsed headerBlockItem = blockItems.get(block);
            final Bytes headerSubStreamResponse = buildSubscribeStreamResponse(headerBlockItem);

            final BlockItemUnparsed bodyBlockItem = blockItems.get(block + 1);
            final Bytes bodySubStreamResponse = buildSubscribeStreamResponse(bodyBlockItem);

            final BlockItemUnparsed stateProofBlockItem = blockItems.get(block + 9);
            final Bytes stateProofStreamResponse = buildSubscribeStreamResponse(stateProofBlockItem);

            verify(pipeline, timeout(testTimeout).times(1)).onNext(headerSubStreamResponse);
            verify(pipeline, timeout(testTimeout).times(8)).onNext(bodySubStreamResponse);
            verify(pipeline, timeout(testTimeout).times(1)).onNext(stateProofStreamResponse);
        }
    }

    private static Bytes buildSubscribeStreamResponse(BlockItemUnparsed blockItem) {
        return SubscribeStreamResponseUnparsed.PROTOBUF.toBytes(SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(
                        BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build())
                .build());
    }

    private static Bytes buildEndOfStreamResponse() {
        final EndOfStream endOfStream = EndOfStream.newBuilder()
                .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                .build();
        return PublishStreamResponse.PROTOBUF.toBytes(
                PublishStreamResponse.newBuilder().status(endOfStream).build());
    }

    private PbjBlockStreamServiceProxy buildBlockStreamService(final BlockWriter<List<BlockItemUnparsed>> blockWriter) {

        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = buildStreamMediator(new ConcurrentHashMap<>(32), serviceStatus);
        final var notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);

        return new PbjBlockStreamServiceProxy(
                streamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext);
    }

    private LiveStreamMediator buildStreamMediator(
            final Map<
                            BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>,
                            BatchEventProcessor<ObjectEvent<SubscribeStreamResponseUnparsed>>>
                    subscribers,
            final ServiceStatus serviceStatus) {

        serviceStatus.setWebServer(webServer);

        return LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .subscribers(subscribers)
                .build();
    }
}
