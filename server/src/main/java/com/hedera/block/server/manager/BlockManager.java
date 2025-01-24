// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import com.hedera.pbj.runtime.io.buffer.Bytes;

/**
 * Interface for managing blocks, their persistence, verification, and failure.
 *
 * Responsible for sending Block Acknowledgements to the producer.
 */
public interface BlockManager {

    /**
     * Called when we receive a "persistence" event for the given blockNumber.
     */
    void blockPersisted(long blockNumber);

    /**
     * Called when we receive a "verified" event for the given blockNumber,
     * with the computed blockHash.
     */
    void blockVerified(long blockNumber, Bytes blockHash);

    /**
     * Called by the Verification Service when we get a verification failure for the given blockNumber.
     */
    void blockVerificationFailed(long blockNumber);
}
