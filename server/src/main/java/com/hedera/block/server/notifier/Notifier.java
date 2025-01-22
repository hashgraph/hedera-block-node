// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.notifier;

import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.util.List;

/**
 * Use this interface to combine the contract for streaming block items with the contract to be
 * notified of critical system events.
 */
public interface Notifier extends StreamMediator<List<BlockItemUnparsed>, PublishStreamResponse>, Notifiable {
    void sendAck(long blockNumber, Bytes blockHash, boolean isDuplicated);
}
