package com.hedera.block.server;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;
import io.helidon.webserver.http.HttpRouting;

import java.util.stream.Stream;

/**
 * Main class for the block node server
 */
public class Server {

    private static ServerCalls.ClientStreamingMethod<Stream<BlockStreamServiceGrpcProto.Block>, StreamObserver<BlockStreamServiceGrpcProto.Empty>> clientStreamingMethod;
    private static ServerCalls.ServerStreamingMethod<Stream<BlockStreamServiceGrpcProto.Block>, StreamObserver<BlockStreamServiceGrpcProto.Block>> serverStreamingMethod;

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
                        .service(new BlockStreamService())
                        .clientStream(BlockStreamServiceGrpcProto.getDescriptor(),
                                "BlockStreamGrpc",
                                "StreamSink",
                                clientStreamingMethod)
                        .serverStream(BlockStreamServiceGrpcProto.getDescriptor(),
                                "BlockStreamGrpc",
                                "StreamSource",
                                serverStreamingMethod))
                .build()
                .start();
    }
}
