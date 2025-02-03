// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.grpc.Pipeline;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.InstantSource;
import java.util.concurrent.CompletionService;

/**
 * LiveStreamEventHandlerBuilder is a factory class for building the event handler chain for
 * streaming block items.
 */
public class LiveStreamEventHandlerBuilder {
    public static BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> build(
            @NonNull final CompletionService<Void> completionService,
            @NonNull final InstantSource producerLivenessClock,
            @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
            @NonNull final Pipeline<? super SubscribeStreamResponseUnparsed> observer,
            @NonNull final BlockNodeContext blockNodeContext) {

        // Set the links forward through the chain
        final var consumerStreamResponseObserver =
                new ConsumerStreamResponseObserver(producerLivenessClock, observer, blockNodeContext);

        final var asyncConsumerStreamResponseObserver = new AsyncConsumerStreamResponseObserver(
                completionService, subscriptionHandler, consumerStreamResponseObserver);

        // Set the link backward to handle unsubscribe events
        consumerStreamResponseObserver.setPrevSubscriptionHandler(asyncConsumerStreamResponseObserver);

        // Return the top level chain reference
        return asyncConsumerStreamResponseObserver;
    }
}
