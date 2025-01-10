// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.notifier;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SuccessfulPubStreamResp;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Gauge.NotifierRingBufferRemainingCapacity;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Gauge.Producers;
import static com.hedera.block.server.producer.Util.getFakeHash;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.mediator.SubscriptionHandlerBase;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.EndOfStream;
import com.hedera.hapi.block.ItemAcknowledgement;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Use NotifierImpl to mediate the stream of responses from the persistence layer back to multiple
 * producers.
 *
 * <p>As an implementation of the StreamMediator interface, it proxies block item persistence
 * responses back to the producers as they arrive via a RingBuffer maintained in the base class and
 * persists the block items to a store. It also notifies the mediator of critical system events and
 * will stop the server in the event of an unrecoverable error.
 */
@Singleton
public class NotifierImpl extends SubscriptionHandlerBase<PublishStreamResponse> implements Notifier {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    /** The initial capacity of producers in the subscriber map. */
    private static final int SUBSCRIBER_INIT_CAPACITY = 5;

    private final Notifiable mediator;
    private final MetricsService metricsService;
    private final ServiceStatus serviceStatus;

    /**
     * Constructs a new NotifierImpl instance with the given mediator, block node context, and
     * service status.
     *
     * @param mediator the mediator to notify of critical system events
     * @param blockNodeContext the block node context
     * @param serviceStatus the service status to stop the service and web server if an exception
     *     occurs
     */
    @Inject
    public NotifierImpl(
            @NonNull final Notifiable mediator,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {

        super(
                new ConcurrentHashMap<>(SUBSCRIBER_INIT_CAPACITY),
                blockNodeContext.metricsService().get(Producers),
                blockNodeContext
                        .configuration()
                        .getConfigData(NotifierConfig.class)
                        .ringBufferSize());

        this.mediator = mediator;
        this.metricsService = blockNodeContext.metricsService();
        this.serviceStatus = serviceStatus;
    }

    @Override
    public void notifyUnrecoverableError() {

        mediator.notifyUnrecoverableError();

        // Publish an end of stream response to the producers.
        final PublishStreamResponse errorStreamResponse = buildErrorStreamResponse();
        ringBuffer.publishEvent((event, sequence) -> event.set(errorStreamResponse));

        // Stop the server
        serviceStatus.stopWebServer(getClass().getName());
    }

    /**
     * Publishes the given block item to all subscribed producers.
     *
     * @param blockItems the block items from the persistence layer to publish a response to
     *     upstream producers
     */
    @Override
    public void publish(@NonNull List<BlockItemUnparsed> blockItems) {

        try {
            if (serviceStatus.isRunning()) {
                // Publish the block item to the subscribers
                final var publishStreamResponse = PublishStreamResponse.newBuilder()
                        .acknowledgement(buildAck(blockItems))
                        .build();
                ringBuffer.publishEvent((event, sequence) -> event.set(publishStreamResponse));

                metricsService.get(NotifierRingBufferRemainingCapacity).set(ringBuffer.remainingCapacity());
                metricsService.get(SuccessfulPubStreamResp).increment();
            } else {
                LOGGER.log(ERROR, "Notifier is not running.");
            }

        } catch (NoSuchAlgorithmException e) {

            // Stop the server
            serviceStatus.stopRunning(getClass().getName());

            final var errorResponse = buildErrorStreamResponse();
            LOGGER.log(ERROR, "Error calculating hash: ", e);

            // Send an error response to all the producers.
            ringBuffer.publishEvent((event, sequence) -> event.set(errorResponse));
        }
    }

    /**
     * Builds an error stream response.
     *
     * @return the error stream response
     */
    @NonNull
    static PublishStreamResponse buildErrorStreamResponse() {
        // TODO: Replace this with a real error enum.
        final EndOfStream endOfStream = EndOfStream.newBuilder()
                .status(PublishStreamResponseCode.STREAM_ITEMS_UNKNOWN)
                .build();
        return PublishStreamResponse.newBuilder().status(endOfStream).build();
    }

    /**
     * Protected method meant for testing. Builds an Acknowledgement for the block item.
     *
     * @param blockItems the block items to build the Acknowledgement for
     * @return the Acknowledgement for the block item
     * @throws NoSuchAlgorithmException if the hash algorithm is not supported
     */
    @NonNull
    Acknowledgement buildAck(@NonNull final List<BlockItemUnparsed> blockItems) throws NoSuchAlgorithmException {
        final ItemAcknowledgement itemAck = ItemAcknowledgement.newBuilder()
                // TODO: Replace this with a real hash generator
                .itemsHash(Bytes.wrap(getFakeHash(blockItems)))
                .build();

        return Acknowledgement.newBuilder().itemAck(itemAck).build();
    }
}
