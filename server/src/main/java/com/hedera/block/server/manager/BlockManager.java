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
}
