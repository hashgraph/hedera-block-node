package com.hedera.block.server.persistence.cache;


import com.hedera.block.protos.BlockStreamServiceGrpcProto;

public interface BlockCache {
    Long insert(BlockStreamServiceGrpcProto.Block block);
    BlockStreamServiceGrpcProto.Block get(Long id);
    boolean contains(Long id);
}
