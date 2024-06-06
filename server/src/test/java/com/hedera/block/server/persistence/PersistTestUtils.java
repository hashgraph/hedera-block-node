package com.hedera.block.server.persistence;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class PersistTestUtils {

    private PersistTestUtils() {}

    public static List<BlockStreamServiceGrpcProto.Block> generateBlocks(int numOfBlocks) {
        return IntStream
                .range(1, numOfBlocks + 1)
                .mapToObj(i -> BlockStreamServiceGrpcProto.Block
                        .newBuilder()
                        .setId(i)
                        .setValue("block-node-" + i).build()
                )
                .collect(Collectors.toList());
    }}
