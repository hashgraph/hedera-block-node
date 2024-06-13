package com.hedera.block.server;

import com.hedera.block.protos.EchoServiceGrpcProto;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.grpc.GrpcRouting;

import static io.helidon.webserver.grpc.ResponseHelper.complete;

/**
 * Main class for the block node server
 */
public class Server {
    private Server() {
        // Not meant to be instantiated
    }

    /**
     * Main entrypoint for the block node server
     *
     * @param args Command line arguments. Not used at present,
     */
    public static void main(String[] args) {
        WebServer.builder()
                .port(8080)
                .addRouting(HttpRouting.builder()
                        .get("/greet", (req, res) -> res.send("Hello World!")))
                .addRouting(GrpcRouting.builder()
                        .unary(EchoServiceGrpcProto.getDescriptor(),
                                "EchoService",
                                "Echo",
                                Server::grpcEcho))
                .build()
                .start();
    }

    static void grpcEcho(EchoServiceGrpcProto.EchoRequest request, StreamObserver<EchoServiceGrpcProto.EchoResponse> responseObserver) {
        String requestMessage = request.getMessage();
        System.out.println("grpc request: " + requestMessage);
        complete(responseObserver, EchoServiceGrpcProto.EchoResponse.newBuilder()
                .setMessage(requestMessage).build());
    }
}
