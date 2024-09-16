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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * use builder methods to create a {@link BlockNodeEventHandler} to validate and persist block
 * items.
 *
 * <p>When a stream validator is created, it will provide access to validate and persist block
 * items.
 */
public class StreamVerifierBuilder {
    private final BlockWriter<BlockItem> blockWriter;
    private final BlockNodeContext blockNodeContext;
    private final ServiceStatus serviceStatus;
    private final Notifier notifier;

    private SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler;

    private StreamVerifierBuilder(
            @NonNull final Notifier notifier,
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        this.notifier = notifier;
        this.blockWriter = blockWriter;
        this.blockNodeContext = blockNodeContext;
        this.serviceStatus = serviceStatus;
    }

    /**
     * Creates a new stream validator builder using the minimum required parameters.
     *
     * @param blockWriter is required to persist block items.
     * @param blockNodeContext is required to provide configuration and metric reporting mechanisms.
     * @param serviceStatus is required to determine the service status and to stop the service if
     *     necessary.
     * @return a stream validator builder configured with required parameters.
     */
    @NonNull
    public static StreamVerifierBuilder newBuilder(
            @NonNull final Notifier notifier,
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        return new StreamVerifierBuilder(notifier, blockWriter, blockNodeContext, serviceStatus);
    }

    /**
     * Optionally, provide a subscription handler to handle the response from the stream.
     *
     * @param subscriptionHandler is required to handle the response from the stream.
     * @return the stream validator builder configured with the subscription handler.
     */
    @NonNull
    public StreamVerifierBuilder subscriptionHandler(
            @NonNull final SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler) {
        this.subscriptionHandler = subscriptionHandler;
        return this;
    }

    @NonNull
    public BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> build() {
        return new StreamVerifierImpl(
                subscriptionHandler, blockWriter, notifier, blockNodeContext, serviceStatus);
    }
}
