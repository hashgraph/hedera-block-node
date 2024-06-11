package com.hedera.block.server.persistence;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;

import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;

public interface BlockPersistenceHandler {
    Long persist(BlockStreamServiceGrpcProto.Block block);
    Optional<BlockStreamServiceGrpcProto.Block> read(Long id);
    Queue<BlockStreamServiceGrpcProto.Block> readFrom(Long id, Predicate<BlockStreamServiceGrpcProto.Block> filter);
}
