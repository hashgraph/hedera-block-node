// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * POJO that holds information about a block while in process.
 */
class BlockInfo {

    private final long blockNumber;
    private Bytes blockHash;
    private final BlockStatus blockStatus;

    /**
     * Constructor.
     * @param blockNumber the block number
     */
    public BlockInfo(long blockNumber) {
        this.blockNumber = blockNumber;
        this.blockStatus = new BlockStatus();
    }

    /**
     * Get the block number.
     * @return the block number
     */
    public long getBlockNumber() {
        return blockNumber;
    }

    /**
     * Get the block hash.
     * @return the block hash
     */
    public Bytes getBlockHash() {
        return blockHash;
    }

    /**
     * Get the block status.
     * @return the block status
     */
    public BlockStatus getBlockStatus() {
        return blockStatus;
    }

    /**
     * Set the block hash.
     * @param blockHash the block hash
     */
    public void setBlockHash(@NonNull Bytes blockHash) {
        this.blockHash = blockHash;
    }
}
