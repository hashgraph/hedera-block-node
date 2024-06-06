package com.hedera.block.server;


import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.persistence.BlockPersistenceHandler;
import io.grpc.stub.StreamObserver;

import java.util.logging.Logger;

public class InboundBlockStreamObserver implements StreamObserver<BlockStreamServiceGrpcProto.Block> {

    private final StreamObserver<BlockStreamServiceGrpcProto.BlockResponse> outboundBlockStreamObserver;
    private final BlockPersistenceHandler blockPersistenceHandler;
    private final Logger logger = Logger.getLogger("BlockPersistenceHandler");

    public InboundBlockStreamObserver(StreamObserver<BlockStreamServiceGrpcProto.BlockResponse> outboundBlockStreamObserver,
                                      BlockPersistenceHandler blockPersistenceHandler) {
        this.outboundBlockStreamObserver = outboundBlockStreamObserver;
        this.blockPersistenceHandler = blockPersistenceHandler;
    }

    @Override
    public void onNext(BlockStreamServiceGrpcProto.Block block) {
        logger.fine("onNext called");
        this.blockPersistenceHandler.persist(block);
        BlockStreamServiceGrpcProto.BlockResponse blockResponse = BlockStreamServiceGrpcProto.BlockResponse.newBuilder().setId(block.getId()).build();
        this.outboundBlockStreamObserver.onNext(blockResponse);
    }

    @Override
    public void onError(Throwable t) {
        logger.severe("onError called: " + t.getMessage());
        this.outboundBlockStreamObserver.onError(t);
    }

    @Override
    public void onCompleted() {
        logger.fine("onCompleted called");
        this.outboundBlockStreamObserver.onCompleted();
    }
}
