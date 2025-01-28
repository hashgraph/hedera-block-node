// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.StreamPersistenceHandlerError;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.util.Objects.requireNonNull;

import com.hedera.block.server.ack.AckHandler;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.exception.BlockStreamProtocolException;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.OneOf;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use the StreamPersistenceHandlerImpl to persist live block items passed asynchronously through
 * the LMAX Disruptor
 *
 * <p>This implementation is the primary integration point between the LMAX Disruptor and the file
 * system. The stream persistence handler implements the EventHandler interface so the Disruptor can
 * invoke the onEvent() method when a new SubscribeStreamResponse is available.
 */
@Singleton
public class StreamPersistenceHandlerImpl
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;
    private final BlockWriter<List<BlockItemUnparsed>, Long> blockWriter;
    private final Notifier notifier;
    private final MetricsService metricsService;
    private final ServiceStatus serviceStatus;
    private final AckHandler ackHandler;

    private static final String PROTOCOL_VIOLATION_MESSAGE =
            "Protocol Violation. %s is OneOf type %s but %s is null.\n%s";

    /**
     * Constructs a new StreamPersistenceHandlerImpl instance with the given subscription handler,
     * notifier, block writer,
     *
     * @param subscriptionHandler is used to unsubscribe from the mediator if an error occurs.
     * @param notifier is used to pass successful response messages back to producers and to trigger
     *     error handling in the event of unrecoverable errors.
     * @param blockWriter is used to persist the block items.
     * @param blockNodeContext contains the context with metrics and configuration for the
     *     application.
     * @param serviceStatus is used to stop the service and web server if an exception occurs while
     *     persisting a block item, stop the web server for maintenance, etc.
     */
    @Inject
    public StreamPersistenceHandlerImpl(
            @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
            @NonNull final Notifier notifier,
            @NonNull final BlockWriter<List<BlockItemUnparsed>, Long> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final AckHandler ackHandler) {
        this.subscriptionHandler = requireNonNull(subscriptionHandler);
        this.blockWriter = requireNonNull(blockWriter);
        this.notifier = requireNonNull(notifier);
        this.metricsService = requireNonNull(blockNodeContext.metricsService());
        this.serviceStatus = requireNonNull(serviceStatus);
        this.ackHandler = requireNonNull(ackHandler);
    }

    /**
     * The onEvent method is invoked by the Disruptor when a new SubscribeStreamResponse is
     * available. The method processes the response and persists the block item to the file system.
     *
     * @param event the ObjectEvent containing the SubscribeStreamResponse
     * @param l the sequence number of the event
     * @param b true if the event is the last in the sequence
     */
    @Override
    public void onEvent(ObjectEvent<SubscribeStreamResponseUnparsed> event, long l, boolean b) {
        try {
            if (serviceStatus.isRunning()) {
                final SubscribeStreamResponseUnparsed subscribeStreamResponse = event.get();
                final OneOf<SubscribeStreamResponseUnparsed.ResponseOneOfType> oneOfTypeOneOf =
                        subscribeStreamResponse.response();
                switch (oneOfTypeOneOf.kind()) {
                    case BLOCK_ITEMS -> {
                        if (subscribeStreamResponse.blockItems() == null) {
                            final String message = PROTOCOL_VIOLATION_MESSAGE.formatted(
                                    "SubscribeStreamResponse", "BLOCK_ITEM", "block_item", subscribeStreamResponse);
                            LOGGER.log(ERROR, message);
                            metricsService.get(StreamPersistenceHandlerError).increment();
                            throw new BlockStreamProtocolException(message);
                        } else {
                            // Persist the BlockItems
                            List<BlockItemUnparsed> blockItems =
                                    subscribeStreamResponse.blockItems().blockItems();
                            Optional<Long> result = blockWriter.write(blockItems);
                            // Notify the block manager that the block has been persisted
                            result.ifPresent(ackHandler::blockPersisted);
                        }
                    }
                    case STATUS -> LOGGER.log(DEBUG, "Unexpected received a status message rather than a block item");
                    default -> {
                        final String message = "Unknown response type: " + oneOfTypeOneOf.kind();
                        LOGGER.log(ERROR, message);
                        metricsService.get(StreamPersistenceHandlerError).increment();
                        throw new BlockStreamProtocolException(message);
                    }
                }
            } else {
                LOGGER.log(ERROR, "Service is not running. Block item will not be processed further.");
            }
        } catch (final Exception e) {
            LOGGER.log(ERROR, "Failed to persist BlockItems: ", e);

            metricsService.get(StreamPersistenceHandlerError).increment();

            // Trigger the server to stop accepting new requests
            serviceStatus.stopRunning(getClass().getName());

            // Unsubscribe from the mediator to avoid additional onEvent calls.
            subscriptionHandler.unsubscribe(this);

            // Broadcast the problem to the notifier
            notifier.notifyUnrecoverableError();
        }
    }
}
