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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.health.HealthService;
import com.hedera.block.server.mediator.LiveStreamMediatorBuilder;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.producer.ItemAckBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;
import io.helidon.webserver.http.HttpRouting;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The main class for the Block Node application. This class is responsible for starting the server
 * and initializing the context.
 */
@Singleton
public class BlockNodeApp {

    private static final System.Logger LOGGER = System.getLogger(Server.class.getName());
    private final ServiceStatus serviceStatus;
    private final HealthService healthService;
    private final BlockNodeContext blockNodeContext;

    /**
     * Has all needed dependencies to start the server and initialize the context.
     *
     * @param serviceStatus the status of the service
     * @param healthService the health service
     */
    @Inject
    public BlockNodeApp(
            @NonNull ServiceStatus serviceStatus,
            @NonNull HealthService healthService,
            @NonNull BlockNodeContext blockNodeContext) {
        this.serviceStatus = serviceStatus;
        this.healthService = healthService;
        this.blockNodeContext = blockNodeContext;
    }

    /**
     * Starts the server and binds to the specified port.
     *
     * @throws IOException if the server cannot be started
     */
    public void startServer() throws IOException {

        @NonNull
        final BlockWriter<com.hedera.block.protos.BlockStreamService.BlockItem> blockWriter =
                BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
        @NonNull
        final StreamMediator<
                        com.hedera.block.protos.BlockStreamService.BlockItem,
                        ObjectEvent<
                                com.hedera.block.protos.BlockStreamService.SubscribeStreamResponse>>
                streamMediator =
                        LiveStreamMediatorBuilder.newBuilder(
                                        blockWriter, blockNodeContext, serviceStatus)
                                .build();

        @NonNull
        final BlockReader<com.hedera.block.protos.BlockStreamService.Block> blockReader =
                BlockAsDirReaderBuilder.newBuilder(
                                blockNodeContext
                                        .configuration()
                                        .getConfigData(PersistenceStorageConfig.class))
                        .build();

        @NonNull
        final BlockStreamService blockStreamService =
                buildBlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        @NonNull
        final GrpcRouting.Builder grpcRouting = GrpcRouting.builder().service(blockStreamService);

        @NonNull
        final HttpRouting.Builder httpRouting =
                HttpRouting.builder().register(healthService.getHealthRootPath(), healthService);

        // Build the web server
        // TODO: make port server a configurable value.
        @NonNull
        final WebServer webServer =
                WebServer.builder()
                        .port(8080)
                        .addRouting(grpcRouting)
                        .addRouting(httpRouting)
                        .build();

        // Update the serviceStatus with the web server
        serviceStatus.setWebServer(webServer);

        // Start the web server
        webServer.start();

        // Log the server status
        LOGGER.log(
                System.Logger.Level.INFO, "Block Node Server started at port: " + webServer.port());
    }

    @NonNull
    private static BlockStreamService buildBlockStreamService(
            @NonNull
                    final StreamMediator<
                                    com.hedera.block.protos.BlockStreamService.BlockItem,
                                    ObjectEvent<
                                            com.hedera.block.protos.BlockStreamService
                                                    .SubscribeStreamResponse>>
                            streamMediator,
            @NonNull
                    final BlockReader<com.hedera.block.protos.BlockStreamService.Block> blockReader,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final BlockNodeContext blockNodeContext) {

        return new BlockStreamService(
                new ItemAckBuilder(), streamMediator, blockReader, serviceStatus, blockNodeContext);
    }
}
