/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.block.server;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.mediator.LiveStreamMediatorImpl;
import com.hedera.block.server.persistence.WriteThroughCacheHandler;
import com.hedera.block.server.persistence.storage.BlockStorage;
import com.hedera.block.server.persistence.storage.FileSystemBlockStorage;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;

import java.io.IOException;
import java.util.stream.Stream;

import static com.hedera.block.server.Constants.*;

/**
 * Main class for the block node server
 */
public class Server {

    // Function stubs to satisfy the bidi routing param signatures.  The implementations are in the service class.
    private static ServerCalls.BidiStreamingMethod<Stream<BlockStreamServiceGrpcProto.Block>, StreamObserver<BlockStreamServiceGrpcProto.Block>> clientBidiStreamingMethod;
    private static ServerCalls.BidiStreamingMethod<Stream<BlockStreamServiceGrpcProto.BlockResponse>, StreamObserver<BlockStreamServiceGrpcProto.Block>> serverBidiStreamingMethod;

    private static final System.Logger LOGGER = System.getLogger(Server.class.getName());

    private Server() {}

    /**
     * Main entrypoint for the block node server
     *
     * @param args Command line arguments. Not used at present,
     */
    public static void main(final String[] args) {

        try {

            // Set the global configuration
            final Config config = Config.create();
            Config.global(config);

            // Get Timeout threshold from configuration
            final long consumerTimeoutThreshold = config.get(BLOCKNODE_SERVER_CONSUMER_TIMEOUT_THRESHOLD_KEY).asLong().orElse(1500L);

            // Initialize the block storage, cache, and service
            final BlockStorage<BlockStreamServiceGrpcProto.Block> blockStorage = new FileSystemBlockStorage(BLOCKNODE_STORAGE_ROOT_PATH_KEY, config);

            // TODO: Make timeoutThresholdMillis configurable
            final BlockStreamService blockStreamService = new BlockStreamService(consumerTimeoutThreshold,
                    new LiveStreamMediatorImpl(new WriteThroughCacheHandler(blockStorage)),
                    new WriteThroughCacheHandler(blockStorage));

            // Start the web server
            WebServer.builder()
                    .port(8080)
                    .addRouting(GrpcRouting.builder()
                            .service(blockStreamService)
                            .bidi(BlockStreamServiceGrpcProto.getDescriptor(),
                                    SERVICE_NAME,
                                    CLIENT_STREAMING_METHOD_NAME,
                                    clientBidiStreamingMethod)
                            .bidi(BlockStreamServiceGrpcProto.getDescriptor(),
                                    SERVICE_NAME,
                                    SERVER_STREAMING_METHOD_NAME,
                                    serverBidiStreamingMethod)
                            .unary(BlockStreamServiceGrpcProto.getDescriptor(),
                            "BlockStreamGrpc",
                            "GetBlock",
                            Server::grpcGetBlock))
                    .build()
                    .start();
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "An exception was thrown starting the server", e);
            throw new RuntimeException(e);
        }
    }

    static void grpcGetBlock(BlockStreamServiceGrpcProto.BlockRequest request, StreamObserver<BlockStreamServiceGrpcProto.Block> responseObserver) {}
}
