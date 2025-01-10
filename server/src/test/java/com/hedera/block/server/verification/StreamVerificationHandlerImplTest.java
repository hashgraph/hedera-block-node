// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.verification;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.VerificationBlocksError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.verification.service.BlockVerificationService;
import com.hedera.hapi.block.BlockItemSetUnparsed;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.OneOf;
import com.hedera.pbj.runtime.ParseException;
import com.swirlds.metrics.api.Counter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StreamVerificationHandlerImplTest {

    @Mock
    private SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;

    @Mock
    private Notifier notifier;

    @Mock
    private MetricsService metricsService;

    @Mock
    private Counter verificationBlocksError;

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private BlockVerificationService blockVerificationService;

    private static final int testTimeout = 50;

    @Test
    public void testOnEventWhenServiceIsNotRunning() {
        when(serviceStatus.isRunning()).thenReturn(false);

        final var streamVerificationHandler = new StreamVerificationHandlerImpl(
                subscriptionHandler, notifier, metricsService, serviceStatus, blockVerificationService);

        final ObjectEvent<SubscribeStreamResponseUnparsed> event = new ObjectEvent<>();
        final var response = SubscribeStreamResponseUnparsed.newBuilder().build(); // no block items, no status
        event.set(response);

        // Call the handler
        streamVerificationHandler.onEvent(event, 0, false);

        verify(serviceStatus, timeout(testTimeout).times(0)).stopRunning(any());
        verify(subscriptionHandler, timeout(testTimeout).times(0)).unsubscribe(any());
        verify(notifier, timeout(testTimeout).times(0)).notifyUnrecoverableError();
    }

    @Test
    public void testBlockItemsNullThrowsException() {
        when(metricsService.get(VerificationBlocksError)).thenReturn(verificationBlocksError);

        when(serviceStatus.isRunning()).thenReturn(true);

        final var streamVerificationHandler = new StreamVerificationHandlerImpl(
                subscriptionHandler, notifier, metricsService, serviceStatus, blockVerificationService);

        // Create a SubscribeStreamResponseUnparsed with BLOCK_ITEMS type but null blockItems
        final SubscribeStreamResponseUnparsed response = spy(SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems((BlockItemSetUnparsed) null)
                .build());
        final ObjectEvent<SubscribeStreamResponseUnparsed> event = new ObjectEvent<>();
        event.set(response);

        // Ensure that the response kind is BLOCK_ITEMS but blockItems is null
        final OneOf<SubscribeStreamResponseUnparsed.ResponseOneOfType> blockItemsOneOf =
                new OneOf<>(SubscribeStreamResponseUnparsed.ResponseOneOfType.BLOCK_ITEMS, null);
        when(response.response()).thenReturn(blockItemsOneOf);

        // Trigger the event
        streamVerificationHandler.onEvent(event, 0, false);

        // We expect a protocol exception, leading to service stop and unsubscribing
        verify(metricsService, timeout(testTimeout).times(1)).get(VerificationBlocksError);
        verify(serviceStatus, timeout(testTimeout).times(1)).stopRunning(any());
        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(any());
        verify(notifier, timeout(testTimeout).times(1)).notifyUnrecoverableError();
        // verify(blockVerificationService, never()).onBlockItemsReceived(any());
    }

    @Test
    public void testUnknownResponseTypeThrowsException() {
        when(serviceStatus.isRunning()).thenReturn(true);

        final var streamVerificationHandler = new StreamVerificationHandlerImpl(
                subscriptionHandler, notifier, metricsService, serviceStatus, blockVerificationService);

        final SubscribeStreamResponseUnparsed response =
                spy(SubscribeStreamResponseUnparsed.newBuilder().build());
        final ObjectEvent<SubscribeStreamResponseUnparsed> event = new ObjectEvent<>();
        event.set(response);

        // Force an UNKNOWN/UNSET oneOf type
        final OneOf<SubscribeStreamResponseUnparsed.ResponseOneOfType> unknownOneOf =
                new OneOf<>(SubscribeStreamResponseUnparsed.ResponseOneOfType.UNSET, null);
        when(response.response()).thenReturn(unknownOneOf);

        streamVerificationHandler.onEvent(event, 0, false);

        verify(metricsService, timeout(testTimeout).times(1)).get(VerificationBlocksError);
        verify(serviceStatus, timeout(testTimeout).times(1)).stopRunning(any());
        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(any());
        verify(notifier, timeout(testTimeout).times(1)).notifyUnrecoverableError();
        // verify(blockVerificationService, never()).onBlockItemsReceived(any());
    }

    @Test
    public void testStatusMessageDoesNotThrow() {
        when(serviceStatus.isRunning()).thenReturn(true);

        final var streamVerificationHandler = new StreamVerificationHandlerImpl(
                subscriptionHandler, notifier, metricsService, serviceStatus, blockVerificationService);

        // Create a subscribeStreamResponse with a STATUS type
        final SubscribeStreamResponseUnparsed response = SubscribeStreamResponseUnparsed.newBuilder()
                .status(com.hedera.hapi.block.SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                .build();
        final ObjectEvent<SubscribeStreamResponseUnparsed> event = new ObjectEvent<>();
        event.set(response);

        streamVerificationHandler.onEvent(event, 0, false);

        verify(serviceStatus, never()).stopRunning(any());
        verify(subscriptionHandler, never()).unsubscribe(any());
        verify(notifier, never()).notifyUnrecoverableError();
        // verify(blockVerificationService, never()).onBlockItemsReceived(any());
    }

    @Test
    public void testValidBlockItemsAreVerified() throws ParseException {
        when(serviceStatus.isRunning()).thenReturn(true);

        final var streamVerificationHandler = new StreamVerificationHandlerImpl(
                subscriptionHandler, notifier, metricsService, serviceStatus, blockVerificationService);

        BlockHeader blockHeader = BlockHeader.newBuilder().number(10).build();

        // Create a valid blockItems response
        List<BlockItemUnparsed> blockItems = List.of(BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(blockHeader))
                .build());

        final BlockItemSetUnparsed blockItemSet =
                BlockItemSetUnparsed.newBuilder().blockItems(blockItems).build();
        final SubscribeStreamResponseUnparsed response = SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(blockItemSet)
                .build();
        final ObjectEvent<SubscribeStreamResponseUnparsed> event = new ObjectEvent<>();
        event.set(response);

        streamVerificationHandler.onEvent(event, 0, false);

        verify(blockVerificationService, times(1)).onBlockItemsReceived(blockItems);
        verify(serviceStatus, never()).stopRunning(any());
        verify(subscriptionHandler, never()).unsubscribe(any());
        verify(notifier, never()).notifyUnrecoverableError();
    }

    @Test
    public void testExceptionInVerificationTriggersErrorResponse() throws ParseException {
        when(serviceStatus.isRunning()).thenReturn(true);

        final var streamVerificationHandler = new StreamVerificationHandlerImpl(
                subscriptionHandler, notifier, metricsService, serviceStatus, blockVerificationService);

        BlockHeader blockHeader = BlockHeader.newBuilder().number(10).build();

        // Create a valid blockItems response
        List<BlockItemUnparsed> blockItems = List.of(BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(blockHeader))
                .build());

        final BlockItemSetUnparsed blockItemSet =
                BlockItemSetUnparsed.newBuilder().blockItems(blockItems).build();
        final SubscribeStreamResponseUnparsed response = SubscribeStreamResponseUnparsed.newBuilder()
                .blockItems(blockItemSet)
                .build();
        final ObjectEvent<SubscribeStreamResponseUnparsed> event = new ObjectEvent<>();
        event.set(response);

        // Simulate an exception when verifying block items
        doThrow(new RuntimeException("Verification failed"))
                .when(blockVerificationService)
                .onBlockItemsReceived(blockItems);

        streamVerificationHandler.onEvent(event, 0, false);

        verify(serviceStatus, timeout(testTimeout).times(1)).stopRunning(any());
        verify(subscriptionHandler, timeout(testTimeout).times(1)).unsubscribe(any());
        verify(notifier, timeout(testTimeout).times(1)).notifyUnrecoverableError();
    }
}
