// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.hapi.block.BlockItemUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
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

        private static final System.Logger LOGGER = System.getLogger(ProcessOutboundEvent.class.getName());

        private final BlockNodeEventHandler<ObjectEvent<List<BlockItemUnparsed>>> nextBlockNodeEventHandler;
        private final ObjectEvent<List<BlockItemUnparsed>> event;
        private final long l;
        private final boolean b;

        // spotless:off
        ProcessOutboundEvent(@NonNull final ObjectEvent<List<BlockItemUnparsed>> event,
             final long l,
             final boolean b,
             @NonNull final BlockNodeEventHandler<ObjectEvent<List<BlockItemUnparsed>>> nextBlockNodeEventHandler) {

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
            if (event.get() == null) {
                LOGGER.log(ERROR, "BlockItems list is null.");
            } else {
                nextBlockNodeEventHandler.onEvent(event, l, b);
            }

            return null;
        }
    }
}
