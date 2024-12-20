// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.remove;

import com.hedera.block.common.utils.Preconditions;
import java.io.IOException;

/**
 * A Block remover that handles block-as-local-file.
 */
public final class BlockAsLocalFileRemover implements LocalBlockRemover {
    /**
     * Constructor.
     */
    private BlockAsLocalFileRemover() {}

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalFileRemover}.
     *
     * @return a new fully initialized instance of
     * {@link BlockAsLocalFileRemover}
     */
    public static BlockAsLocalFileRemover newInstance() {
        return new BlockAsLocalFileRemover();
    }

    /**
     * Remove a block with the given block number.
     *
     * @param blockNumber the block number of the block to remove
     * @throws IOException when failing to remove a block
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @Override
    public void remove(final long blockNumber) throws IOException {
        Preconditions.requireWhole(blockNumber);
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
