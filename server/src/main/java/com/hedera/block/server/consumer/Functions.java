// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
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

        private final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> nextBlockNodeEventHandler;
        private final ObjectEvent<SubscribeStreamResponseUnparsed> event;
        private final long l;
        private final boolean b;

        // spotless:off
        ProcessOutboundEvent(@NonNull final ObjectEvent<SubscribeStreamResponseUnparsed> event,
             final long l,
             final boolean b,
             @NonNull final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> nextBlockNodeEventHandler) {

            this.event = event;
            this.l = l;
            this.b = b;
            this.nextBlockNodeEventHandler = Objects.requireNonNull(nextBlockNodeEventHandler);
        }
        // spotless:on

        /**
         * {@inheritDoc}
         */
        @Override
        public Void call() throws Exception {
            nextBlockNodeEventHandler.onEvent(event, l, b);
            return null;
        }
    }
}
