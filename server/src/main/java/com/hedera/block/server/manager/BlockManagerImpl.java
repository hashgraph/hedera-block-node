// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import com.hedera.block.server.notifier.Notifier;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;

/**
 * A simplified BlockManager that:
 * 1. No longer has a "blockReceived" method.
 * 2. Creates BlockInfo entries on demand when blockPersisted or blockVerified arrives.
 * 3. Has no "stateUpdated" or "finished" concept.
 * 4. If either skipPersistence or skipVerification is true, ignores all events entirely (no ACKs).
 * 5. Acks blocks only in strictly increasing order. If block #n arrives before block #(n-1) is acked,
 *    the ACK is delayed until it is that block's turn. When it *is* the block's turn, we try to send
 *    consecutive ACKs for all blocks that are both persisted and verified.
 */
public class BlockManagerImpl implements BlockManager {

    private final Map<Long, BlockInfo> blockInfoMap = new ConcurrentHashMap<>();
    private volatile long lastAcknowledgedBlockNumber = 0;
    private final Notifier notifier;

    // We do not track "currentBlock" or "lastBlock" unless you specifically need it.
    // If you don't need a "currentBlock" notion, you can remove it from the interface.
    private volatile long currentBlock = -1;

    // Skips
    private final boolean skipPersistence;
    private final boolean skipVerification;

    /**
     * Constructor. If either skipPersistence or skipVerification is true,
     * we ignore all events (no ACKs ever sent).
     */
    @Inject
    public BlockManagerImpl(@NonNull Notifier notifier, boolean skipPersistence, boolean skipVerification) {
        this.notifier = notifier;
        this.skipPersistence = skipPersistence;
        this.skipVerification = skipVerification;
    }

    @Override
    public long currentBlockNumber() {
        // If "currentBlock" is no longer relevant, you can remove this method.
        return currentBlock;
    }

    @Override
    public long lastAcknowledgedBlockNumber() {
        return lastAcknowledgedBlockNumber;
    }

    @Override
    public void blockPersisted(long blockNumber) {
        // If skipping, do nothing
        if (skipPersistence || skipVerification) {
            return;
        }

        // "Create if absent" semantics
        BlockInfo info = blockInfoMap.computeIfAbsent(blockNumber, BlockInfo::new);
        // Mark persisted
        info.getBlockStatus().setPersisted();

        // Try to ACK in sequence if possible
        attemptAcks();
    }

    @Override
    public void blockVerified(long blockNumber, Bytes blockHash) {
        // If skipping, do nothing
        if (skipPersistence || skipVerification) {
            return;
        }

        // "Create if absent" semantics
        BlockInfo info = blockInfoMap.computeIfAbsent(blockNumber, BlockInfo::new);
        // Store the blockHash
        info.setBlockHash(blockHash);
        // Mark verified
        info.getBlockStatus().setVerified();

        // Try to ACK in sequence if possible
        attemptAcks();
    }

    /**
     * Attempt to ACK any blocks starting from (lastAcknowledgedBlockNumber+1) onward,
     * as long as each is both persisted and verified, in strict ascending order.
     */
    private void attemptAcks() {
        while (true) {
            long nextBlock = lastAcknowledgedBlockNumber + 1;
            BlockInfo info = blockInfoMap.get(nextBlock);
            if (info == null) {
                // We don't have that block yet at all
                break;
            }
            BlockStatus status = info.getBlockStatus();
            if (!status.isPersisted() || !status.isVerified()) {
                // It's not fully ready to be ACKed
                break;
            }
            // If we get here, we can ACK it
            notifier.sendAck(nextBlock, info.getBlockHash(), false);

            // Update last acknowledged
            lastAcknowledgedBlockNumber = nextBlock;

            // Remove from map now that we've sent ACK (no "finished" concept anymore)
            blockInfoMap.remove(nextBlock);
        }
    }
}
