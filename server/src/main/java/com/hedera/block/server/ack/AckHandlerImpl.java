// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.ack;

import com.hedera.block.server.block.BlockInfo;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;

/**
 * A simplified AckHandler that:
 *  Creates BlockInfo entries on demand when blockPersisted or blockVerified arrives.
 *  If either skipPersistence or skipVerification is true, ignores all events entirely (no ACKs).
 *  Acks blocks only in strictly increasing order
 *    the ACK is delayed until it is that block's turn.
 *    consecutive ACKs for all blocks that are both persisted and verified.
 */
public class AckHandlerImpl implements AckHandler {

    private final Map<Long, BlockInfo> blockInfo = new ConcurrentHashMap<>();
    private volatile long lastAcknowledgedBlockNumber = -1;
    private final Notifier notifier;
    private final boolean skipAcknowledgement;
    private final ServiceStatus serviceStatus;

    /**
     * Constructor. If either skipPersistence or skipVerification is true,
     * we ignore all events (no ACKs ever sent).
     */
    @Inject
    public AckHandlerImpl(
            @NonNull Notifier notifier, boolean skipAcknowledgement, @NonNull final ServiceStatus serviceStatus) {
        this.notifier = notifier;
        this.skipAcknowledgement = skipAcknowledgement;
        this.serviceStatus = serviceStatus;
    }

    /**
     * Called when we receive a "persistence" event for the given blockNumber.
     * @param blockNumber the block number
     */
    @Override
    public void blockPersisted(long blockNumber) {
        if (skipAcknowledgement) {
            return;
        }

        BlockInfo info = blockInfo.computeIfAbsent(blockNumber, BlockInfo::new);
        info.getBlockStatus().setPersisted();

        attemptAcks();
    }

    /**
     * Called when we receive a "verified" event for the given blockNumber,
     * with the computed blockHash.
     * @param blockNumber the block number
     * @param blockHash the block hash
     */
    @Override
    public void blockVerified(long blockNumber, @NonNull Bytes blockHash) {
        if (skipAcknowledgement) {
            return;
        }

        BlockInfo info = blockInfo.computeIfAbsent(blockNumber, BlockInfo::new);
        info.setBlockHash(blockHash);
        info.getBlockStatus().setVerified();

        attemptAcks();
    }

    /**
     * If the block verification failed, we send an end of stream message to the notifier.
     * @param blockNumber the block number that failed verification
     */
    @Override
    public void blockVerificationFailed(long blockNumber) {
        notifier.sendEndOfStream(blockNumber, PublishStreamResponseCode.STREAM_ITEMS_BAD_STATE_PROOF);
        // TODO We need to notify persistence to delete this block_number.
    }

    /**
     * Attempt to ACK all blocks that are ready to be ACKed.
     * This method is called whenever a block is persisted or verified.
     * It ACKs all blocks in sequence that are both persisted and verified.
     */
    private void attemptAcks() {
        // Temporarily if lastAcknowledgedBlockNumber is -1, we get the first block in the map
        if (lastAcknowledgedBlockNumber == -1) {
            lastAcknowledgedBlockNumber =
                    blockInfo.keySet().stream().min(Long::compareTo).orElse(1L) - 1;
        }

        // Keep ACK-ing starting from the next block in sequence
        while (true) {
            long nextBlock = lastAcknowledgedBlockNumber + 1;
            BlockInfo info = blockInfo.get(nextBlock);

            if (info == null) {
                // We have no info for the next expected block yet.
                // => We can't ACK the "next" block. Stop.
                break;
            }

            // Check if this block is fully ready
            if (!info.getBlockStatus().isPersisted() || !info.getBlockStatus().isVerified()) {
                // Not fully ready. Stop.
                break;
            }

            // Attempt to mark ACK sent (CAS-protected to avoid duplicates)
            if (info.getBlockStatus().markAckSentIfNotAlready()) {
                // We "won" the race; we do the actual ACK
                notifier.sendAck(nextBlock, info.getBlockHash(), false);

                // Update last acknowledged
                lastAcknowledgedBlockNumber = nextBlock;

                // Update the service status
                serviceStatus.setLatestAckedBlockNumber(info);

                // Remove from map if desired (so we don't waste memory)
                blockInfo.remove(nextBlock);
            }

            // Loop again in case the next block is also ready.
            // This can ACK multiple consecutive blocks if they are all
            // persisted & verified in order.
        }
    }
}
