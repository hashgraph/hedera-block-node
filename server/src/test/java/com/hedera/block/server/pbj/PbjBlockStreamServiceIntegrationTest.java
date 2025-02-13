// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.pbj;

import static com.hedera.block.server.util.PbjProtoTestUtils.buildEmptyPublishStreamRequest;
import static com.hedera.block.server.util.PbjProtoTestUtils.buildEmptySubscribeStreamRequest;
import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.ack.AckHandler;
import com.hedera.block.server.ack.AckHandlerImpl;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.mediator.LiveStreamMediatorBuilder;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.notifier.NotifierImpl;
import com.hedera.block.server.persistence.StreamPersistenceHandlerImpl;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.write.AsyncBlockWriterFactory;
import com.hedera.block.server.persistence.storage.write.AsyncNoOpWriterFactory;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.service.ServiceStatusImpl;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.verification.StreamVerificationHandlerImpl;
import com.hedera.block.server.verification.VerificationConfig;
import com.hedera.block.server.verification.service.BlockVerificationService;
import com.hedera.block.server.verification.service.BlockVerificationServiceImpl;
import com.hedera.block.server.verification.session.BlockVerificationSessionFactory;
import com.hedera.block.server.verification.signature.SignatureVerifierDummy;
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
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.grpc.ServiceInterface;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.lmax.disruptor.BatchEventProcessor;
import io.helidon.webserver.WebServer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("FieldCanBeLocal")
@ExtendWith(MockitoExtension.class)
class PbjBlockStreamServiceIntegrationTest {
    private static final int JUNIT_TIMEOUT = 5_000;
    private static final int testTimeout = 2_000;

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
    private Notifier notifierMock;

    @Mock
    private WebServer webServerMock;

    @Mock
    private BlockReader<BlockUnparsed> blockReaderMock;

    @Mock
    private ServiceInterface.RequestOptions optionsMock;

    @Mock
    private AckHandler ackHandlerMock;

    @Mock
    private AsyncBlockWriterFactory asyncBlockWriterFactoryMock;

    @Mock
    private Executor executorMock;

    @TempDir
    private Path testLiveRootPath;

    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;

    @BeforeEach
    void setUp() throws IOException {
        final Map<String, String> properties = new HashMap<>();
        properties.put(PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY, testLiveRootPath.toString());
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(properties);
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);

        final String testConfigLiveRootPath = testConfig.liveRootPath();
        assertThat(testConfigLiveRootPath).isEqualTo(testLiveRootPath.toString());
    }

    @Disabled("@todo(642) make test deterministic via correct executor injection")
    @Test
    @Timeout(value = JUNIT_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    void testPublishBlockStreamRegistrationAndExecution() {
        final int numberOfBlocks = 1;
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfBlocks);
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = buildBlockStreamService(executor);

        // Register 3 producers - Opening a pipeline is not enough to register a producer.
        // pipeline.onNext() must be invoked to register the producer at the Helidon PBJ layer.
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, optionsMock, helidonPublishStreamObserver1);
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.publishBlockStream,
                        optionsMock,
                        helidonPublishStreamObserver2)
                .onNext(buildEmptyPublishStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.publishBlockStream,
                        optionsMock,
                        helidonPublishStreamObserver3)
                .onNext(buildEmptyPublishStreamRequest());

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver3)
                .onNext(buildEmptySubscribeStreamRequest());

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(numberOfBlocks);
        for (final BlockItemUnparsed blockItem : blockItems) {
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

        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(1)).onNext(any());
        verify(helidonPublishStreamObserver2, timeout(testTimeout).times(1)).onNext(any());
        verify(helidonPublishStreamObserver3, timeout(testTimeout).times(1)).onNext(any());

        // Close the stream as Helidon does
        helidonPublishStreamObserver1.onComplete();

        // verify the onCompleted() method is invoked on the wrapped StreamObserver
        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(1)).onComplete();
    }

    @Disabled("@todo(642) make test deterministic via correct executor injection")
    @Test
    @Timeout(value = JUNIT_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    void testFullProducerConsumerHappyPath() {
        final int numberOfBlocks = 5;

        final ExecutorService executor = Executors.newFixedThreadPool(numberOfBlocks);
        // Use a real BlockWriter to test the full integration
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = buildBlockStreamService(executor);

        // Register 3 producers - Opening a pipeline is not enough to register a producer.
        // pipeline.onNext() must be invoked to register the producer at the Helidon PBJ layer.
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, optionsMock, helidonPublishStreamObserver1);
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.publishBlockStream,
                        optionsMock,
                        helidonPublishStreamObserver2)
                .onNext(buildEmptyPublishStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.publishBlockStream,
                        optionsMock,
                        helidonPublishStreamObserver3)
                .onNext(buildEmptyPublishStreamRequest());

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver3)
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
        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(numberOfBlocks))
                .onNext(any());
        verify(helidonPublishStreamObserver2, timeout(testTimeout).times(numberOfBlocks))
                .onNext(any());
        verify(helidonPublishStreamObserver3, timeout(testTimeout).times(numberOfBlocks))
                .onNext(any());
    }

    @Test
    @Timeout(value = JUNIT_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    void testFullWithSubscribersAddedDynamically() {
        final int numberOfBlocks = 100;

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = buildBlockStreamService(executor);

        // Register a producer
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, optionsMock, helidonPublishStreamObserver1);

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver3)
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
                                optionsMock,
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
                                optionsMock,
                                subscribeStreamObserver5)
                        .onNext(buildEmptySubscribeStreamRequest());
            }
            // Add a new subscriber
            if (i == 88) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                optionsMock,
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
    @Timeout(value = JUNIT_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    void testSubAndUnsubWhileStreaming() throws InterruptedException {
        final int numberOfBlocks = 100;
        final LinkedHashMap<
                        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>,
                        BatchEventProcessor<ObjectEvent<SubscribeStreamResponseUnparsed>>>
                consumers = new LinkedHashMap<>();
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final LiveStreamMediator streamMediator = buildStreamMediator(consumers, serviceStatus);
        final AsyncNoOpWriterFactory writerFactory =
                new AsyncNoOpWriterFactory(ackHandlerMock, blockNodeContext.metricsService());
        final StreamPersistenceHandlerImpl blockNodeEventHandler = new StreamPersistenceHandlerImpl(
                streamMediator,
                notifierMock,
                blockNodeContext,
                serviceStatus,
                ackHandlerMock,
                writerFactory,
                executorMock);
        final StreamVerificationHandlerImpl streamVerificationHandler = new StreamVerificationHandlerImpl(
                streamMediator,
                notifierMock,
                blockNodeContext.metricsService(),
                serviceStatus,
                mock(BlockVerificationService.class));
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = new PbjBlockStreamServiceProxy(
                streamMediator,
                serviceStatus,
                blockNodeEventHandler,
                streamVerificationHandler,
                notifierMock,
                blockNodeContext);

        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, optionsMock, helidonPublishStreamObserver1);

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver3)
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
                final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> k =
                        consumers.firstEntry().getKey();
                streamMediator.unsubscribe(k);
            }

            // Remove 2nd subscriber
            if (i == 60) {
                // Pause here to ensure the last sent block item is received.
                // This makes the test deterministic.
                Thread.sleep(50);
                final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> k =
                        consumers.firstEntry().getKey();
                streamMediator.unsubscribe(k);
            }

            // Add a new subscriber
            if (i == 51) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                optionsMock,
                                subscribeStreamObserver4)
                        .onNext(buildEmptySubscribeStreamRequest());
            }

            // Remove 3rd subscriber
            if (i == 70) {
                // Pause here to ensure the last sent block item is received.
                // This makes the test deterministic.
                Thread.sleep(50);
                final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> k =
                        consumers.firstEntry().getKey();
                streamMediator.unsubscribe(k);
            }

            // Add a new subscriber
            if (i == 76) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                optionsMock,
                                subscribeStreamObserver5)
                        .onNext(buildEmptySubscribeStreamRequest());
            }

            // Add a new subscriber
            if (i == 88) {
                pbjBlockStreamServiceProxy
                        .open(
                                PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                                optionsMock,
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
    @Timeout(value = JUNIT_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    void testMediatorExceptionHandlingWhenPersistenceFailure() {
        final ConcurrentHashMap<
                        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>,
                        BatchEventProcessor<ObjectEvent<SubscribeStreamResponseUnparsed>>>
                consumers = new ConcurrentHashMap<>();
        // Use a spy to use the real object but also verify the behavior.
        final ServiceStatus serviceStatus = spy(new ServiceStatusImpl(blockNodeContext));
        doCallRealMethod().when(serviceStatus).setWebServer(webServerMock);
        doCallRealMethod().when(serviceStatus).isRunning();
        doCallRealMethod().when(serviceStatus).stopWebServer(any());
        serviceStatus.setWebServer(webServerMock);

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(1);

        // the mocked factory will throw a npe
        final LiveStreamMediator streamMediator = buildStreamMediator(consumers, serviceStatus);
        final Notifier notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final StreamPersistenceHandlerImpl blockNodeEventHandler = new StreamPersistenceHandlerImpl(
                streamMediator,
                notifier,
                blockNodeContext,
                serviceStatus,
                ackHandlerMock,
                asyncBlockWriterFactoryMock,
                executorMock);
        final StreamVerificationHandlerImpl streamVerificationHandler = new StreamVerificationHandlerImpl(
                streamMediator,
                notifier,
                blockNodeContext.metricsService(),
                serviceStatus,
                mock(BlockVerificationService.class));
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = new PbjBlockStreamServiceProxy(
                streamMediator,
                serviceStatus,
                blockNodeEventHandler,
                streamVerificationHandler,
                notifier,
                blockNodeContext);

        // Register a producer
        final Pipeline<? super Bytes> producerPipeline = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, optionsMock, helidonPublishStreamObserver1);

        // Register 3 consumers - Opening a pipeline is not enough to register a consumer.
        // pipeline.onNext() must be invoked to register the consumer at the Helidon PBJ layer.
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver1)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver2)
                .onNext(buildEmptySubscribeStreamRequest());
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver3)
                .onNext(buildEmptySubscribeStreamRequest());

        // 3 subscribers + 1 streamPersistenceHandler
        assertEquals(5, consumers.size());

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
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, optionsMock, helidonPublishStreamObserver2);

        expectedNoOpProducerPipeline.onNext(publishStreamRequest);

        verify(helidonPublishStreamObserver2, timeout(testTimeout).times(1)).onNext(buildEndOfStreamResponse());

        // Build a request to invoke the singleBlock service
        final SingleBlockRequest singleBlockRequest =
                SingleBlockRequest.newBuilder().blockNumber(1).build();

        final PbjBlockAccessServiceProxy pbjBlockAccessServiceProxy =
                new PbjBlockAccessServiceProxy(serviceStatus, blockReaderMock, blockNodeContext);

        // Simulate a consumer attempting to connect to the Block Node after the exception.
        final SingleBlockResponseUnparsed singleBlockResponse =
                pbjBlockAccessServiceProxy.singleBlock(singleBlockRequest);

        // Build a request to invoke the subscribeBlockStream service
        // Simulate a consumer attempting to connect to the Block Node after the exception.
        pbjBlockStreamServiceProxy
                .open(
                        PbjBlockStreamService.BlockStreamMethod.subscribeBlockStream,
                        optionsMock,
                        subscribeStreamObserver4)
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
        verify(webServerMock, timeout(testTimeout).times(1)).stop();

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

    private static Bytes buildSubscribeStreamResponse(final BlockItemUnparsed blockItem) {
        return SubscribeStreamResponseUnparsed.PROTOBUF.toBytes(SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(
                        BlockItemSetUnparsed.newBuilder().blockItems(blockItem).build())
                .build());
    }

    private static Bytes buildEndOfStreamResponse() {
        final EndOfStream endOfStream = EndOfStream.newBuilder()
                .status(PublishStreamResponseCode.STREAM_ITEMS_INTERNAL_ERROR)
                .blockNumber(1L)
                .build();
        return PublishStreamResponse.PROTOBUF.toBytes(
                PublishStreamResponse.newBuilder().status(endOfStream).build());
    }

    private BlockVerificationSessionFactory getBlockVerificationSessionFactory() {
        final VerificationConfig config = blockNodeContext.configuration().getConfigData(VerificationConfig.class);
        final SignatureVerifierDummy signatureVerifier = mock(SignatureVerifierDummy.class);
        when(signatureVerifier.verifySignature(any(), any())).thenReturn(true);
        final ExecutorService executorService = ForkJoinPool.commonPool();
        return new BlockVerificationSessionFactory(
                config, blockNodeContext.metricsService(), signatureVerifier, executorService);
    }

    private PbjBlockStreamServiceProxy buildBlockStreamService(final ExecutorService persistenceExecutor) {
        final BlockRemover blockRemover = mock(BlockRemover.class);
        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final LiveStreamMediator streamMediator = buildStreamMediator(new ConcurrentHashMap<>(32), serviceStatus);
        final Notifier notifier = new NotifierImpl(streamMediator, blockNodeContext, serviceStatus);
        final AckHandler blockManager =
                new AckHandlerImpl(notifier, false, serviceStatus, blockRemover, blockNodeContext.metricsService());
        final BlockVerificationSessionFactory blockVerificationSessionFactory = getBlockVerificationSessionFactory();
        final BlockVerificationService BlockVerificationService = new BlockVerificationServiceImpl(
                blockNodeContext.metricsService(), blockVerificationSessionFactory, blockManager);
        final AsyncNoOpWriterFactory writerFactory =
                new AsyncNoOpWriterFactory(blockManager, blockNodeContext.metricsService());
        final StreamPersistenceHandlerImpl blockNodeEventHandler = new StreamPersistenceHandlerImpl(
                streamMediator,
                notifier,
                blockNodeContext,
                serviceStatus,
                blockManager,
                writerFactory,
                persistenceExecutor);
        final StreamVerificationHandlerImpl streamVerificationHandler = new StreamVerificationHandlerImpl(
                streamMediator, notifier, blockNodeContext.metricsService(), serviceStatus, BlockVerificationService);
        return new PbjBlockStreamServiceProxy(
                streamMediator,
                serviceStatus,
                blockNodeEventHandler,
                streamVerificationHandler,
                notifier,
                blockNodeContext);
    }

    private LiveStreamMediator buildStreamMediator(
            final Map<
                            BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>,
                            BatchEventProcessor<ObjectEvent<SubscribeStreamResponseUnparsed>>>
                    subscribers,
            final ServiceStatus serviceStatus) {
        serviceStatus.setWebServer(webServerMock);
        return LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .subscribers(subscribers)
                .build();
    }
}
