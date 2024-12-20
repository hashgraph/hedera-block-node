// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.remove;

import java.io.IOException;

/** The BlockRemover interface defines the contract for removing a block from storage. */
public interface BlockRemover {
    /**
     * Remove a block with the given block number.
     *
     * @param blockNumber the block number of the block to remove
     * @throws IOException when failing to remove a block
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    void remove(final long blockNumber) throws IOException;
}
