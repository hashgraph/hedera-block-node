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
 *  Creates BlockInfo entries on demand when blockPersisted or blockVerified arrives.
 *  If either skipPersistence or skipVerification is true, ignores all events entirely (no ACKs).
 *  Acks blocks only in strictly increasing order
 *    the ACK is delayed until it is that block's turn.
 *    consecutive ACKs for all blocks that are both persisted and verified.
 */
public class BlockManagerImpl implements BlockManager {

    private final Map<Long, BlockInfo> blockInfoMap = new ConcurrentHashMap<>();
    private volatile long lastAcknowledgedBlockNumber = -1;
    private final Notifier notifier;
    private final boolean skipAcknowledgement;

    /**
     * Constructor. If either skipPersistence or skipVerification is true,
     * we ignore all events (no ACKs ever sent).
     */
    @Inject
    public BlockManagerImpl(@NonNull Notifier notifier, boolean skipAcknowledgement) {
        this.notifier = notifier;
        this.skipAcknowledgement = skipAcknowledgement;
    }

    @Override
    public void blockPersisted(long blockNumber) {
        if (skipAcknowledgement) {
            return;
        }

        BlockInfo info = blockInfoMap.computeIfAbsent(blockNumber, BlockInfo::new);
        info.getBlockStatus().setPersisted();

        attemptAcks();
    }

    @Override
    public void blockVerified(long blockNumber, Bytes blockHash) {
        if (skipAcknowledgement) {
            return;
        }

        BlockInfo info = blockInfoMap.computeIfAbsent(blockNumber, BlockInfo::new);
        info.setBlockHash(blockHash);
        info.getBlockStatus().setVerified();

        attemptAcks();
    }

    private void attemptAcks() {
        // Temporarily if lastAcknowledgedBlockNumber is -1, we get the first block in the map
        if (lastAcknowledgedBlockNumber == -1) {
            lastAcknowledgedBlockNumber = blockInfoMap.keySet().stream().min(Long::compareTo).orElse(-1L);
        }

        // Keep ACK-ing starting from the next block in sequence
        while (true) {
            long nextBlock = lastAcknowledgedBlockNumber + 1;
            BlockInfo info = blockInfoMap.get(nextBlock);

            if (info == null) {
                // We have no info for the next expected block yet.
                // => We can't ACK the "next" block. Stop.
                break;
            }

            // Check if this block is fully ready
            if (!info.getBlockStatus().isPersisted() ||
                    !info.getBlockStatus().isVerified()) {
                // Not fully ready. Stop.
                break;
            }

            // Attempt to mark ACK sent (CAS-protected to avoid duplicates)
            if (info.getBlockStatus().markAckSentIfNotAlready()) {
                // We "won" the race; we do the actual ACK
                notifier.sendAck(nextBlock, info.getBlockHash(), false);

                // Update last acknowledged
                lastAcknowledgedBlockNumber = nextBlock;

                // Remove from map if desired (so we don't waste memory)
                blockInfoMap.remove(nextBlock);
            }

            // Loop again in case the next block is also ready.
            // This can ACK multiple consecutive blocks if they are all
            // persisted & verified in order.
        }
    }

}
