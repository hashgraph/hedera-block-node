// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import com.hedera.pbj.runtime.io.buffer.Bytes;

/**
 * A simplified BlockManager interface that does not have "blockReceived" nor "blockStateUpdated".
 */
public interface BlockManager {

    /**
     * Called when we receive a "persistence" event for the given blockNumber.
     */
    void blockPersisted(long blockNumber);

    /**
     * Called when we receive a "verified" event for the given blockNumber,
     * with the newly computed blockHash.
     */
    void blockVerified(long blockNumber, Bytes blockHash);

    /**
     * @return The highest block number for which an ACK has been sent.
     */
    long lastAcknowledgedBlockNumber();

    /**
     * @return The current block number if there is any notion of such,
     *         or -1 if not used. (Optional, if you do not need it, remove.)
     */
    long currentBlockNumber();
}
