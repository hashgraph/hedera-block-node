package com.hedera.block.server.consumer;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;

public class LiveStreamObserver2Impl implements LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> {

    @Override
    public void notify(BlockStreamServiceGrpcProto.Block block) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onNext(BlockStreamServiceGrpcProto.BlockResponse value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onError(Throwable t) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onCompleted() {
        throw new UnsupportedOperationException("Not implemented");
    }
}

