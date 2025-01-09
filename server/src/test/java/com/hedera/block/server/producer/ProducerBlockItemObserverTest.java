// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.producer;

import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.Publisher;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.service.ServiceStatusImpl;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.pbj.runtime.grpc.Pipeline;
import java.io.IOException;
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProducerBlockItemObserverTest {

    @Mock
    private InstantSource testClock;

    @Mock
    private Publisher<List<BlockItemUnparsed>> publisher;

    @Mock
    private SubscriptionHandler<PublishStreamResponse> subscriptionHandler;

    @Mock
    private Pipeline<PublishStreamResponse> helidonPublishPipeline;

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private ObjectEvent<PublishStreamResponse> objectEvent;

    private final long TEST_TIME = 1_719_427_664_950L;
    private final long TIMEOUT_THRESHOLD_MILLIS = 50L;
    private static final int testTimeout = 1000;

    BlockNodeContext testContext;

    @BeforeEach
    public void setUp() throws IOException {
        this.testContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY, String.valueOf(TIMEOUT_THRESHOLD_MILLIS)));
    }

    @Test
    public void testConfirmOnErrorNotCalled() {

        final ProducerBlockItemObserver producerBlockItemObserver = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, helidonPublishPipeline, testContext, serviceStatus);

        // Confirm that onError will call the handler
        // to unsubscribe but make sure onError is never
        // called on the helidonPublishPipeline.
        // Calling onError() on the helidonPublishPipeline
        // passed by the Helidon PBJ plugin may cause
        // a loop of calls.
        final Throwable t = new Throwable("Test error");
        producerBlockItemObserver.onError(t);
        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(any());
        verify(helidonPublishPipeline, never()).onError(t);
    }

    @Test
    public void testOnEventCallsUnsubscribeOnExpiration() {

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS + 1);
        final ProducerBlockItemObserver producerBlockItemObserver = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, helidonPublishPipeline, testContext, serviceStatus);

        producerBlockItemObserver.onEvent(objectEvent, 0, true);
        producerBlockItemObserver.onEvent(objectEvent, 0, true);

        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(producerBlockItemObserver);
    }

    @Test
    public void testOnSubscribe() {

        final ProducerBlockItemObserver producerBlockItemObserver = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, helidonPublishPipeline, testContext, serviceStatus);

        // Currently, our implementation of onSubscribe() is a
        // no-op.
        producerBlockItemObserver.onSubscribe(null);
    }

    @Test
    public void testEmptyBlockItems() {

        final ProducerBlockItemObserver producerBlockItemObserver = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, helidonPublishPipeline, testContext, serviceStatus);

        producerBlockItemObserver.onNext(List.of());
        verify(publisher, never()).publish(any());
    }

    @Test
    public void testOnlyErrorStreamResponseAllowedAfterStatusChange() {

        final ServiceStatus serviceStatus = new ServiceStatusImpl(testContext);

        final ProducerBlockItemObserver producerBlockItemObserver = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, helidonPublishPipeline, testContext, serviceStatus);

        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(1);

        // Send a request
        producerBlockItemObserver.onNext(blockItems);

        // Change the status of the service
        serviceStatus.stopRunning(getClass().getName());

        // Send another request
        producerBlockItemObserver.onNext(blockItems);

        // Confirm that closing the observer allowed only 1 response to be sent.
        verify(helidonPublishPipeline, timeout(testTimeout).times(1)).onNext(any());
    }

    @Test
    public void testClientEndStreamReceived() {

        final ProducerBlockItemObserver producerBlockItemObserver = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, helidonPublishPipeline, testContext, serviceStatus);

        producerBlockItemObserver.clientEndStreamReceived();

        // Confirm that the observer was unsubscribed
        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(producerBlockItemObserver);
    }
}
