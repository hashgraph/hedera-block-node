package com.hedera.block.server;

import com.google.protobuf.Descriptors;
import com.hedera.block.protos.EchoServiceGrpcProto;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.grpc.GrpcService;

import java.util.logging.Logger;

import static io.helidon.webserver.grpc.ResponseHelper.complete;

public class EchoService implements GrpcService {

    Logger logger = Logger.getLogger(EchoService.class.getName());

    @Override
    public Descriptors.FileDescriptor proto() {
        return EchoServiceGrpcProto.getDescriptor();
    }

    @Override
    public void update(Routing routing) {
        routing.unary("Echo", this::echo);
    }

    /**
     * Echo the message back to the caller.
     *
     * @param request  the echo request containing the message to echo
     * @param observer the response observer
     */
    public void echo(EchoServiceGrpcProto.EchoRequest request, StreamObserver<EchoServiceGrpcProto.EchoResponse> observer) {
        String message = request.getMessage();
        logger.info("EchoService grpc request: " + message);
        EchoServiceGrpcProto.EchoResponse response = EchoServiceGrpcProto.EchoResponse.newBuilder().setMessage(message).build();
        complete(observer, response);
    }
}
