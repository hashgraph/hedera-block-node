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

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.StreamPersistenceHandlerError;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.exception.BlockStreamProtocolException;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.pbj.runtime.OneOf;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
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
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler;
    private final BlockWriter<Block> blockWriter;
    private final Notifier notifier;
    private final MetricsService metricsService;
    private final ServiceStatus serviceStatus;

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
            @NonNull final SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler,
            @NonNull final Notifier notifier,
            @NonNull final BlockWriter<Block> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        this.subscriptionHandler = subscriptionHandler;
        this.blockWriter = blockWriter;
        this.notifier = notifier;
        this.metricsService = blockNodeContext.metricsService();
        this.serviceStatus = serviceStatus;
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
    public void onEvent(ObjectEvent<SubscribeStreamResponse> event, long l, boolean b) {
        try {
            if (serviceStatus.isRunning()) {

                final SubscribeStreamResponse subscribeStreamResponse = event.get();
                final OneOf<SubscribeStreamResponse.ResponseOneOfType> oneOfTypeOneOf =
                        subscribeStreamResponse.response();
                switch (oneOfTypeOneOf.kind()) {
                    case BLOCK -> {
                        final Block blockItem = subscribeStreamResponse.block();
                        if (blockItem == null) {
                            final String message =
                                    PROTOCOL_VIOLATION_MESSAGE.formatted(
                                            "SubscribeStreamResponse",
                                            "BLOCK_ITEM",
                                            "block_item",
                                            subscribeStreamResponse);
                            LOGGER.log(ERROR, message);
                            metricsService.get(StreamPersistenceHandlerError).increment();
                            throw new BlockStreamProtocolException(message);
                        } else {
                            // Persist the BlockItem
                            Optional<Block> result = blockWriter.write(blockItem);
                            if (result.isPresent()) {
                                // Publish the block item back upstream to the notifier
                                // to send responses to producers.
                                // notifier.publish(blockItem);
                            }
                        }
                    }
                    case STATUS -> LOGGER.log(
                            DEBUG, "Unexpected received a status message rather than a block item");
                    default -> {
                        final String message = "Unknown response type: " + oneOfTypeOneOf.kind();
                        LOGGER.log(ERROR, message);
                        metricsService.get(StreamPersistenceHandlerError).increment();
                        throw new BlockStreamProtocolException(message);
                    }
                }
            } else {
                LOGGER.log(
                        ERROR, "Service is not running. Block item will not be processed further.");
            }

        } catch (BlockStreamProtocolException | IOException e) {

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
