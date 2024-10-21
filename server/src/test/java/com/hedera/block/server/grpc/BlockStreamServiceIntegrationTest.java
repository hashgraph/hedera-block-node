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

import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItems;
import static com.hedera.block.server.producer.Util.getFakeHash;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
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
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.service.ServiceStatusImpl;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.ItemAcknowledgement;
import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponseSet;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.lmax.disruptor.BatchEventProcessor;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.WebServer;
import java.io.IOException;
import java.nio.file.Files;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BlockStreamServiceIntegrationTest {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    @Mock private Notifier notifier;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
            publishStreamResponseObserver1;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
            publishStreamResponseObserver2;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse>
            publishStreamResponseObserver3;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SingleBlockResponse>
            singleBlockResponseStreamObserver;

    @Mock private com.hedera.hapi.block.protoc.SubscribeStreamRequest subscribeStreamRequest;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
            subscribeStreamObserver1;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
            subscribeStreamObserver2;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
            subscribeStreamObserver3;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
            subscribeStreamObserver4;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
            subscribeStreamObserver5;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse>
            subscribeStreamObserver6;

    @Mock private WebServer webServer;

    @Mock private BlockReader<Block> blockReader;
    @Mock private BlockWriter<List<BlockItem>> blockWriter;

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
    public void testPublishBlockStreamRegistrationAndExecution()
            throws IOException, NoSuchAlgorithmException {

        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();
        final var notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final var blockNodeEventHandler =
                new StreamPersistenceHandlerImpl(
                        streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        // Register 3 producers
        final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest>
                publishStreamObserver =
                        blockStreamService.protocPublishBlockStream(publishStreamResponseObserver1);
        blockStreamService.protocPublishBlockStream(publishStreamResponseObserver2);
        blockStreamService.protocPublishBlockStream(publishStreamResponseObserver3);

        // Register 3 consumers
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver1);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver2);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver3);

        List<BlockItem> blockItems = generateBlockItems(1);
        for (int i = 0; i < blockItems.size(); i++) {
            if (i == 9) {
                when(blockWriter.write(List.of(blockItems.get(i))))
                        .thenReturn(Optional.of(List.of(blockItems.get(i))));
            } else {
                when(blockWriter.write(List.of(blockItems.get(i)))).thenReturn(Optional.empty());
            }
        }

        for (BlockItem blockItem : blockItems) {
            final PublishStreamRequest publishStreamRequest =
                    PublishStreamRequest.newBuilder().blockItems(blockItem).build();

            // Calling onNext() as Helidon does with each block item for
            // the first producer.
            publishStreamObserver.onNext(fromPbj(publishStreamRequest));
        }

        // Verify all 10 BlockItems were sent to each of the 3 consumers
        verify(subscribeStreamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.getFirst())));
        verify(subscribeStreamObserver1, timeout(testTimeout).times(8))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.get(1))));
        verify(subscribeStreamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.get(9))));

        verify(subscribeStreamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.getFirst())));
        verify(subscribeStreamObserver2, timeout(testTimeout).times(8))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.get(1))));
        verify(subscribeStreamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.get(9))));

        verify(subscribeStreamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.getFirst())));
        verify(subscribeStreamObserver3, timeout(testTimeout).times(8))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.get(1))));
        verify(subscribeStreamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(buildSubscribeStreamResponse(blockItems.get(9))));

        // Only 1 response is expected per block sent
        final Acknowledgement itemAck = buildAck(List.of(blockItems.get(9)));
        final PublishStreamResponse publishStreamResponse =
                PublishStreamResponse.newBuilder().acknowledgement(itemAck).build();

        // Verify all 3 producers received the response
        verify(publishStreamResponseObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));

        verify(publishStreamResponseObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));

        verify(publishStreamResponseObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));

        // Close the stream as Helidon does
        publishStreamObserver.onCompleted();

        // verify the onCompleted() method is invoked on the wrapped StreamObserver
        verify(publishStreamResponseObserver1, timeout(testTimeout).times(1)).onCompleted();
    }

    @Test
    public void testSubscribeBlockStream() throws IOException {

        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        serviceStatus.setWebServer(webServer);

        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();

        final var streamMediator =
                LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus).build();

        // Build the BlockStreamService
        final var blockNodeEventHandler =
                new StreamPersistenceHandlerImpl(
                        streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final BlockStreamService blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        // Subscribe the consumers
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver1);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver2);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver3);

        // Subscribe the producer
        final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest> streamObserver =
                blockStreamService.protocPublishBlockStream(publishStreamResponseObserver1);

        // Build the BlockItem
        final List<BlockItem> blockItems = generateBlockItems(1);
        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder()
                        .blockItems(List.of(blockItems.getFirst()))
                        .build();

        // Calling onNext() with a BlockItem
        streamObserver.onNext(fromPbj(publishStreamRequest));

        // Verify the counter was incremented
        assertEquals(1, blockNodeContext.metricsService().get(LiveBlockItems).get());

        verify(blockWriter, timeout(testTimeout).times(1)).write(List.of(blockItems.getFirst()));

        final SubscribeStreamResponseSet subscribeStreamResponseSet =
                SubscribeStreamResponseSet.newBuilder()
                        .blockItems(List.of(blockItems.getFirst()))
                        .build();
        final SubscribeStreamResponse subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().blockItems(subscribeStreamResponseSet).build();

        verify(subscribeStreamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(subscribeStreamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(subscribeStreamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
    }

    @Test
    public void testFullHappyPath() throws IOException {
        int numberOfBlocks = 100;

        final BlockWriter<List<BlockItem>> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
        final BlockStreamService blockStreamService = buildBlockStreamService(blockWriter);

        // Pass a StreamObserver to the producer as Helidon does
        final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest> streamObserver =
                blockStreamService.protocPublishBlockStream(publishStreamResponseObserver1);

        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver1);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver2);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver3);

        final List<BlockItem> blockItems = generateBlockItems(numberOfBlocks);
        for (BlockItem blockItem : blockItems) {
            final PublishStreamRequest publishStreamRequest =
                    PublishStreamRequest.newBuilder().blockItems(blockItem).build();
            streamObserver.onNext(fromPbj(publishStreamRequest));
        }

        verifySubscribeStreamResponse(
                numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver1, blockItems);
        verifySubscribeStreamResponse(
                numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver2, blockItems);
        verifySubscribeStreamResponse(
                numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver3, blockItems);

        streamObserver.onCompleted();

        verify(publishStreamResponseObserver1, timeout(testTimeout).times(100)).onNext(any());
    }

    @Test
    public void testFullWithSubscribersAddedDynamically() {

        int numberOfBlocks = 100;

        final BlockStreamService blockStreamService = buildBlockStreamService(blockWriter);

        // Pass a StreamObserver to the producer as Helidon does
        final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest> streamObserver =
                blockStreamService.protocPublishBlockStream(publishStreamResponseObserver1);

        final List<BlockItem> blockItems = generateBlockItems(numberOfBlocks);

        // Subscribe the initial consumers
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver1);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver2);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver3);

        for (int i = 0; i < blockItems.size(); i++) {
            final PublishStreamRequest publishStreamRequest =
                    PublishStreamRequest.newBuilder().blockItems(blockItems.get(i)).build();

            // Add a new subscriber
            if (i == 51) {
                blockStreamService.protocSubscribeBlockStream(
                        subscribeStreamRequest, subscribeStreamObserver4);
            }

            // Transmit the BlockItem
            streamObserver.onNext(fromPbj(publishStreamRequest));

            // Add a new subscriber
            if (i == 76) {
                blockStreamService.protocSubscribeBlockStream(
                        subscribeStreamRequest, subscribeStreamObserver5);
            }

            // Add a new subscriber
            if (i == 88) {
                blockStreamService.protocSubscribeBlockStream(
                        subscribeStreamRequest, subscribeStreamObserver6);
            }
        }

        // Verify subscribers who were listening before the stream started
        verifySubscribeStreamResponse(
                numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver1, blockItems);
        verifySubscribeStreamResponse(
                numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver2, blockItems);
        verifySubscribeStreamResponse(
                numberOfBlocks, 0, numberOfBlocks, subscribeStreamObserver3, blockItems);

        // Verify subscribers added while the stream was in progress.
        // The Helidon-provided StreamObserver onNext() method will only
        // be called once a Header BlockItem is reached. So, pass in
        // the number of BlockItems to wait to verify that the method
        // was called.
        verifySubscribeStreamResponse(
                numberOfBlocks, 59, numberOfBlocks, subscribeStreamObserver4, blockItems);
        verifySubscribeStreamResponse(
                numberOfBlocks, 79, numberOfBlocks, subscribeStreamObserver5, blockItems);
        verifySubscribeStreamResponse(
                numberOfBlocks, 89, numberOfBlocks, subscribeStreamObserver6, blockItems);

        streamObserver.onCompleted();
    }

    @Test
    public void testSubAndUnsubWhileStreaming() throws InterruptedException {

        int numberOfBlocks = 100;

        final LinkedHashMap<
                        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>>,
                        BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
                consumers = new LinkedHashMap<>();

        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = buildStreamMediator(consumers, serviceStatus);
        final var blockNodeEventHandler =
                new StreamPersistenceHandlerImpl(
                        streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final var blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        // Pass a StreamObserver to the producer as Helidon does
        final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest> streamObserver =
                blockStreamService.protocPublishBlockStream(publishStreamResponseObserver1);

        final List<BlockItem> blockItems = generateBlockItems(numberOfBlocks);

        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver1);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver2);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver3);

        for (int i = 0; i < blockItems.size(); i++) {

            // Transmit the BlockItem
            streamObserver.onNext(
                    fromPbj(
                            PublishStreamRequest.newBuilder()
                                    .blockItems(blockItems.get(i))
                                    .build()));

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
                blockStreamService.protocSubscribeBlockStream(
                        subscribeStreamRequest, subscribeStreamObserver4);
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
                blockStreamService.protocSubscribeBlockStream(
                        subscribeStreamRequest, subscribeStreamObserver5);
            }

            // Add a new subscriber
            if (i == 88) {
                blockStreamService.protocSubscribeBlockStream(
                        subscribeStreamRequest, subscribeStreamObserver6);
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
        verifySubscribeStreamResponse(
                numberOfBlocks, 59, numberOfBlocks, subscribeStreamObserver4, blockItems);
        verifySubscribeStreamResponse(
                numberOfBlocks, 79, numberOfBlocks, subscribeStreamObserver5, blockItems);
        verifySubscribeStreamResponse(
                numberOfBlocks, 89, numberOfBlocks, subscribeStreamObserver6, blockItems);

        streamObserver.onCompleted();
    }

    @Test
    public void testMediatorExceptionHandlingWhenPersistenceFailure() throws IOException {
        final ConcurrentHashMap<
                        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>>,
                        BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
                consumers = new ConcurrentHashMap<>();

        // Use a spy to use the real object but also verify the behavior.
        final ServiceStatus serviceStatus = spy(new ServiceStatusImpl(blockNodeContext));
        doCallRealMethod().when(serviceStatus).setWebServer(webServer);
        doCallRealMethod().when(serviceStatus).isRunning();
        doCallRealMethod().when(serviceStatus).stopWebServer(any());
        serviceStatus.setWebServer(webServer);

        final List<BlockItem> blockItems = generateBlockItems(1);

        // Use a spy to make sure the write() method throws an IOException
        final BlockWriter<List<BlockItem>> blockWriter =
                spy(BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build());
        doThrow(IOException.class).when(blockWriter).write(blockItems);

        final var streamMediator = buildStreamMediator(consumers, serviceStatus);
        final var notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final var blockNodeEventHandler =
                new StreamPersistenceHandlerImpl(
                        streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
        final var blockStreamService =
                new BlockStreamService(
                        streamMediator,
                        blockReader,
                        serviceStatus,
                        blockNodeEventHandler,
                        notifier,
                        blockNodeContext);

        final BlockAccessService blockAccessService =
                new BlockAccessService(
                        serviceStatus, blockReader, blockNodeContext.metricsService());

        // Subscribe the consumers
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver1);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver2);
        blockStreamService.protocSubscribeBlockStream(
                subscribeStreamRequest, subscribeStreamObserver3);

        // Initialize the producer
        final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest> streamObserver =
                blockStreamService.protocPublishBlockStream(publishStreamResponseObserver1);

        // Transmit a BlockItem
        final PublishStreamRequest publishStreamRequest =
                PublishStreamRequest.newBuilder().blockItems(blockItems).build();
        streamObserver.onNext(fromPbj(publishStreamRequest));

        // Use verify to make sure the serviceStatus.stopRunning() method is called
        // before the next block is transmitted.
        verify(serviceStatus, timeout(testTimeout).times(2)).stopRunning(any());

        // Simulate another producer attempting to connect to the Block Node after the exception.
        // Later, verify they received a response indicating the stream is closed.
        final StreamObserver<com.hedera.hapi.block.protoc.PublishStreamRequest>
                expectedNoOpStreamObserver =
                        blockStreamService.protocPublishBlockStream(publishStreamResponseObserver2);
        expectedNoOpStreamObserver.onNext(fromPbj(publishStreamRequest));

        verify(publishStreamResponseObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(buildEndOfStreamResponse()));

        // Build a request to invoke the singleBlock service
        final com.hedera.hapi.block.protoc.SingleBlockRequest singleBlockRequest =
                com.hedera.hapi.block.protoc.SingleBlockRequest.newBuilder()
                        .setBlockNumber(1)
                        .build();

        // Simulate a consumer attempting to connect to the Block Node after the exception.
        blockAccessService.protocSingleBlock(singleBlockRequest, singleBlockResponseStreamObserver);

        // Build a request to invoke the subscribeBlockStream service
        final SubscribeStreamRequest subscribeStreamRequest =
                SubscribeStreamRequest.newBuilder().startBlockNumber(1).build();
        // Simulate a consumer attempting to connect to the Block Node after the exception.
        blockStreamService.protocSubscribeBlockStream(
                fromPbj(subscribeStreamRequest), subscribeStreamObserver4);

        // The BlockItem expected to pass through since it was published
        // before the IOException was thrown.
        final SubscribeStreamResponseSet subscribeStreamResponseSet =
                SubscribeStreamResponseSet.newBuilder().blockItems(blockItems).build();

        final SubscribeStreamResponse subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().blockItems(subscribeStreamResponseSet).build();
        verify(subscribeStreamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(subscribeStreamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));
        verify(subscribeStreamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(subscribeStreamResponse));

        // Verify all the consumers received the end of stream response
        // TODO: Fix the response code when it's available
        final SubscribeStreamResponse endStreamResponse =
                SubscribeStreamResponse.newBuilder()
                        .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                        .build();
        verify(subscribeStreamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(endStreamResponse));
        verify(subscribeStreamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(endStreamResponse));
        verify(subscribeStreamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(endStreamResponse));

        assertEquals(3, consumers.size());

        // Verify the publishBlockStream service returned the expected
        // error code indicating the service is not available.
        verify(publishStreamResponseObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(buildEndOfStreamResponse()));

        // Adding extra time to allow the service to stop given
        // the built-in delay.
        verify(webServer, timeout(2000).times(1)).stop();

        // Verify the singleBlock service returned the expected
        // error code indicating the service is not available.
        final SingleBlockResponse expectedSingleBlockNotAvailable =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();

        verify(singleBlockResponseStreamObserver, timeout(testTimeout).times(1))
                .onNext(fromPbj(expectedSingleBlockNotAvailable));

        // TODO: Fix the response code when it's available
        final SubscribeStreamResponse expectedSubscriberStreamNotAvailable =
                SubscribeStreamResponse.newBuilder()
                        .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                        .build();
        verify(subscribeStreamObserver4, timeout(testTimeout).times(1))
                .onNext(fromPbj(expectedSubscriberStreamNotAvailable));
    }

    private static void verifySubscribeStreamResponse(
            int numberOfBlocks,
            int blockItemsToWait,
            int blockItemsToSkip,
            StreamObserver<com.hedera.hapi.block.protoc.SubscribeStreamResponse> streamObserver,
            List<BlockItem> blockItems) {

        // Each block has 10 BlockItems. Verify all the BlockItems
        // in a given block per iteration.
        for (int block = 0; block < numberOfBlocks; block += 10) {

            if (block < blockItemsToWait || block >= blockItemsToSkip) {
                continue;
            }

            final BlockItem headerBlockItem = blockItems.get(block);
            final SubscribeStreamResponse headerSubStreamResponse =
                    buildSubscribeStreamResponse(headerBlockItem);

            final BlockItem bodyBlockItem = blockItems.get(block + 1);
            final SubscribeStreamResponse bodySubStreamResponse =
                    buildSubscribeStreamResponse(bodyBlockItem);

            final BlockItem stateProofBlockItem = blockItems.get(block + 9);
            final SubscribeStreamResponse stateProofStreamResponse =
                    buildSubscribeStreamResponse(stateProofBlockItem);

            verify(streamObserver, timeout(testTimeout).times(1))
                    .onNext(fromPbj(headerSubStreamResponse));
            verify(streamObserver, timeout(testTimeout).times(8))
                    .onNext(fromPbj(bodySubStreamResponse));
            verify(streamObserver, timeout(testTimeout).times(1))
                    .onNext(fromPbj(stateProofStreamResponse));
        }
    }

    private static SubscribeStreamResponse buildSubscribeStreamResponse(BlockItem blockItem) {
        final SubscribeStreamResponseSet subscribeStreamResponseSet =
                SubscribeStreamResponseSet.newBuilder().blockItems(blockItem).build();
        return SubscribeStreamResponse.newBuilder().blockItems(subscribeStreamResponseSet).build();
    }

    private static PublishStreamResponse buildEndOfStreamResponse() {
        final EndOfStream endOfStream =
                EndOfStream.newBuilder()
                        .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                        .build();
        return PublishStreamResponse.newBuilder().status(endOfStream).build();
    }

    private BlockStreamService buildBlockStreamService(
            final BlockWriter<List<BlockItem>> blockWriter) {

        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = buildStreamMediator(new ConcurrentHashMap<>(32), serviceStatus);
        final var notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final var blockNodeEventHandler =
                new StreamPersistenceHandlerImpl(
                        streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);

        return new BlockStreamService(
                streamMediator,
                blockReader,
                serviceStatus,
                blockNodeEventHandler,
                notifier,
                blockNodeContext);
    }

    private LiveStreamMediator buildStreamMediator(
            final Map<
                            BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>>,
                            BatchEventProcessor<ObjectEvent<SubscribeStreamResponse>>>
                    subscribers,
            final ServiceStatus serviceStatus) {

        serviceStatus.setWebServer(webServer);

        return LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .subscribers(subscribers)
                .build();
    }

    public static Acknowledgement buildAck(@NonNull final List<BlockItem> blockItems)
            throws NoSuchAlgorithmException {
        ItemAcknowledgement itemAck =
                ItemAcknowledgement.newBuilder()
                        .itemsHash(Bytes.wrap(getFakeHash(blockItems)))
                        .build();

        return Acknowledgement.newBuilder().itemAck(itemAck).build();
    }
}
