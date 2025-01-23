// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.notifier;

import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.util.List;

/**
 * Use this interface to combine the contract for streaming block items with the contract to be
 * notified of critical system events.
 */
public interface Notifier extends StreamMediator<List<BlockItemUnparsed>, PublishStreamResponse>, Notifiable {
    /**
     * Sends an acknowledgement for the given block number and block hash.
     * @param blockNumber number of the block to ack
     * @param blockHash root hash of the block to ack
     * @param isDuplicated true if the block is a duplicate, false otherwise
     */
    void sendAck(long blockNumber, Bytes blockHash, boolean isDuplicated);

    void sendEndOfStream(long block_number, PublishStreamResponseCode responseCode);
}
