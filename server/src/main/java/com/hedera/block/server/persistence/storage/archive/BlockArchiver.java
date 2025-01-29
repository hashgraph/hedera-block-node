// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.archive;

// @todo(517) add documentation once we have the final implementation
public interface BlockArchiver {
    void signalBlockWritten(final long currentBlockNumber);

    void stop() throws InterruptedException;
}
