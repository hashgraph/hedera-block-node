// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Use Functions static classes to offset lambda performance penalties.
 */
final class Functions {
    private Functions() {}

    /**
     * ProcessOutboundEvent is a Callable used to stream events asynchronously to
     * a consumer.
     */
    static final class ProcessOutboundEvent implements Callable<Void> {

        private static final Logger LOGGER = System.getLogger(ProcessOutboundEvent.class.getName());

        private final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler;
        private final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> nextBlockNodeEventHandler;
        private final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>>
                asyncConsumerStreamResponseObserver;
        private final ObjectEvent<SubscribeStreamResponseUnparsed> event;
        private final long l;
        private final boolean b;

        // spotless:off
        ProcessOutboundEvent(@NonNull final ObjectEvent<SubscribeStreamResponseUnparsed> event,
             final long l,
             final boolean b,
             @NonNull final SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandler,
             @NonNull final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> asyncConsumerStreamResponseObserver,
             @NonNull final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> nextBlockNodeEventHandler) {

            this.event = event;
            this.l = l;
            this.b = b;
            this.subscriptionHandler = Objects.requireNonNull(subscriptionHandler);
            this.asyncConsumerStreamResponseObserver = Objects.requireNonNull(asyncConsumerStreamResponseObserver);
            this.nextBlockNodeEventHandler = Objects.requireNonNull(nextBlockNodeEventHandler);
        }
        // spotless:on

        /**
         * {@inheritDoc}
         */
        @Override
        public Void call() {
            try {
                nextBlockNodeEventHandler.onEvent(event, l, b);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (UncheckedIOException e) {
                // UncheckedIOException at this layer will almost
                // always be wrapped SocketExceptions from individual
                // clients disconnecting from the server streaming
                // service. This should be happening all the time.
                subscriptionHandler.unsubscribe(asyncConsumerStreamResponseObserver);
                LOGGER.log(
                        DEBUG,
                        "UncheckedIOException caught from Pipeline instance. Unsubscribed consumer observer instance");
            } catch (Exception e) {
                subscriptionHandler.unsubscribe(asyncConsumerStreamResponseObserver);
                LOGGER.log(ERROR, "Exception caught from Pipeline instance. Unsubscribed consumer observer instance.");
                LOGGER.log(ERROR, e.getMessage(), e);
            }
            return null;
        }
    }
}
