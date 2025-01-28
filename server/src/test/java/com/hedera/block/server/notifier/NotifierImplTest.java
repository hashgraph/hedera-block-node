// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.notifier;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Gauge.Producers;
import static com.hedera.block.server.util.PbjProtoTestUtils.buildEmptyPublishStreamRequest;
import static java.lang.System.Logger.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.manager.BlockManager;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.mediator.LiveStreamMediatorBuilder;
import com.hedera.block.server.mediator.Publisher;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.pbj.PbjBlockStreamService;
import com.hedera.block.server.pbj.PbjBlockStreamServiceProxy;
import com.hedera.block.server.persistence.StreamPersistenceHandlerImpl;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.producer.ProducerBlockItemObserver;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.service.ServiceStatusImpl;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.block.server.verification.StreamVerificationHandlerImpl;
import com.hedera.block.server.verification.service.BlockVerificationService;
import com.hedera.block.server.verification.service.NoOpBlockVerificationService;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.grpc.ServiceInterface;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.lmax.disruptor.BatchEventProcessor;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.WebServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotifierImplTest {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    @Mock
    private Notifiable mediator;

    @Mock
    private Publisher<List<BlockItemUnparsed>> publisher;

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private SubscriptionHandler<PublishStreamResponse> subscriptionHandler;

    @Mock
    private Pipeline<? super Bytes> helidonPublishStreamObserver1;

    @Mock
    private Pipeline<? super Bytes> helidonPublishStreamObserver2;

    @Mock
    private Pipeline<? super Bytes> helidonPublishStreamObserver3;

    @Mock
    private Pipeline<? super PublishStreamResponse> publishStreamObserver1;

    @Mock
    private Pipeline<? super PublishStreamResponse> publishStreamObserver2;

    @Mock
    private Pipeline<? super PublishStreamResponse> publishStreamObserver3;

    @Mock
    private InstantSource testClock;

    @Mock
    private WebServer webServer;

    @Mock
    private BlockWriter<List<BlockItemUnparsed>, Long> blockWriter;

    @Mock
    private ServiceInterface.RequestOptions options;

    @Mock
    BlockManager blockManager;

    private final long TIMEOUT_THRESHOLD_MILLIS = 100L;
    private final long TEST_TIME = 1_719_427_664_950L;

    private static final int testTimeout = 1000;

    private Path testPath;
    private BlockNodeContext blockNodeContext;

    private static final String TEMP_DIR = "notifier-unit-test-dir";

    @BeforeEach
    public void setUp() throws IOException {
        testPath = Files.createTempDirectory(TEMP_DIR);
        LOGGER.log(INFO, "Created temp directory: " + testPath.toString());

        blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    }

    @AfterEach
    public void tearDown() {
        TestUtils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testRegistration() throws NoSuchAlgorithmException {

        //        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);
        when(serviceStatus.isRunning()).thenReturn(true);

        final var notifier = new NotifierImpl(mediator, blockNodeContext, serviceStatus);
        final PbjBlockStreamServiceProxy pbjBlockStreamServiceProxy = buildBlockStreamService(notifier);

        // Register 3 producers - Opening a pipeline is not enough to register a producer.
        // pipeline.onNext() must be invoked to register the producer at the Helidon PBJ layer.
        final Pipeline<? super Bytes> producerPipeline1 = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver1);
        producerPipeline1.onNext(buildEmptyPublishStreamRequest());

        final Pipeline<? super Bytes> producerPipeline2 = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver2);
        producerPipeline2.onNext(buildEmptyPublishStreamRequest());

        final Pipeline<? super Bytes> producerPipeline3 = pbjBlockStreamServiceProxy.open(
                PbjBlockStreamService.BlockStreamMethod.publishBlockStream, options, helidonPublishStreamObserver3);
        producerPipeline3.onNext(buildEmptyPublishStreamRequest());

        long producers = blockNodeContext.metricsService().get(Producers).get();
        assertEquals(3, producers, "Expected 3 producers to be registered");

        Bytes blockHash = Bytes.wrap("1234");
        long blockNumber = 2L;
        boolean isDuplicated = false;

        notifier.sendAck(blockNumber, blockHash, isDuplicated);

        Acknowledgement blockAcknowledgement = notifier.buildAck(blockHash, blockNumber, isDuplicated);

        // Verify once the serviceStatus is not running that we do not publish the responses
        final var publishStreamResponse = PublishStreamResponse.newBuilder()
                .acknowledgement(blockAcknowledgement)
                .build();

        // Verify the response was received by all observers
        final Bytes publishStreamResponseBytes = PublishStreamResponse.PROTOBUF.toBytes(publishStreamResponse);

        verify(helidonPublishStreamObserver1, timeout(testTimeout).times(1)).onNext(publishStreamResponseBytes);
        verify(helidonPublishStreamObserver2, timeout(testTimeout).times(1)).onNext(publishStreamResponseBytes);
        verify(helidonPublishStreamObserver3, timeout(testTimeout).times(1)).onNext(publishStreamResponseBytes);

        // Unsubscribe the observers
        producerPipeline1.onComplete();
        producerPipeline2.onComplete();
        producerPipeline3.onComplete();

        producers = blockNodeContext.metricsService().get(Producers).get();
        assertEquals(0, producers, "Expected 0 producers to be registered");
    }

    @Test
    public void testServiceStatusNotRunning() throws NoSuchAlgorithmException {

        // Set the serviceStatus to not running
        when(serviceStatus.isRunning()).thenReturn(false);
        final var notifier = new TestNotifier(mediator, blockNodeContext, serviceStatus);
        final var concreteObserver1 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, publishStreamObserver1, blockNodeContext, serviceStatus);

        final var concreteObserver2 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, publishStreamObserver2, blockNodeContext, serviceStatus);

        final var concreteObserver3 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, publishStreamObserver3, blockNodeContext, serviceStatus);

        notifier.subscribe(concreteObserver1);
        notifier.subscribe(concreteObserver2);
        notifier.subscribe(concreteObserver3);

        assertTrue(notifier.isSubscribed(concreteObserver1), "Expected the notifier to have observer1 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver2), "Expected the notifier to have observer2 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver3), "Expected the notifier to have observer3 subscribed");

        Bytes blockHash = Bytes.wrap("1234");
        long blockNumber = 2L;
        boolean isDuplicated = false;

        notifier.sendAck(blockNumber, blockHash, isDuplicated);

        Acknowledgement blockAcknowledgement = notifier.buildAck(blockHash, blockNumber, isDuplicated);

        // Verify once the serviceStatus is not running that we do not publish the responses
        final var publishStreamResponse = PublishStreamResponse.newBuilder()
                .acknowledgement(blockAcknowledgement)
                .build();

        verify(publishStreamObserver1, timeout(testTimeout).times(0)).onNext(publishStreamResponse);
        verify(publishStreamObserver2, timeout(testTimeout).times(0)).onNext(publishStreamResponse);
        verify(publishStreamObserver3, timeout(testTimeout).times(0)).onNext(publishStreamResponse);
    }

    private static final class TestNotifier extends NotifierImpl {
        public TestNotifier(
                @NonNull final Notifiable mediator,
                @NonNull final BlockNodeContext blockNodeContext,
                @NonNull final ServiceStatus serviceStatus) {
            super(mediator, blockNodeContext, serviceStatus);
        }
    }

    private PbjBlockStreamServiceProxy buildBlockStreamService(final Notifier notifier) {

        final ServiceStatus serviceStatus = new ServiceStatusImpl(blockNodeContext);
        final var streamMediator = buildStreamMediator(new ConcurrentHashMap<>(32), serviceStatus);
        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus, blockManager);
        final BlockVerificationService blockVerificationService = new NoOpBlockVerificationService();

        final var streamVerificationHandler = new StreamVerificationHandlerImpl(
                streamMediator, notifier, blockNodeContext.metricsService(), serviceStatus, blockVerificationService);

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

        serviceStatus.setWebServer(webServer);

        return LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .subscribers(subscribers)
                .build();
    }
}
