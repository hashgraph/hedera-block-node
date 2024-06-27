package com.hedera.block.server;

import com.google.protobuf.Descriptors;
import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;

import java.util.logging.Logger;

import static io.helidon.webserver.grpc.ResponseHelper.complete;


public class BlockStreamService implements GrpcService {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public Descriptors.FileDescriptor proto() {
        return BlockStreamServiceGrpcProto.getDescriptor();
    }

    @Override
    public String serviceName() {
        return "BlockStreamGrpc";
    }

    @Override
    public void update(Routing routing) {
        routing.unary("GetBlock", this::getBlock);
    }

    private void getBlock(BlockStreamServiceGrpcProto.BlockRequest request, StreamObserver<BlockStreamServiceGrpcProto.Block> responseObserver) {
        String message = "GET BLOCK RESPONSE! ";
        logger.info("GetBlock request received");
        BlockStreamServiceGrpcProto.Block response = BlockStreamServiceGrpcProto.Block.newBuilder().setValue(message).build();
        complete(responseObserver, response);
    }


}
