// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import com.hedera.pbj.runtime.io.buffer.Bytes;

/**
 * POJO that holds information about a block while in process.
 */
public class BlockInfo {

    private final long blockNumber;
    private Bytes blockHash;
    private final BlockStatus blockStatus;

    public BlockInfo(long blockNumber) {
        this.blockNumber = blockNumber;
        this.blockStatus = new BlockStatus();
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public Bytes getBlockHash() {
        return blockHash;
    }

    public BlockStatus getBlockStatus() {
        return blockStatus;
    }

    public void setBlockHash(Bytes blockHash) {
        this.blockHash = blockHash;
    }
}
