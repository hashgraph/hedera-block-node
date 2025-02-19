// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.generator.itemhandler;

import com.hedera.hapi.block.stream.input.protoc.EventHeader;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hedera.hapi.platform.event.legacy.EventCore;

public class EventHeaderHandler extends AbstractBlockItemHandler {
    @Override
    public BlockItem getItem() {
        if (blockItem == null) {
            blockItem =
                    BlockItem.newBuilder().setEventHeader(createEventHeader()).build();
        }
        return blockItem;
    }

    private EventHeader createEventHeader() {
        return EventHeader.newBuilder().setEventCore(createEventCore()).build();
    }

    private EventCore createEventCore() {
        return EventCore.newBuilder()
                .setCreatorNodeId(generateRandomValue(1, 32))
                .setVersion(getSemanticVersion())
                .build();
    }
}
