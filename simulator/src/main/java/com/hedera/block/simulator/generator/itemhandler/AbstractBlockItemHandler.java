// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.generator.itemhandler;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.hederahashgraph.api.proto.java.SemanticVersion;
import com.hederahashgraph.api.proto.java.Timestamp;
import java.util.Random;

abstract class AbstractBlockItemHandler implements ItemHandler {
    protected BlockItem blockItem;

    public BlockItem getItem() {
        return blockItem;
    }

    public BlockItemUnparsed unparseBlockItem() throws BlockSimulatorParsingException {
        try {
            return BlockItemUnparsed.PROTOBUF.parse(Bytes.wrap(getItem().toByteArray()));
        } catch (ParseException e) {
            throw new BlockSimulatorParsingException(e);
        }
    }

    protected Timestamp getTimestamp() {
        return Timestamp.newBuilder()
                .setSeconds(System.currentTimeMillis() / 1000)
                .build();
    }

    protected SemanticVersion getSemanticVersion() {
        return SemanticVersion.newBuilder().setMajor(0).setMinor(1).setPatch(0).build();
    }

    protected long generateRandomValue(long min, long max) {
        Preconditions.requirePositive(min);
        Preconditions.requirePositive(max);

        return new Random().nextLong(min, max);
    }
}
