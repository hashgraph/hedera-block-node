// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.generator.itemhandler;

import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.protoc.BlockItem;

public interface ItemHandler {
    BlockItem getItem();

    BlockItemUnparsed unparseBlockItem() throws BlockSimulatorParsingException;
}
