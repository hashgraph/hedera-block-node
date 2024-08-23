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

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.config.BlockNodeContextFactory;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.health.HealthService;
import com.hedera.block.server.health.HealthServiceImpl;
import com.hedera.block.server.mediator.LiveStreamMediatorBuilder;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;
import io.helidon.webserver.http.HttpRouting;
import java.io.IOException;

/** Main class for the block node server */
public class Server {

    private static final Logger LOGGER = System.getLogger(Server.class.getName());

    private Server() {}

    /**
     * Main entrypoint for the block node server
     *
     * @param args Command line arguments. Not used at present.
     */
    public static void main(final String[] args) {

        LOGGER.log(INFO, "Starting BlockNode Server");

        try {
            // init context, metrics, and configuration.
            final BlockNodeContext blockNodeContext = BlockNodeContextFactory.create();
            final ServiceStatus serviceStatus = new ServiceStatusImpl();

            final BlockWriter<BlockItem> blockWriter =
                    BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();

            final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> streamMediator =
                    LiveStreamMediatorBuilder.newBuilder(
                                    blockWriter, blockNodeContext, serviceStatus)
                            .build();

            final BlockReader<Block> blockReader =
                    BlockAsDirReaderBuilder.newBuilder(
                                    blockNodeContext
                                            .configuration()
                                            .getConfigData(PersistenceStorageConfig.class))
                            .build();

            final BlockStreamService blockStreamService =
                    buildBlockStreamService(
                            streamMediator, blockReader, serviceStatus, blockNodeContext);

            final GrpcRouting.Builder grpcRouting =
                    GrpcRouting.builder().service(blockStreamService);

            final HealthService healthService = new HealthServiceImpl(serviceStatus);

            final HttpRouting.Builder httpRouting =
                    HttpRouting.builder()
                            .register(healthService.getHealthRootPath(), healthService);

            // Build the web server
            // TODO: make port server a configurable value.
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
            LOGGER.log(INFO, "Block Node Server started at port: " + webServer.port());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static BlockStreamService buildBlockStreamService(
            @NonNull
                    final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>>
                            streamMediator,
            @NonNull final BlockReader<Block> blockReader,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final BlockNodeContext blockNodeContext) {

        return new BlockStreamService(streamMediator, blockReader, serviceStatus, blockNodeContext);
    }
}
