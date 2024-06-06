package com.hedera.block.server.persistence.storage;


import com.hedera.block.protos.BlockStreamServiceGrpcProto;

import java.util.Optional;

public interface BlockStorage {
    Optional<Long> write(BlockStreamServiceGrpcProto.Block block);
    Optional<BlockStreamServiceGrpcProto.Block> read(Long blockId);
}
