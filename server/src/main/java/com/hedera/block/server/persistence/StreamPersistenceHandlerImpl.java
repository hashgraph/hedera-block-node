// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.StreamPersistenceHandlerError;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.ack.AckHandler;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.exception.BlockStreamProtocolException;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.AsyncBlockWriter;
import com.hedera.block.server.persistence.storage.write.AsyncBlockWriterFactory;
import com.hedera.block.server.persistence.storage.write.BlockPersistenceResult;
import com.hedera.block.server.persistence.storage.write.BlockPersistenceResult.BlockPersistenceStatus;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.BlockItemSetUnparsed;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.OneOf;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TransferQueue;
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
    private static final System.Logger LOGGER = System.getLogger(StreamPersistenceHandlerImpl.class.getName());
    private static final String PROTOCOL_VIOLATION_MESSAGE =
            "Protocol Violation. %s is OneOf type %s but %s is null.\n%s";
    private final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;
    private final Notifier notifier;
    private final MetricsService metricsService;
    private final ServiceStatus serviceStatus;
    private final AckHandler ackHandler;
    private final AsyncBlockWriterFactory asyncBlockWriterFactory;
    private final CompletionService<Void> completionService;
    private TransferQueue<BlockItemUnparsed> currentWriterQueue;

    /**
     * Constructor.
     *
     * @param subscriptionHandler valid, non-null instance of {@link SubscriptionHandler}
     * @param notifier valid, non-null instance of {@link Notifier}
     * @param blockNodeContext valid, non-null instance of {@link BlockNodeContext}
     * @param serviceStatus valid, non-null instance of {@link ServiceStatus}
     * @param ackHandler valid, non-null instance of {@link AckHandler}
     * @param asyncBlockWriterFactory valid, non-null instance of {@link AsyncBlockWriterFactory}
     * @param executor valid, non-null instance of {@link Executor}
     */
    @Inject
    public StreamPersistenceHandlerImpl(
            @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
            @NonNull final Notifier notifier,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final AckHandler ackHandler,
            @NonNull final AsyncBlockWriterFactory asyncBlockWriterFactory,
            @NonNull final Executor executor) {
        this.subscriptionHandler = Objects.requireNonNull(subscriptionHandler);
        this.notifier = Objects.requireNonNull(notifier);
        this.metricsService = blockNodeContext.metricsService();
        this.serviceStatus = Objects.requireNonNull(serviceStatus);
        this.ackHandler = Objects.requireNonNull(ackHandler);
        this.asyncBlockWriterFactory = Objects.requireNonNull(asyncBlockWriterFactory);
        this.completionService = new ExecutorCompletionService<>(Objects.requireNonNull(executor));
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
    public void onEvent(final ObjectEvent<SubscribeStreamResponseUnparsed> event, long l, boolean b) {
        try {
            if (serviceStatus.isRunning()) {
                final SubscribeStreamResponseUnparsed subscribeStreamResponse = event.get();
                final OneOf<SubscribeStreamResponseUnparsed.ResponseOneOfType> oneOfTypeOneOf =
                        subscribeStreamResponse.response();
                switch (oneOfTypeOneOf.kind()) {
                    case BLOCK_ITEMS -> {
                        final BlockItemSetUnparsed blockItemSetUnparsed = subscribeStreamResponse.blockItems();
                        if (blockItemSetUnparsed == null) {
                            final String message = PROTOCOL_VIOLATION_MESSAGE.formatted(
                                    "SubscribeStreamResponse", "BLOCK_ITEM", "block_item", subscribeStreamResponse);
                            throw new BlockStreamProtocolException(message);
                        } else {
                            final List<BlockItemUnparsed> blockItems = blockItemSetUnparsed.blockItems();
                            if (blockItems.isEmpty()) {
                                final String message = "BlockItems list is empty.";
                                throw new BlockStreamProtocolException(message);
                            } else {
                                handleBlockItems(blockItems);
                            }
                        }
                    }
                    case STATUS -> LOGGER.log(DEBUG, "Unexpected received a status message rather than a block item");
                    default -> {
                        final String message = "Unknown response type: " + oneOfTypeOneOf.kind();
                        LOGGER.log(ERROR, message);
                        throw new BlockStreamProtocolException(message);
                    }
                }
            } else {
                LOGGER.log(ERROR, "Service is not running. Block item will not be processed further.");
            }
        } catch (final Exception e) {
            LOGGER.log(ERROR, "Failed to persist BlockItems due to {0}", e);
            teardown();
        }
    }

    @Override
    public void unsubscribe() {
        subscriptionHandler.unsubscribe(this);
    }

    private void handleBlockItems(final List<BlockItemUnparsed> blockItems)
            throws ParseException, BlockStreamProtocolException {
        final BlockItemUnparsed firstItem = blockItems.getFirst();
        if (firstItem.hasBlockHeader()) {
            if (currentWriterQueue != null) {
                // we do not expect to enter here, but if we have, this means that a block header was found
                // before the previous block was completed (no block proof received), the current block is
                // incomplete

                // push the incomplete block to the flag which will signal the async block writer to
                // clean up and return an incomplete block status
                currentWriterQueue.offer(AsyncBlockWriter.INCOMPLETE_BLOCK_FLAG);

                // we need to set the queue to null in case where the first batch does not end with
                // a block proof, we need to keep accepting items in follow-up batches, but not
                // processing them (not pushing them to a queue) until the next block comes along,
                // which will start anew
                currentWriterQueue = null;
            } else {
                final BlockHeader header = BlockHeader.PROTOBUF.parse(firstItem.blockHeader());
                final long blockNumber = header.number();
                if (blockNumber >= 0) {
                    final AsyncBlockWriter writer = asyncBlockWriterFactory.create(blockNumber);
                    currentWriterQueue = writer.getQueue();
                    completionService.submit(writer);
                } else {
                    // we need to notify the ackHandler that the block number is invalid
                    // IMPORTANT: the currentWriterQueue MUST be null after we have
                    // pinged the ack handler with the bad block number status! This must be done
                    // because if the current batch does not end with block proof, we must not
                    // be processing the items (pushing them to a queue) until the next block
                    // comes along, which will start anew. Even if the branching that reaches here
                    // ensures that the queue is null, it is still assigned as an assurance for
                    // future changes that could potentially affect this due to changes in the
                    // branching or other.
                    final BlockPersistenceResult persistenceResult =
                            new BlockPersistenceResult(blockNumber, BlockPersistenceStatus.BAD_BLOCK_NUMBER);
                    LOGGER.log(
                            DEBUG,
                            "Bad Block Number received [%d], publishing Persistence Result: %s"
                                    .formatted(blockNumber, persistenceResult));
                    ackHandler.blockPersisted(persistenceResult);
                    currentWriterQueue = null;
                }
            }
        }
        for (int i = 0; i < blockItems.size() && currentWriterQueue != null; i++) {
            // We need the non-null check because of the bad block number
            // case, we still need to continue processing following block items,
            // but if the first batch with the bad number does not end with a
            // block proof, we need to keep accepting (but not pushing since the
            // queue is null) until we see the proof, and then we can move on to
            // the next block. Also, for the incomplete block flag, we will be
            // setting the queue to null there, for the same reason, in case
            // the first batch does not end with a block proof, to keep accepting
            // items, but not processing them until the next block comes along,
            // which will start anew.
            currentWriterQueue.offer(blockItems.get(i));
        }
        if (blockItems.getLast().hasBlockProof()) {
            currentWriterQueue = null;
        }
        Future<Void> completionResult;
        while ((completionResult = completionService.poll()) != null) {
            handlePersistenceExecution(completionResult);
        }
    }

    private void handlePersistenceExecution(final Future<Void> completionResult) throws BlockStreamProtocolException {
        try {
            if (completionResult.isCancelled()) {
                // @todo(545) submit cancelled to ackHandler when migrated
            } else {
                // we call get here to verify that the task has run to completion
                // we do not expect it to throw an exception, but to publish
                // a meaningful result, if an exception is thrown, it should be
                // either considered a bug or an unhandled exception
                completionResult.get();
            }
        } catch (final ExecutionException e) {
            // we do not expect to enter here, if an exception during execution
            // occurs inside the async block writer, it should publish a sensible
            // result otherwise, it is either a bug or an unhandled case
            throw new BlockStreamProtocolException("Unexpected exception during block persistence.", e);
        } catch (final InterruptedException e) {
            // @todo(545) if we enter here, then the ring buffer thread was
            // interrupted. What shall we do here? How to handle?
            Thread.currentThread().interrupt();
        }
    }

    private void teardown() {
        metricsService.get(StreamPersistenceHandlerError).increment();

        // Trigger the server to stop accepting new requests
        serviceStatus.stopRunning(getClass().getName());

        // Unsubscribe from the mediator to avoid additional onEvent calls.
        unsubscribe();

        // Broadcast the problem to the notifier
        notifier.notifyUnrecoverableError();
    }
}
