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

package com.hedera.block.server.verification;

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.StreamPersistenceHandlerError;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.exception.BlockStreamProtocolException;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.OneOf;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamVerificationHandlerImpl
        implements BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;
    private final MetricsService metricsService;
    private final ServiceStatus serviceStatus;
    private final BlockHashService blockHashService;

    private static final String PROTOCOL_VIOLATION_MESSAGE =
            "Protocol Violation. %s is OneOf type %s but %s is null.\n%s";

    @Inject
    public StreamVerificationHandlerImpl(
            @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
            @NonNull final MetricsService metricsService,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final BlockHashService blockHashService) {

        this.subscriptionHandler = subscriptionHandler;
        this.metricsService = metricsService;
        this.serviceStatus = serviceStatus;
        this.blockHashService = blockHashService;
    }

    @Override
    public void onEvent(ObjectEvent<SubscribeStreamResponseUnparsed> event, long l, boolean b) throws Exception {
        if (!serviceStatus.isRunning()) {
            LOGGER.log(ERROR, "Service is not running. Block item will not be processed further.");
        }

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
                    List<BlockItemUnparsed> blockItems =
                            subscribeStreamResponse.blockItems().blockItems();
                    blockHashService.onBlockItemsReceived(blockItems);
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
    }
}
