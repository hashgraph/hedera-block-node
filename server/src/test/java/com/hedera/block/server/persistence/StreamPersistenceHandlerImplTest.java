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

package com.hedera.block.server.persistence;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StreamPersistenceHandlerImplTest {

    @Mock
    private SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;

    @Mock
    private BlockWriter<List<BlockItemUnparsed>> blockWriter;

    @Mock
    private Notifier notifier;

    @Mock
    private BlockNodeContext blockNodeContext;

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private MetricsService metricsService;

    private static final int testTimeout = 0;

    //    @Test
    //    public void testOnEventWhenServiceIsNotRunning() {
    //
    //        when(blockNodeContext.metricsService()).thenReturn(metricsService);
    //        when(serviceStatus.isRunning()).thenReturn(false);
    //
    //        final var streamPersistenceHandler = new StreamPersistenceHandlerImpl(
    //                subscriptionHandler, notifier, blockWriter, blockNodeContext, serviceStatus);
    //
    //        final List<BlockItemUnparsed> blockItems = generateBlockItems(1);
    //        final BlockItemSetUnparsed blockItemSet =
    //                BlockItemSet.newBuilder().blockItems(blockItems).build();
    //        final var subscribeStreamResponse =
    //                SubscribeStreamResponseUnparsed.newBuilder().blockItems(blockItemSet).build();
    //        final ObjectEvent<SubscribeStreamResponseUnparsed> event = new ObjectEvent<>();
    //        event.set(subscribeStreamResponse);
    //
    //        streamPersistenceHandler.onEvent(event, 0, false);
    //
    //        // Indirectly confirm the branch we're in by verifying
    //        // these methods were not called.
    //        verify(notifier, never()).publish(blockItems);
    //        verify(metricsService, never()).get(StreamPersistenceHandlerError);
    //    }

    //    @Test
    //    public void testBlockItemIsNull() throws IOException {
    //
    //        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    //        when(serviceStatus.isRunning()).thenReturn(true);
    //
    //        final var streamPersistenceHandler = new StreamPersistenceHandlerImpl(
    //                subscriptionHandler, notifier, blockWriter, blockNodeContext, serviceStatus);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(1);
    //        final BlockItemSet blockItemSet =
    //                BlockItemSet.newBuilder().blockItems(blockItems).build();
    //        final var subscribeStreamResponse = spy(
    //                SubscribeStreamResponse.newBuilder().blockItems(blockItemSet).build());
    //
    //        // Force the block item to be null
    //        when(subscribeStreamResponse.blockItems()).thenReturn(null);
    //        final ObjectEvent<SubscribeStreamResponse> event = new ObjectEvent<>();
    //        event.set(subscribeStreamResponse);
    //
    //        streamPersistenceHandler.onEvent(event, 0, false);
    //
    //        verify(serviceStatus, timeout(testTimeout).times(1)).stopRunning(any());
    //        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(any());
    //        verify(notifier, timeout(testTimeout).times(1)).notifyUnrecoverableError();
    //    }

    //    @Test
    //    public void testSubscribeStreamResponseTypeUnknown() throws IOException {
    //        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    //        when(serviceStatus.isRunning()).thenReturn(true);
    //
    //        final var streamPersistenceHandler = new StreamPersistenceHandlerImpl(
    //                subscriptionHandler, notifier, blockWriter, blockNodeContext, serviceStatus);
    //
    //        final List<BlockItem> blockItems = generateBlockItems(1);
    //        final BlockItemSet blockItemSet =
    //                BlockItemSet.newBuilder().blockItems(blockItems).build();
    //        final var subscribeStreamResponse = spy(
    //                SubscribeStreamResponse.newBuilder().blockItems(blockItemSet).build());
    //
    //        // Force the block item to be UNSET
    //        final OneOf<SubscribeStreamResponse.ResponseOneOfType> illegalOneOf =
    //                new OneOf<>(SubscribeStreamResponse.ResponseOneOfType.UNSET, null);
    //        when(subscribeStreamResponse.response()).thenReturn(illegalOneOf);
    //
    //        final ObjectEvent<SubscribeStreamResponse> event = new ObjectEvent<>();
    //        event.set(subscribeStreamResponse);
    //
    //        streamPersistenceHandler.onEvent(event, 0, false);
    //
    //        verify(serviceStatus, timeout(testTimeout).times(1)).stopRunning(any());
    //        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(any());
    //        verify(notifier, timeout(testTimeout).times(1)).notifyUnrecoverableError();
    //    }

    //    @Test
    //    public void testSubscribeStreamResponseTypeStatus() {
    //        when(blockNodeContext.metricsService()).thenReturn(metricsService);
    //        when(serviceStatus.isRunning()).thenReturn(true);
    //
    //        final var streamPersistenceHandler = new StreamPersistenceHandlerImpl(
    //                subscriptionHandler, notifier, blockWriter, blockNodeContext, serviceStatus);
    //
    //        final SubscribeStreamResponse subscribeStreamResponse = spy(
    //                SubscribeStreamResponse.newBuilder().status(READ_STREAM_SUCCESS).build());
    //        final ObjectEvent<SubscribeStreamResponse> event = new ObjectEvent<>();
    //        event.set(subscribeStreamResponse);
    //
    //        streamPersistenceHandler.onEvent(event, 0, false);
    //
    //        verify(serviceStatus, never()).stopRunning(any());
    //        verify(subscriptionHandler, never()).unsubscribe(any());
    //        verify(notifier, never()).notifyUnrecoverableError();
    //    }
}
