// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.remove;

/**
 * A no-op Block remover.
 */
public final class NoOpBlockRemover implements BlockRemover {
    /**
     * Constructor.
     */
    private NoOpBlockRemover() {}

    /**
     * This method creates and returns a new instance of {@link NoOpBlockRemover}.
     *
     * @return a new, fully initialized instance of {@link NoOpBlockRemover}
     */
    public static NoOpBlockRemover newInstance() {
        return new NoOpBlockRemover();
    }

    /**
     * No-op remover. Does nothing and returns immediately. No preconditions
     * check also.
     */
    @Override
    public void remove(final long blockNumber) {
        // do nothing
    }
}
