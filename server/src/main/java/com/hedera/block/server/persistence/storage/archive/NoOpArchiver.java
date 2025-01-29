// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.archive;

/**
 * A no-op implementation of the archiver.
 */
public final class NoOpArchiver implements BlockArchiver {
    /**
     * Constructor.
     */
    private NoOpArchiver() {}

    /**
     * Factory method. Returns a new, fully initialized instance of
     * {@link NoOpArchiver}.
     *
     * @return a new, fully initialized and valid instance of
     * {@link NoOpArchiver}
     */
    public static NoOpArchiver newInstance() {
        return new NoOpArchiver();
    }

    /**
     * This method does nothing, it also has no precondition checks.
     */
    @Override
    public void signalBlockWritten(final long currentBlockNumber) {
        // do nothing
    }

    /**
     * This method does nothing, it also has no precondition checks.
     */
    @Override
    public void stop() throws InterruptedException {
        // do nothing
    }
}
