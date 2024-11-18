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

package com.hedera.block.server.producer;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProducerBlockItemObserverTest {

    //    @Mock
    //    private InstantSource testClock;
    //
    //    @Mock
    //    private Publisher<List<BlockItem>> publisher;
    //
    //    @Mock
    //    private SubscriptionHandler<PublishStreamResponse> subscriptionHandler;
    //
    //    @Mock
    //    private Pipeline<PublishStreamResponse> publishStreamResponseObserver;
    //
    //    @Mock
    //    private ServiceStatus serviceStatus;
    //
    //    @Mock
    //    private ObjectEvent<PublishStreamResponse> objectEvent;
    //
    //    private final long TIMEOUT_THRESHOLD_MILLIS = 50L;
    //    private static final int testTimeout = 1000;
    //
    //    BlockNodeContext testContext;
    //
    //    @BeforeEach
    //    public void setUp() throws IOException {
    //        this.testContext = TestConfigUtil.getTestBlockNodeContext(
    //                Map.of(TestConfigUtil.CONSUMER_TIMEOUT_THRESHOLD_KEY, String.valueOf(TIMEOUT_THRESHOLD_MILLIS)));
    //    }
    //
    //    @Test
    //    @Disabled
    //    public void testOnError() throws IOException {
    //
    //        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    //        final ProducerBlockItemObserver producerBlockItemObserver = new ProducerBlockItemObserver(
    //                testClock,
    //                publisher,
    //                subscriptionHandler,
    //                publishStreamResponseObserver,
    //                blockNodeContext,
    //                serviceStatus);
    //
    //        final Throwable t = new Throwable("Test error");
    //        producerBlockItemObserver.onError(t);
    //        verify(publishStreamResponseObserver).onError(t);
    //    }
    //
    //    @Test
    //    public void testOnlyErrorStreamResponseAllowedAfterStatusChange() {
    //
    //        final ServiceStatus serviceStatus = new ServiceStatusImpl(testContext);
    //
    //        final ProducerBlockItemObserver producerBlockItemObserver = new ProducerBlockItemObserver(
    //                testClock, publisher, subscriptionHandler, publishStreamResponseObserver, testContext,
    // serviceStatus);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(1);
    //        final PublishStreamRequest publishStreamRequest = PublishStreamRequest.newBuilder()
    //                .blockItems(new BlockItemSet(blockItems))
    //                .build();
    //
    //        // Confirm that the observer is called with the first BlockItem
    //        producerBlockItemObserver.onNext(publishStreamRequest);
    //
    //        // Change the status of the service
    //        serviceStatus.stopRunning(getClass().getName());
    //
    //        // Confirm that the observer is called with the first BlockItem
    //        producerBlockItemObserver.onNext(publishStreamRequest);
    //
    //        // Confirm that closing the observer allowed only 1 response to be sent.
    //        verify(publishStreamResponseObserver, timeout(testTimeout).times(1)).onNext(any());
    //    }
}
