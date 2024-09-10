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

import static com.hedera.block.server.BlockStreamServiceIntegrationTest.buildAck;
import static com.hedera.block.server.Translator.fromPbj;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.BlockNodeEventHandler;
import com.hedera.block.server.mediator.Publisher;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.producer.ProducerBlockItemObserver;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.lmax.disruptor.BatchEventProcessor;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.stub.StreamObserver;
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

    @Mock private Notifiable mediator;
    @Mock private Notifiable blockStreamService;
    @Mock private Publisher<BlockItem> publisher;
    @Mock private ServiceStatus serviceStatus;
    @Mock private SubscriptionHandler<PublishStreamResponse> subscriptionHandler;

    //    @Mock private EventHandler<ObjectEvent<PublishStreamResponse>> observer1;
    //    @Mock private EventHandler<ObjectEvent<PublishStreamResponse>> observer2;
    //    @Mock private EventHandler<ObjectEvent<PublishStreamResponse>> observer3;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse> streamObserver1;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse> streamObserver2;

    @Mock
    private StreamObserver<com.hedera.hapi.block.protoc.PublishStreamResponse> streamObserver3;

    @Mock private InstantSource testClock;

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

        final var notifier =
                NotifierBuilder.newBuilder(mediator, testContext)
                        .blockStreamService(blockStreamService)
                        .build();

        when(testClock.millis()).thenReturn(TEST_TIME, TEST_TIME + TIMEOUT_THRESHOLD_MILLIS);

        final var concreteObserver1 =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        streamObserver1,
                        testContext,
                        serviceStatus);

        final var concreteObserver2 =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        streamObserver2,
                        testContext,
                        serviceStatus);

        final var concreteObserver3 =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        streamObserver3,
                        testContext,
                        serviceStatus);

        notifier.subscribe(concreteObserver1);
        notifier.subscribe(concreteObserver2);
        notifier.subscribe(concreteObserver3);

        assertTrue(
                notifier.isSubscribed(concreteObserver1),
                "Expected the notifier to have observer1 subscribed");
        assertTrue(
                notifier.isSubscribed(concreteObserver2),
                "Expected the notifier to have observer2 subscribed");
        assertTrue(
                notifier.isSubscribed(concreteObserver3),
                "Expected the notifier to have observer3 subscribed");

        List<BlockItem> blockItems = generateBlockItems(1);
        notifier.publish(blockItems.getFirst());

        notifier.unsubscribe(concreteObserver1);
        assertFalse(
                notifier.isSubscribed(concreteObserver1),
                "Expected the notifier to have unsubscribed observer1");

        notifier.unsubscribe(concreteObserver2);
        assertFalse(
                notifier.isSubscribed(concreteObserver2),
                "Expected the notifier to have unsubscribed observer2");

        notifier.unsubscribe(concreteObserver3);
        assertFalse(
                notifier.isSubscribed(concreteObserver3),
                "Expected the notifier to have unsubscribed observer3");

        final var publishStreamResponse =
                PublishStreamResponse.newBuilder()
                        .acknowledgement(buildAck(blockItems.getFirst()))
                        .build();
        verify(streamObserver1, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));
        verify(streamObserver2, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));
        verify(streamObserver3, timeout(testTimeout).times(1))
                .onNext(fromPbj(publishStreamResponse));
    }

    @Test
    public void testTimeoutExpiredHandling() throws InterruptedException {
        final var notifier =
                NotifierBuilder.newBuilder(mediator, testContext)
                        .blockStreamService(blockStreamService)
                        .build();

        // Set the clocks to be expired
        final InstantSource testClock1 = mock(InstantSource.class);
        when(testClock1.millis()).thenReturn(TEST_TIME, TEST_TIME + 1501L);

        final InstantSource testClock2 = mock(InstantSource.class);
        when(testClock2.millis()).thenReturn(TEST_TIME, TEST_TIME + 1501L);

        final InstantSource testClock3 = mock(InstantSource.class);
        when(testClock3.millis()).thenReturn(TEST_TIME, TEST_TIME + 1501L);

        final var concreteObserver1 =
                new ProducerBlockItemObserver(
                        testClock1,
                        publisher,
                        notifier,
                        streamObserver1,
                        testContext,
                        serviceStatus);

        final var concreteObserver2 =
                new ProducerBlockItemObserver(
                        testClock2,
                        publisher,
                        notifier,
                        streamObserver2,
                        testContext,
                        serviceStatus);

        final var concreteObserver3 =
                new ProducerBlockItemObserver(
                        testClock3,
                        publisher,
                        notifier,
                        streamObserver3,
                        testContext,
                        serviceStatus);

        notifier.subscribe(concreteObserver1);
        notifier.subscribe(concreteObserver2);
        notifier.subscribe(concreteObserver3);

        assertTrue(
                notifier.isSubscribed(concreteObserver1),
                "Expected the notifier to have observer1 subscribed");
        assertTrue(
                notifier.isSubscribed(concreteObserver2),
                "Expected the notifier to have observer2 subscribed");
        assertTrue(
                notifier.isSubscribed(concreteObserver3),
                "Expected the notifier to have observer3 subscribed");

        List<BlockItem> blockItems = generateBlockItems(1);
        notifier.publish(blockItems.getFirst());

        Thread.sleep(testTimeout);

        assertFalse(
                notifier.isSubscribed(concreteObserver1),
                "Expected the notifier to have observer1 unsubscribed");
        assertFalse(
                notifier.isSubscribed(concreteObserver2),
                "Expected the notifier to have observer2 unsubscribed");
        assertFalse(
                notifier.isSubscribed(concreteObserver3),
                "Expected the notifier to have observer3 unsubscribed");
    }

    @Test
    public void testPublishThrowsNoSuchAlgorithmException() {
        final var notifier =
                new TestNotifier(new HashMap<>(), blockStreamService, mediator, testContext);
        final var concreteObserver1 =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        streamObserver1,
                        testContext,
                        serviceStatus);

        final var concreteObserver2 =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        streamObserver2,
                        testContext,
                        serviceStatus);

        final var concreteObserver3 =
                new ProducerBlockItemObserver(
                        testClock,
                        publisher,
                        subscriptionHandler,
                        streamObserver3,
                        testContext,
                        serviceStatus);

        notifier.subscribe(concreteObserver1);
        notifier.subscribe(concreteObserver2);
        notifier.subscribe(concreteObserver3);

        assertTrue(
                notifier.isSubscribed(concreteObserver1),
                "Expected the notifier to have observer1 subscribed");
        assertTrue(
                notifier.isSubscribed(concreteObserver2),
                "Expected the notifier to have observer2 subscribed");
        assertTrue(
                notifier.isSubscribed(concreteObserver3),
                "Expected the notifier to have observer3 subscribed");

        List<BlockItem> blockItems = generateBlockItems(1);
        notifier.publish(blockItems.getFirst());

        final PublishStreamResponse errorResponse = TestNotifier.buildErrorStreamResponse();
        verify(streamObserver1, timeout(testTimeout).times(1)).onNext(fromPbj(errorResponse));
        verify(streamObserver2, timeout(testTimeout).times(1)).onNext(fromPbj(errorResponse));
        verify(streamObserver3, timeout(testTimeout).times(1)).onNext(fromPbj(errorResponse));
    }

    private static final class TestNotifier extends NotifierImpl {
        public TestNotifier(
                @NonNull
                        final Map<
                                        BlockNodeEventHandler<ObjectEvent<PublishStreamResponse>>,
                                        BatchEventProcessor<ObjectEvent<PublishStreamResponse>>>
                                subscribers,
                Notifiable blockStreamService,
                Notifiable mediator,
                BlockNodeContext blockNodeContext) {
            super(subscribers, blockStreamService, mediator, blockNodeContext);
        }

        @Override
        @NonNull
        Acknowledgement buildAck(@NonNull final BlockItem blockItem)
                throws NoSuchAlgorithmException {
            throw new NoSuchAlgorithmException("Test exception");
        }
    }
}
