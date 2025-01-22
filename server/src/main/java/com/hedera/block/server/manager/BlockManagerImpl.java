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
    private volatile long lastAcknowledgedBlockNumber = 0;
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
        // If skipping, do nothing
        if (skipAcknowledgement) {
            return;
        }

        BlockInfo info = blockInfoMap.computeIfAbsent(blockNumber, BlockInfo::new);
        info.getBlockStatus().setPersisted();

        attemptAcks();
    }

    @Override
    public void blockVerified(long blockNumber, Bytes blockHash) {
        // If skipping, do nothing
        if (skipAcknowledgement) {
            return;
        }

        BlockInfo info = blockInfoMap.computeIfAbsent(blockNumber, BlockInfo::new);
        info.setBlockHash(blockHash);
        info.getBlockStatus().setVerified();

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
            // remove from map we no longer need it.
            blockInfoMap.remove(nextBlock);
        }
    }
}
