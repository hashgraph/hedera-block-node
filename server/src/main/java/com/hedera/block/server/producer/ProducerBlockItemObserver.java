// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.producer;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlockItemsReceived;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SuccessfulPubStreamRespSent;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

import com.hedera.block.server.block.BlockInfo;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.LivenessCalculator;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.Publisher;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.BlockAcknowledgement;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipeline;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.InstantSource;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The ProducerBlockStreamObserver class plugs into Helidon's server-initiated bidirectional gRPC
 * service implementation. Helidon calls methods on this class as networking events occur with the
 * connection to the upstream producer (e.g. block items streamed from the Consensus Node to the
 * server).
 */
public class ProducerBlockItemObserver
        implements Pipeline<List<BlockItemUnparsed>>, BlockNodeEventHandler<ObjectEvent<PublishStreamResponse>> {

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final SubscriptionHandler<PublishStreamResponse> subscriptionHandler;
    private final Publisher<List<BlockItemUnparsed>> publisher;
    private final ServiceStatus serviceStatus;
    private final MetricsService metricsService;
    private final Flow.Subscriber<? super PublishStreamResponse> publishStreamResponseObserver;

    private final AtomicBoolean isResponsePermitted = new AtomicBoolean(true);

    private final LivenessCalculator livenessCalculator;

    private boolean skipCurrentBlock = false;

    /**
     * Constructor for the ProducerBlockStreamObserver class. It is responsible for calling the
     * mediator with blocks as they arrive from the upstream producer. It also sends responses back
     * to the upstream producer via the responseStreamObserver.
     *
     * @param producerLivenessClock the clock used to calculate the producer liveness.
     * @param publisher the block item list publisher to used to pass block item lists to consumers
     *     as they arrive from the upstream producer.
     * @param subscriptionHandler the subscription handler used to
     * @param publishStreamResponseObserver the response stream observer to send responses back to
     *     the upstream producer for each block item processed.
     * @param blockNodeContext the block node context used to access context objects for the Block
     *     Node (e.g. - the metrics service).
     * @param serviceStatus the service status used to stop the server in the event of an
     *     unrecoverable error.
     */
    public ProducerBlockItemObserver(
            @NonNull final InstantSource producerLivenessClock,
            @NonNull final Publisher<List<BlockItemUnparsed>> publisher,
            @NonNull final SubscriptionHandler<PublishStreamResponse> subscriptionHandler,
            @NonNull final Pipeline<? super PublishStreamResponse> publishStreamResponseObserver,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {

        this.livenessCalculator = new LivenessCalculator(
                producerLivenessClock,
                blockNodeContext
                        .configuration()
                        .getConfigData(ConsumerConfig.class)
                        .timeoutThresholdMillis());

        this.publisher = publisher;
        this.publishStreamResponseObserver = publishStreamResponseObserver;
        this.subscriptionHandler = subscriptionHandler;
        this.metricsService = blockNodeContext.metricsService();
        this.serviceStatus = serviceStatus;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        LOGGER.log(DEBUG, "onSubscribe called");
    }

    /**
     * Helidon triggers this method when it receives a new PublishStreamRequest from the upstream
     * producer. The method publish the block item data to all subscribers via the Publisher and
     * sends a response back to the upstream producer.
     *
     */
    @Override
    public void onNext(@NonNull final List<BlockItemUnparsed> blockItems) {

        LOGGER.log(DEBUG, "Received PublishStreamRequest from producer with " + blockItems.size() + " BlockItems.");
        if (blockItems.isEmpty()) {
            return;
        }

        metricsService.get(LiveBlockItemsReceived).add(blockItems.size());

        // Publish the block to all the subscribers unless
        // there's an issue with the StreamMediator.
        if (serviceStatus.isRunning()) {
            // Refresh the producer liveness
            livenessCalculator.refresh();

            // pre-check for a duplicate block
            if (duplicateBlockPreCheck(blockItems)) {
                // Publish the block to the mediator
                publisher.publish(blockItems);
            }
        } else {
            LOGGER.log(ERROR, getClass().getName() + " is not accepting BlockItems");
            stopProcessing();

            // Close the upstream connection to the producer(s)
            publishStreamResponseObserver.onNext(buildErrorStreamResponse());
            LOGGER.log(ERROR, "Error PublishStreamResponse sent to upstream producer");
        }
    }

    @Override
    public void onEvent(ObjectEvent<PublishStreamResponse> event, long sequence, boolean endOfBatch) {

        if (isResponsePermitted.get()) {
            if (isTimeoutExpired()) {
                stopProcessing();
                LOGGER.log(DEBUG, "Producer liveness timeout. Unsubscribed ProducerBlockItemObserver.");
            } else {
                LOGGER.log(DEBUG, "Publishing response to upstream producer: " + publishStreamResponseObserver);
                publishStreamResponseObserver.onNext(event.get());
                metricsService.get(SuccessfulPubStreamRespSent).increment();
            }
        }
    }

    @NonNull
    private static PublishStreamResponse buildErrorStreamResponse() {
        // TODO: Replace this with a real error enum.
        final EndOfStream endOfStream = EndOfStream.newBuilder()
                .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                .build();
        return PublishStreamResponse.newBuilder().status(endOfStream).build();
    }

    /**
     * Helidon triggers this method when an error occurs on the bidirectional stream to the upstream
     * producer.
     *
     * @param t the error occurred on the stream
     */
    @Override
    public void onError(@NonNull final Throwable t) {
        stopProcessing();
        LOGGER.log(ERROR, "onError method invoked with an exception: ", t);
        LOGGER.log(ERROR, "Producer cancelled the stream. Observer unsubscribed.");
    }

    /**
     * Helidon triggers this method when the bidirectional stream to the upstream producer is
     * completed. Unsubscribe all the observers from the mediator.
     */
    @Override
    public void onComplete() {
        stopProcessing();
        LOGGER.log(DEBUG, "Producer completed the stream. Observer unsubscribed.");
    }

    @Override
    public boolean isTimeoutExpired() {
        return livenessCalculator.isTimeoutExpired();
    }

    @Override
    public void clientEndStreamReceived() {
        stopProcessing();
        LOGGER.log(DEBUG, "Producer cancelled the stream. Observer unsubscribed.");
    }

    private void stopProcessing() {
        isResponsePermitted.set(false);
        subscriptionHandler.unsubscribe(this);
    }

    private boolean duplicateBlockPreCheck(@NonNull final List<BlockItemUnparsed> blockItems) {
        // If there is no block header, just return the opposite of skipCurrentBlock
        BlockItemUnparsed firstItem = blockItems.getFirst();
        if (!firstItem.hasBlockHeader()) {
            return !skipCurrentBlock;
        }

        // Attempt to parse the block header
        long nextBlockNumber;
        try {
            BlockHeader blockHeader = BlockHeader.PROTOBUF.parse(firstItem.blockHeader());
            nextBlockNumber = blockHeader.number();
        } catch (ParseException e) {
            LOGGER.log(ERROR, "Error parsing block header", e);
            throw new RuntimeException(e);
        }

        // Check if this block was already acknowledged
        BlockInfo lastAckedBlockInfo = serviceStatus.getLatestAckedBlockNumber();
        boolean isDuplicateAckedBlock =
                lastAckedBlockInfo != null && nextBlockNumber <= lastAckedBlockInfo.getBlockNumber();

        if (isDuplicateAckedBlock) {
            skipCurrentBlock = true;
            LOGGER.log(
                    WARNING,
                    "Received block number {}, but block number {} has already been acked. "
                            + "Skipping duplicate and sending duplicate ACK",
                    nextBlockNumber,
                    lastAckedBlockInfo.getBlockNumber());
            publishStreamResponseObserver.onNext(buildDuplicateBlockStreamResponse(lastAckedBlockInfo));
        } else {
            skipCurrentBlock = false;
        }

        // Check if this block was already received but not yet fully processed
        boolean isAlreadyReceived =
                !skipCurrentBlock && nextBlockNumber <= serviceStatus.getLatestReceivedBlockNumber();

        if (isAlreadyReceived) {
            LOGGER.log(
                    WARNING,
                    "Received block number {}, but block number {} has already been received. " + "Skipping duplicate",
                    nextBlockNumber,
                    serviceStatus.getLatestReceivedBlockNumber());
            // Create BlockInfo without block hash (indicating partial or unverified persistence).
            BlockInfo blockInfo = new BlockInfo(nextBlockNumber);
            publishStreamResponseObserver.onNext(buildDuplicateBlockStreamResponse(blockInfo));
            skipCurrentBlock = true;
        }

        // Update the latest received block number
        serviceStatus.setLatestReceivedBlockNumber(nextBlockNumber);

        // Return whether we should process this block or skip it
        return !skipCurrentBlock;
    }

    private static PublishStreamResponse buildDuplicateBlockStreamResponse(@NonNull final BlockInfo blockInfo) {
        final BlockAcknowledgement blockAcknowledgement = BlockAcknowledgement.newBuilder()
                .blockAlreadyExists(true)
                .blockNumber(blockInfo.getBlockNumber())
                .blockRootHash(blockInfo.getBlockHash())
                .build();

        return PublishStreamResponse.newBuilder()
                .acknowledgement(Acknowledgement.newBuilder()
                        .blockAck(blockAcknowledgement)
                        .build())
                .build();
    }
}
