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

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.LiveBlocksVerified;
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
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.OneOf;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamPersistenceHandlerImpl
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler;
    private final BlockWriter<BlockItem> blockWriter;
    private final Notifier notifier;
    private final MetricsService metricsService;
    private final ServiceStatus serviceStatus;

    private static final String PROTOCOL_VIOLATION_MESSAGE =
            "Protocol Violation. %s is OneOf type %s but %s is null.\n%s";

    @Inject
    public StreamPersistenceHandlerImpl(
            @NonNull final SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler,
            @NonNull final Notifier notifier,
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        this.subscriptionHandler = subscriptionHandler;
        this.blockWriter = blockWriter;
        this.notifier = notifier;
        this.metricsService = blockNodeContext.metricsService();
        this.serviceStatus = serviceStatus;
    }

    @Override
    public void onEvent(
            ObjectEvent<SubscribeStreamResponse> event, long sequence, boolean endOfBatch) {
        try {
            if (serviceStatus.isRunning()) {

                final SubscribeStreamResponse subscribeStreamResponse = event.get();
                final OneOf<SubscribeStreamResponse.ResponseOneOfType> oneOfTypeOneOf =
                        subscribeStreamResponse.response();
                switch (oneOfTypeOneOf.kind()) {
                    case BLOCK_ITEM -> {
                        final BlockItem blockItem = subscribeStreamResponse.blockItem();
                        if (blockItem == null) {
                            final String message =
                                    PROTOCOL_VIOLATION_MESSAGE.formatted(
                                            "SubscribeStreamResponse",
                                            "BLOCK_ITEM",
                                            "block_item",
                                            subscribeStreamResponse);
                            LOGGER.log(ERROR, message);
                            throw new BlockStreamProtocolException(message);
                        } else {
                            // Persist the BlockItem
                            Optional<BlockItem> result = blockWriter.write(blockItem);
                            if (result.isPresent()) {
                                // Publish the block item back upstream to the notifier
                                // to send responses to producers.
                                notifier.publish(blockItem);
                                metricsService.get(LiveBlocksVerified).increment();
                            }
                        }
                    }
                    case STATUS -> LOGGER.log(
                            DEBUG, "Unexpected received a status message rather than a block item");
                    default -> {
                        final String message = "Unknown response type: " + oneOfTypeOneOf.kind();
                        LOGGER.log(ERROR, message);
                        throw new BlockStreamProtocolException(message);
                    }
                }
            } else {
                LOGGER.log(
                        ERROR, "Service is not running. Block item will not be processed further.");
            }

        } catch (BlockStreamProtocolException | IOException e) {

            // Trigger the server to stop accepting new requests
            serviceStatus.stopRunning(getClass().getName());

            // Unsubscribe from the mediator to avoid additional onEvent calls.
            subscriptionHandler.unsubscribe(this);

            // Broadcast the problem to the notifier
            notifier.notifyUnrecoverableError();
        }
    }
}
