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

package com.hedera.block.server.notifier;

import static com.hedera.block.server.notifier.NotifierImpl.buildErrorStreamResponse;
import static com.hedera.block.server.pbj.PbjBlockStreamServiceIntegrationTest.buildAck;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.mediator.Publisher;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.producer.ProducerBlockItemObserver;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.service.ServiceStatusImpl;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.grpc.Pipeline;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.InstantSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotifierImplTest {

    @Mock
    private Notifiable mediator;

    @Mock
    private Publisher<List<BlockItem>> publisher;

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private SubscriptionHandler<PublishStreamResponse> subscriptionHandler;

    @Mock
    private Pipeline<PublishStreamResponse> streamObserver1;

    @Mock
    private Pipeline<PublishStreamResponse> streamObserver2;

    @Mock
    private Pipeline<PublishStreamResponse> streamObserver3;

    @Mock
    private InstantSource testClock;

    private final long TIMEOUT_THRESHOLD_MILLIS = 100L;
    private final long TEST_TIME = 1_719_427_664_950L;

    private static final int testTimeout = 1000;

    private final BlockNodeContext testContext;

    public NotifierImplTest() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put(TestConfigUtil.MEDIATOR_RING_BUFFER_SIZE_KEY, String.valueOf(1024));
        this.testContext = TestConfigUtil.getTestBlockNodeContext(properties);
    }

    @Test
    public void testRegistration() throws NoSuchAlgorithmException {

        final ServiceStatus serviceStatus = new ServiceStatusImpl(testContext);
        final var notifier = new NotifierImpl(mediator, testContext, serviceStatus);

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var concreteObserver1 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver1, testContext, serviceStatus);

        final var concreteObserver2 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver2, testContext, serviceStatus);

        final var concreteObserver3 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver3, testContext, serviceStatus);

        notifier.subscribe(concreteObserver1);
        notifier.subscribe(concreteObserver2);
        notifier.subscribe(concreteObserver3);

        assertTrue(notifier.isSubscribed(concreteObserver1), "Expected the notifier to have observer1 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver2), "Expected the notifier to have observer2 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver3), "Expected the notifier to have observer3 subscribed");

        List<BlockItem> blockItems = generateBlockItems(1);
        notifier.publish(blockItems);

        // Verify the response was received by all observers
        final var publishStreamResponse = PublishStreamResponse.newBuilder()
                .acknowledgement(buildAck(blockItems))
                .build();
        verify(streamObserver1, timeout(testTimeout).times(1)).onNext(publishStreamResponse);
        verify(streamObserver2, timeout(testTimeout).times(1)).onNext(publishStreamResponse);
        verify(streamObserver3, timeout(testTimeout).times(1)).onNext(publishStreamResponse);

        // Unsubscribe the observers
        notifier.unsubscribe(concreteObserver1);
        assertFalse(notifier.isSubscribed(concreteObserver1), "Expected the notifier to have unsubscribed observer1");

        notifier.unsubscribe(concreteObserver2);
        assertFalse(notifier.isSubscribed(concreteObserver2), "Expected the notifier to have unsubscribed observer2");

        notifier.unsubscribe(concreteObserver3);
        assertFalse(notifier.isSubscribed(concreteObserver3), "Expected the notifier to have unsubscribed observer3");
    }

    @Test
    public void testTimeoutExpiredHandling() throws InterruptedException {

        when(serviceStatus.isRunning()).thenReturn(true);

        final var notifier = new NotifierImpl(mediator, testContext, serviceStatus);

        // Set the clocks to be expired
        final InstantSource testClock1 = mock(InstantSource.class);
        when(testClock1.millis()).thenReturn(TEST_TIME, TEST_TIME + 1501L);

        final InstantSource testClock2 = mock(InstantSource.class);
        when(testClock2.millis()).thenReturn(TEST_TIME, TEST_TIME + 1501L);

        final InstantSource testClock3 = mock(InstantSource.class);
        when(testClock3.millis()).thenReturn(TEST_TIME, TEST_TIME + 1501L);

        final var concreteObserver1 = new ProducerBlockItemObserver(
                testClock1, publisher, notifier, streamObserver1, testContext, serviceStatus);

        final var concreteObserver2 = new ProducerBlockItemObserver(
                testClock2, publisher, notifier, streamObserver2, testContext, serviceStatus);

        final var concreteObserver3 = new ProducerBlockItemObserver(
                testClock3, publisher, notifier, streamObserver3, testContext, serviceStatus);

        notifier.subscribe(concreteObserver1);
        notifier.subscribe(concreteObserver2);
        notifier.subscribe(concreteObserver3);

        assertTrue(notifier.isSubscribed(concreteObserver1), "Expected the notifier to have observer1 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver2), "Expected the notifier to have observer2 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver3), "Expected the notifier to have observer3 subscribed");

        List<BlockItem> blockItems = generateBlockItems(1);
        notifier.publish(blockItems);

        Thread.sleep(testTimeout);

        assertFalse(notifier.isSubscribed(concreteObserver1), "Expected the notifier to have observer1 unsubscribed");
        assertFalse(notifier.isSubscribed(concreteObserver2), "Expected the notifier to have observer2 unsubscribed");
        assertFalse(notifier.isSubscribed(concreteObserver3), "Expected the notifier to have observer3 unsubscribed");
    }

    @Test
    public void testPublishThrowsNoSuchAlgorithmException() {

        when(serviceStatus.isRunning()).thenReturn(true);
        final var notifier = new TestNotifier(mediator, testContext, serviceStatus);
        final var concreteObserver1 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver1, testContext, serviceStatus);

        final var concreteObserver2 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver2, testContext, serviceStatus);

        final var concreteObserver3 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver3, testContext, serviceStatus);

        notifier.subscribe(concreteObserver1);
        notifier.subscribe(concreteObserver2);
        notifier.subscribe(concreteObserver3);

        assertTrue(notifier.isSubscribed(concreteObserver1), "Expected the notifier to have observer1 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver2), "Expected the notifier to have observer2 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver3), "Expected the notifier to have observer3 subscribed");

        List<BlockItem> blockItems = generateBlockItems(1);
        notifier.publish(blockItems);

        final PublishStreamResponse errorResponse = buildErrorStreamResponse();
        verify(streamObserver1, timeout(testTimeout).times(1)).onNext(errorResponse);
        verify(streamObserver2, timeout(testTimeout).times(1)).onNext(errorResponse);
        verify(streamObserver3, timeout(testTimeout).times(1)).onNext(errorResponse);
    }

    @Test
    public void testServiceStatusNotRunning() throws NoSuchAlgorithmException {

        // Set the serviceStatus to not running
        when(serviceStatus.isRunning()).thenReturn(false);
        final var notifier = new TestNotifier(mediator, testContext, serviceStatus);
        final var concreteObserver1 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver1, testContext, serviceStatus);

        final var concreteObserver2 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver2, testContext, serviceStatus);

        final var concreteObserver3 = new ProducerBlockItemObserver(
                testClock, publisher, subscriptionHandler, streamObserver3, testContext, serviceStatus);

        notifier.subscribe(concreteObserver1);
        notifier.subscribe(concreteObserver2);
        notifier.subscribe(concreteObserver3);

        assertTrue(notifier.isSubscribed(concreteObserver1), "Expected the notifier to have observer1 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver2), "Expected the notifier to have observer2 subscribed");
        assertTrue(notifier.isSubscribed(concreteObserver3), "Expected the notifier to have observer3 subscribed");

        final List<BlockItem> blockItems = generateBlockItems(1);
        notifier.publish(blockItems);

        // Verify once the serviceStatus is not running that we do not publish the responses
        final var publishStreamResponse = PublishStreamResponse.newBuilder()
                .acknowledgement(buildAck(blockItems))
                .build();
        verify(streamObserver1, timeout(testTimeout).times(0)).onNext(publishStreamResponse);
        verify(streamObserver2, timeout(testTimeout).times(0)).onNext(publishStreamResponse);
        verify(streamObserver3, timeout(testTimeout).times(0)).onNext(publishStreamResponse);
    }

    private static final class TestNotifier extends NotifierImpl {
        public TestNotifier(
                @NonNull final Notifiable mediator,
                @NonNull final BlockNodeContext blockNodeContext,
                @NonNull final ServiceStatus serviceStatus) {
            super(mediator, blockNodeContext, serviceStatus);
        }

        @Override
        @NonNull
        Acknowledgement buildAck(@NonNull final List<BlockItem> blockItems) throws NoSuchAlgorithmException {
            throw new NoSuchAlgorithmException("Test exception");
        }
    }
}
