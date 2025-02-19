// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.generator.itemhandler;

import static java.util.Objects.requireNonNull;

import com.google.protobuf.ByteString;
import com.hedera.hapi.block.stream.output.protoc.BlockHeader;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hederahashgraph.api.proto.java.BlockHashAlgorithm;
import edu.umd.cs.findbugs.annotations.NonNull;

public class BlockHeaderHandler extends AbstractBlockItemHandler {
    private final byte[] previousBlockHash;
    private final long currentBlockNumber;

    public BlockHeaderHandler(@NonNull final byte[] previousBlockHash, final long currentBlockNumber) {
        this.previousBlockHash = requireNonNull(previousBlockHash);
        this.currentBlockNumber = currentBlockNumber;
    }

    @Override
    public BlockItem getItem() {
        if (blockItem == null) {
            blockItem =
                    BlockItem.newBuilder().setBlockHeader(createBlockHeader()).build();
        }
        return blockItem;
    }

    private BlockHeader createBlockHeader() {
        return BlockHeader.newBuilder()
                .setHapiProtoVersion(getSemanticVersion())
                .setSoftwareVersion(getSemanticVersion())
                .setHashAlgorithm(BlockHashAlgorithm.SHA2_384)
                .setFirstTransactionConsensusTime(getTimestamp())
                .setPreviousBlockHash(ByteString.copyFrom(previousBlockHash))
                .setNumber(currentBlockNumber)
                .build();
    }
}
