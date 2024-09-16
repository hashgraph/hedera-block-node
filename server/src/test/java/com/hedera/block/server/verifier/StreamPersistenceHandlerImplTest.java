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

package com.hedera.block.server.verifier;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlocksVerified;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItems;
import static com.hedera.hapi.block.SubscribeStreamResponseCode.READ_STREAM_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.OneOf;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StreamPersistenceHandlerImplTest {

    @Mock private SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler;

    @Mock private BlockWriter<BlockItem> blockWriter;

    @Mock private Notifier notifier;

    @Mock private BlockNodeContext blockNodeContext;

    @Mock private ServiceStatus serviceStatus;

    @Mock private MetricsService metricsService;

    private static final int testTimeout = 0;

    @Test
    public void testOnEventWhenServiceIsNotRunning() {

        when(blockNodeContext.metricsService()).thenReturn(metricsService);
        when(serviceStatus.isRunning()).thenReturn(false);

        final var streamVerifier =
                new StreamPersistenceHandlerImpl(
                        subscriptionHandler,
                        notifier,
                        blockWriter,
                        blockNodeContext,
                        serviceStatus);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final var subscribeStreamResponse =
                SubscribeStreamResponse.newBuilder().blockItem(blockItems.getFirst()).build();
        final ObjectEvent<SubscribeStreamResponse> event = new ObjectEvent<>();
        event.set(subscribeStreamResponse);

        streamVerifier.onEvent(event, 0, false);

        // Indirectly confirm the branch we're in by verifying
        // these methods were not called.
        verify(notifier, never()).publish(blockItems.getFirst());
        verify(metricsService, never()).get(LiveBlocksVerified);
    }

    @Test
    public void testBlockItemIsNull() {
        when(blockNodeContext.metricsService()).thenReturn(metricsService);
        when(serviceStatus.isRunning()).thenReturn(true);

        final var streamVerifier =
                new StreamPersistenceHandlerImpl(
                        subscriptionHandler,
                        notifier,
                        blockWriter,
                        blockNodeContext,
                        serviceStatus);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final var subscribeStreamResponse =
                spy(SubscribeStreamResponse.newBuilder().blockItem(blockItems.getFirst()).build());

        // Force the block item to be null
        when(subscribeStreamResponse.blockItem()).thenReturn(null);
        final ObjectEvent<SubscribeStreamResponse> event = new ObjectEvent<>();
        event.set(subscribeStreamResponse);

        streamVerifier.onEvent(event, 0, false);

        verify(serviceStatus, timeout(testTimeout).times(1)).stopRunning(any());
        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(any());
        verify(notifier, timeout(testTimeout).times(1)).notifyUnrecoverableError();
    }

    @Test
    public void testSubscribeStreamResponseTypeUnknown() {
        when(blockNodeContext.metricsService()).thenReturn(metricsService);
        when(serviceStatus.isRunning()).thenReturn(true);

        final var streamVerifier =
                new StreamPersistenceHandlerImpl(
                        subscriptionHandler,
                        notifier,
                        blockWriter,
                        blockNodeContext,
                        serviceStatus);

        final List<BlockItem> blockItems = generateBlockItems(1);
        final var subscribeStreamResponse =
                spy(SubscribeStreamResponse.newBuilder().blockItem(blockItems.getFirst()).build());

        // Force the block item to be UNSET
        final OneOf<SubscribeStreamResponse.ResponseOneOfType> illegalOneOf =
                new OneOf<>(SubscribeStreamResponse.ResponseOneOfType.UNSET, null);
        when(subscribeStreamResponse.response()).thenReturn(illegalOneOf);

        final ObjectEvent<SubscribeStreamResponse> event = new ObjectEvent<>();
        event.set(subscribeStreamResponse);

        streamVerifier.onEvent(event, 0, false);

        verify(serviceStatus, timeout(testTimeout).times(1)).stopRunning(any());
        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(any());
        verify(notifier, timeout(testTimeout).times(1)).notifyUnrecoverableError();
    }

    @Test
    public void testSubscribeStreamResponseTypeStatus() {
        when(blockNodeContext.metricsService()).thenReturn(metricsService);
        when(serviceStatus.isRunning()).thenReturn(true);

        final var streamVerifier =
                new StreamPersistenceHandlerImpl(
                        subscriptionHandler,
                        notifier,
                        blockWriter,
                        blockNodeContext,
                        serviceStatus);

        final SubscribeStreamResponse subscribeStreamResponse =
                spy(SubscribeStreamResponse.newBuilder().status(READ_STREAM_SUCCESS).build());
        final ObjectEvent<SubscribeStreamResponse> event = new ObjectEvent<>();
        event.set(subscribeStreamResponse);

        streamVerifier.onEvent(event, 0, false);

        verify(serviceStatus, never()).stopRunning(any());
        verify(subscriptionHandler, never()).unsubscribe(any());
        verify(notifier, never()).notifyUnrecoverableError();
    }
}
