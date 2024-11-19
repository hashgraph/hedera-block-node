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

import static com.hedera.block.server.producer.Util.getFakeHash;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;
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
import com.hedera.block.server.persistence.storage.read.BlockReader;
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
import com.hedera.hapi.block.ItemAcknowledgement;
import com.hedera.hapi.block.PublishStreamRequestUnparsed;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.lmax.disruptor.BatchEventProcessor;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.WebServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private SubscribeStreamRequest subscribeStreamRequest;

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

    private static final String TEMP_DIR = "block-node-unit-test-dir";

    private Path testPath;
    private BlockNodeContext blockNodeContext;

    private static final int testTimeout = 2000;

    @BeforeEach
    public void setUp() throws IOException {
        testPath = Files.createTempDirectory(TEMP_DIR);
        LOGGER.log(INFO, "Created temp directory: " + testPath.toString());

        Map<String, String> properties = new HashMap<>();
        properties.put("persistence.storage.rootPath", testPath.toString());

        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(properties);
    }

    @AfterEach
    public void tearDown() {
        TestUtils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testPublishBlockStreamRegistrationAndExecution() throws IOException, NoSuchAlgorithmException {

        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .build();
        final var notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);

        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = new PbjBlockStreamServiceProxy(
                streamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext);

        // Register 3 producers
        final Bytes publishStreamRequestBytes =
                PublishStreamRequestUnparsed.PROTOBUF.toBytes(PublishStreamRequestUnparsed.newBuilder()
                        .blockItems(BlockItemSetUnparsed.newBuilder().build())
                        .build());
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, null, helidonPublishStreamObserver1);
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.publishBlockStream, null, helidonPublishStreamObserver2)
                .onNext(publishStreamRequestBytes);
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.publishBlockStream, null, helidonPublishStreamObserver3)
                .onNext(publishStreamRequestBytes);

        final Bytes subscribeStreamRequestBytes = SubscribeStreamRequest.PROTOBUF.toBytes(
                SubscribeStreamRequest.newBuilder().startBlockNumber(1).build());
        // Register 3 consumers
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, null, subscribeStreamObserver1)
                .onNext(subscribeStreamRequestBytes);
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, null, subscribeStreamObserver2)
                .onNext(subscribeStreamRequestBytes);
        pbjBlockStreamServiceProxy
                .open(PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream, null, subscribeStreamObserver3)
                .onNext(subscribeStreamRequestBytes);

        List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(1);
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

    //    @Test
    //    public void testSubscribeBlockStream() throws IOException {
    //
    //        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
    //        serviceStatus.setWebServer(webServer);
    //
    //        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    //
    //        final var streamMediator = LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
    //                .build();
    //
    //        // Build the BlockStreamService
    //        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
    //                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
    //
    //        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = new PbjBlockStreamServiceProxy(
    //                streamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext);
    //
    //        // Subscribe the consumers
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver1);
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver2);
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver3);
    //
    //        // Subscribe the producer
    //        final Flow.Subscriber<PublishStreamRequest> producerBlockItemsObserver =
    //                pbjBlockStreamServiceProxy.publishBlockStream(helidonPublishStreamObserver1);
    //
    //        // Build the BlockItem
    //        final List<BlockItem> blockItems = generateBlockItems(1);
    //        final PublishStreamRequest publishStreamRequest = PublishStreamRequest.newBuilder()
    //                .blockItems(new BlockItemSet(List.of(blockItems.getFirst())))
    //                .build();
    //
    //        // Calling onNext() with a BlockItem
    //        producerBlockItemsObserver.onNext(publishStreamRequest);
    //
    //        // Verify the counter was incremented
    //        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());
    //
    //        verify(blockWriter, timeout(testTimeout).times(1)).write(List.of(blockItems.getFirst()));
    //
    //        final BlockItemSet blockItemSet = BlockItemSet.newBuilder()
    //                .blockItems(List.of(blockItems.getFirst()))
    //                .build();
    //        final SubscribeStreamResponse subscribeStreamResponse =
    //                SubscribeStreamResponse.newBuilder().blockItems(blockItemSet).build();
    //
    //        verify(subscribeStreamObserver1, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
    //        verify(subscribeStreamObserver2, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
    //        verify(subscribeStreamObserver3, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
    //    }
    //
    //    @Test
    //    public void testFullProducerConsumerHappyPath() throws IOException {
    //        int numberOfBlocks = 100;
    //
    //        final BlockWriter<List<BlockItem>> blockWriter =
    //                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
    //        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = buildBlockStreamService(blockWriter);
    //
    //        final Flow.Subscriber<PublishStreamRequest> producerBlockItemObserver =
    //                pbjBlockStreamServiceProxy.publishBlockStream(helidonPublishStreamObserver1);
    //
    //        // Subscribe the consumers
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver1);
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver2);
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver3);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(numberOfBlocks);
    //        for (BlockItem blockItem : blockItems) {
    //            final PublishStreamRequest publishStreamRequest = PublishStreamRequest.newBuilder()
    //                    .blockItems(new BlockItemSet(List.of(blockItem)))
    //                    .build();
    //            producerBlockItemObserver.onNext(publishStreamRequest);
    //        }
    //
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver1, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver2, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver3, blockItems);
    //
    //        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(100)).onNext(any());
    //    }

    //    @Test
    //    public void testFullWithSubscribersAddedDynamically() {
    //
    //        int numberOfBlocks = 100;
    //
    //        final PbjBlockStreamServiceProxy blockStreamServiceProxy = buildBlockStreamService(blockWriter);
    //        final Flow.Subscriber<PublishStreamRequest> streamObserver =
    //                blockStreamServiceProxy.publishBlockStream(helidonPublishStreamObserver1);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(numberOfBlocks);
    //
    //        // Subscribe the initial consumers
    //        blockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver1);
    //        blockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver2);
    //        blockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver3);
    //
    //        for (int i = 0; i < blockItems.size(); i++) {
    //            final PublishStreamRequest publishStreamRequest = PublishStreamRequest.newBuilder()
    //                    .blockItems(new BlockItemSet(List.of(blockItems.get(i))))
    //                    .build();
    //
    //            // Add a new subscriber
    //            if (i == 51) {
    //                blockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver4);
    //            }
    //
    //            // Transmit the BlockItem
    //            streamObserver.onNext(publishStreamRequest);
    //
    //            // Add a new subscriber
    //            if (i == 76) {
    //                blockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver5);
    //            }
    //
    //            // Add a new subscriber
    //            if (i == 88) {
    //                blockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver6);
    //            }
    //        }
    //
    //        // Verify subscribers who were listening before the stream started
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver1, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver2, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver3, blockItems);
    //
    //        // Verify subscribers added while the stream was in progress.
    //        // The Helidon-provided StreamObserver onNext() method will only
    //        // be called once a Header BlockItem is reached. So, pass in
    //        // the number of BlockItems to wait to verify that the method
    //        // was called.
    //        verifySubscribeStreamResponse(numberOfBlocks, 59, numberOfBlocks, subscribeStreamObserver4, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 79, numberOfBlocks, subscribeStreamObserver5, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 89, numberOfBlocks, subscribeStreamObserver6, blockItems);
    //    }
    //
    //    @Test
    //    public void testSubAndUnsubWhileStreaming() throws InterruptedException {
    //
    //        int numberOfBlocks = 100;
    //
    //        final LinkedHashMap<
    //                        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>>,
    //                        BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
    //                consumers = new LinkedHashMap<>();
    //
    //        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
    //        final var streamMediator = buildStreamMediator(consumers, serviceStatus);
    //        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
    //                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
    //        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = new PbjBlockStreamServiceProxy(
    //                streamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext);
    //
    //        // Pass a StreamObserver to the producer as Helidon does
    //        final Flow.Subscriber<PublishStreamRequest> streamObserver =
    //                pbjBlockStreamServiceProxy.publishBlockStream(helidonPublishStreamObserver1);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(numberOfBlocks);
    //
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver1);
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver2);
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver3);
    //
    //        for (int i = 0; i < blockItems.size(); i++) {
    //
    //            // Transmit the BlockItem
    //            streamObserver.onNext(PublishStreamRequest.newBuilder()
    //                    .blockItems(new BlockItemSet(List.of(blockItems.get(i))))
    //                    .build());
    //
    //            // Remove 1st subscriber
    //            if (i == 10) {
    //                // Pause here to ensure the last sent block item is received.
    //                // This makes the test deterministic.
    //                Thread.sleep(50);
    //                final var k = consumers.firstEntry().getKey();
    //                streamMediator.unsubscribe(k);
    //            }
    //
    //            // Remove 2nd subscriber
    //            if (i == 60) {
    //                // Pause here to ensure the last sent block item is received.
    //                // This makes the test deterministic.
    //                Thread.sleep(50);
    //                final var k = consumers.firstEntry().getKey();
    //                streamMediator.unsubscribe(k);
    //            }
    //
    //            // Add a new subscriber
    //            if (i == 51) {
    //                pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver4);
    //            }
    //
    //            // Remove 3rd subscriber
    //            if (i == 70) {
    //                // Pause here to ensure the last sent block item is received.
    //                // This makes the test deterministic.
    //                Thread.sleep(50);
    //                final var k = consumers.firstEntry().getKey();
    //                streamMediator.unsubscribe(k);
    //            }
    //
    //            // Add a new subscriber
    //            if (i == 76) {
    //                pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver5);
    //            }
    //
    //            // Add a new subscriber
    //            if (i == 88) {
    //                pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver6);
    //            }
    //        }
    //
    //        // Verify subscribers who were listening before the stream started
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, 10, subscribeStreamObserver1, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, 60, subscribeStreamObserver2, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 0, 70, subscribeStreamObserver3, blockItems);
    //
    //        // Verify subscribers added while the stream was in progress.
    //        // The Helidon-provided StreamObserver onNext() method will only
    //        // be called once a Header BlockItem is reached. So, pass in
    //        // the number of BlockItems to wait to verify that the method
    //        // was called.
    //        verifySubscribeStreamResponse(numberOfBlocks, 59, numberOfBlocks, subscribeStreamObserver4, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 79, numberOfBlocks, subscribeStreamObserver5, blockItems);
    //        verifySubscribeStreamResponse(numberOfBlocks, 89, numberOfBlocks, subscribeStreamObserver6, blockItems);
    //
    //        streamObserver.onComplete();
    //    }
    //
    //    @Test
    //    public void testMediatorExceptionHandlingWhenPersistenceFailure() throws IOException {
    //        final ConcurrentHashMap<
    //                        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>>,
    //                        BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
    //                consumers = new ConcurrentHashMap<>();
    //
    //        // Use a spy to use the real object but also verify the behavior.
    //        final ServiceStatus serviceStatus = spy(new ServiceStatusImpl(blockNodeContext));
    //        doCallRealMethod().when(serviceStatus).setWebServer(webServer);
    //        doCallRealMethod().when(serviceStatus).isRunning();
    //        doCallRealMethod().when(serviceStatus).stopWebServer(any());
    //        serviceStatus.setWebServer(webServer);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(1);
    //
    //        // Use a spy to make sure the write() method throws an IOException
    //        final BlockWriter<List<BlockItem>> blockWriter =
    //                spy(BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build());
    //        doThrow(IOException.class).when(blockWriter).write(blockItems);
    //
    //        final var streamMediator = buildStreamMediator(consumers, serviceStatus);
    //        final var notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
    //        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
    //                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
    //        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = new PbjBlockStreamServiceProxy(
    //                streamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext);
    //
    //        // Subscribe the consumers
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver1);
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver2);
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver3);
    //
    //        // 3 subscribers + 1 streamPersistenceHandler
    //        assertEquals(4, consumers.size());
    //
    //        // Initialize the producer
    //        final Flow.Subscriber<PublishStreamRequest> producerBlockItemObserver =
    //                pbjBlockStreamServiceProxy.publishBlockStream(helidonPublishStreamObserver1);
    //
    //        // Transmit a BlockItem
    //        final PublishStreamRequest publishStreamRequest = PublishStreamRequest.newBuilder()
    //                .blockItems(new BlockItemSet(blockItems))
    //                .build();
    //        producerBlockItemObserver.onNext(publishStreamRequest);
    //
    //        // Use verify to make sure the serviceStatus.stopRunning() method is called
    //        // before the next block is transmitted.
    //        verify(serviceStatus, timeout(testTimeout).times(2)).stopRunning(any());
    //
    //        // Simulate another producer attempting to connect to the Block Node after the exception.
    //        // Later, verify they received a response indicating the stream is closed.
    //        final Flow.Subscriber<PublishStreamRequest> expectedNoOpStreamObserver =
    //                pbjBlockStreamServiceProxy.publishBlockStream(helidonPublishStreamObserver2);
    //        expectedNoOpStreamObserver.onNext(publishStreamRequest);
    //
    //        verify(helidonPublishStreamObserver2, timeout(testTimeout).times(1)).onNext(buildEndOfStreamResponse());
    //
    //        // Build a request to invoke the singleBlock service
    //        final SingleBlockRequest singleBlockRequest =
    //                SingleBlockRequest.newBuilder().blockNumber(1).build();
    //
    //        final PbjBlockAccessServiceProxy pbjBlockAccessServiceProxy =
    //                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext);
    //
    //        // Simulate a consumer attempting to connect to the Block Node after the exception.
    //        final SingleBlockResponse singleBlockResponse =
    // pbjBlockAccessServiceProxy.singleBlock(singleBlockRequest);
    //
    //        // Build a request to invoke the subscribeBlockStream service
    //        final SubscribeStreamRequest subscribeStreamRequest =
    //                SubscribeStreamRequest.newBuilder().startBlockNumber(1).build();
    //        // Simulate a consumer attempting to connect to the Block Node after the exception.
    //        pbjBlockStreamServiceProxy.subscribeBlockStream(subscribeStreamRequest, subscribeStreamObserver4);
    //
    //        // The BlockItem expected to pass through since it was published
    //        // before the IOException was thrown.
    //        final BlockItemSet blockItemSet =
    //                BlockItemSet.newBuilder().blockItems(blockItems).build();
    //
    //        final SubscribeStreamResponse subscribeStreamResponse =
    //                SubscribeStreamResponse.newBuilder().blockItems(blockItemSet).build();
    //        verify(subscribeStreamObserver1, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
    //        verify(subscribeStreamObserver2, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
    //        verify(subscribeStreamObserver3, timeout(testTimeout).times(1)).onNext(subscribeStreamResponse);
    //
    //        // Verify all the consumers received the end of stream response
    //        // TODO: Fix the response code when it's available
    //        final SubscribeStreamResponse endStreamResponse = SubscribeStreamResponse.newBuilder()
    //                .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
    //                .build();
    //        verify(subscribeStreamObserver1, timeout(testTimeout).times(1)).onNext(endStreamResponse);
    //        verify(subscribeStreamObserver2, timeout(testTimeout).times(1)).onNext(endStreamResponse);
    //        verify(subscribeStreamObserver3, timeout(testTimeout).times(1)).onNext(endStreamResponse);
    //
    //        // Verify the publishBlockStream service returned the expected
    //        // error code indicating the service is not available.
    //        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(1)).onNext(buildEndOfStreamResponse());
    //
    //        // Adding extra time to allow the service to stop given
    //        // the built-in delay.
    //        verify(webServer, timeout(testTimeout).times(1)).stop();
    //
    //        assertEquals(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE, singleBlockResponse.status());
    //
    //        // TODO: Fix the response code when it's available
    //        final SubscribeStreamResponse expectedSubscriberStreamNotAvailable = SubscribeStreamResponse.newBuilder()
    //                .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
    //                .build();
    //        verify(subscribeStreamObserver4,
    // timeout(testTimeout).times(1)).onNext(expectedSubscriberStreamNotAvailable);
    //    }

    //    private static void verifySubscribeStreamResponse(
    //            int numberOfBlocks,
    //            int blockItemsToWait,
    //            int blockItemsToSkip,
    //            Pipeline<Bytes> pipeline,
    //            List<BlockItemUnparsed> blockItems) {
    //
    //        // Each block has 10 BlockItems. Verify all the BlockItems
    //        // in a given block per iteration.
    //        for (int block = 0; block < numberOfBlocks; block += 10) {
    //
    //            if (block < blockItemsToWait || block >= blockItemsToSkip) {
    //                continue;
    //            }
    //
    //            final BlockItemUnparsed headerBlockItem = blockItems.get(block);
    //            final Bytes headerSubStreamResponse = buildSubscribeStreamResponse(headerBlockItem);
    //
    //            final BlockItemUnparsed bodyBlockItem = blockItems.get(block + 1);
    //            final Bytes bodySubStreamResponse = buildSubscribeStreamResponse(bodyBlockItem);
    //
    //            final BlockItemUnparsed stateProofBlockItem = blockItems.get(block + 9);
    //            final Bytes stateProofStreamResponse = buildSubscribeStreamResponse(stateProofBlockItem);
    //
    //            verify(pipeline, timeout(testTimeout).times(1)).onNext(headerSubStreamResponse);
    //            verify(pipeline, timeout(testTimeout).times(8)).onNext(bodySubStreamResponse);
    //            verify(pipeline, timeout(testTimeout).times(1)).onNext(stateProofStreamResponse);
    //        }
    //    }

    private static Bytes buildSubscribeStreamResponse(BlockItemUnparsed blockItem) {
        return SubscribeStreamResponseUnparsed.PROTOBUF.toBytes(SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(
                        BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build())
                .build());
    }

    private static PublishStreamResponse buildEndOfStreamResponse() {
        final EndOfStream endOfStream = EndOfStream.newBuilder()
                .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                .build();
        return PublishStreamResponse.newBuilder().status(endOfStream).build();
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

    public static Acknowledgement buildAck(@NonNull final List<BlockItemUnparsed> blockItems)
            throws NoSuchAlgorithmException {
        ItemAcknowledgement itemAck = ItemAcknowledgement.newBuilder()
                .itemsHash(Bytes.wrap(getFakeHash(blockItems)))
                .build();

        return Acknowledgement.newBuilder().itemAck(itemAck).build();
    }
}
