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

import com.hedera.block.server.grpc.BlockAccessService;
import com.hedera.block.server.grpc.BlockStreamService;
import com.hedera.block.server.health.HealthService;
import com.hedera.block.server.service.ServiceStatus;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
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

    private static final Logger LOGGER = System.getLogger(BlockNodeApp.class.getName());

    private final ServiceStatus serviceStatus;
    private final HealthService healthService;
    private final BlockStreamService blockStreamService;
    private final BlockAccessService blockAccessService;
    private final WebServerConfig.Builder webServerBuilder;

    /**
     * Constructs a new BlockNodeApp with the specified dependencies.
     *
     * @param serviceStatus has the status of the service
     * @param healthService handles the health API requests
     * @param blockStreamService handles the block stream requests
     * @param webServerBuilder used to build the web server and start it
     * @param blockAccessService grpc service for block access
     */
    @Inject
    public BlockNodeApp(
            @NonNull ServiceStatus serviceStatus,
            @NonNull HealthService healthService,
            @NonNull BlockStreamService blockStreamService,
            @NonNull WebServerConfig.Builder webServerBuilder,
            @NonNull BlockAccessService blockAccessService) {
        this.serviceStatus = serviceStatus;
        this.healthService = healthService;
        this.blockStreamService = blockStreamService;
        this.webServerBuilder = webServerBuilder;
        this.blockAccessService = blockAccessService;
    }

    /**
     * Starts the server and binds to the specified port.
     *
     * @throws IOException if the server cannot be started
     */
    public void start() throws IOException {

        final GrpcRouting.Builder grpcRouting =
                GrpcRouting.builder().service(blockStreamService).service(blockAccessService);

        final HttpRouting.Builder httpRouting =
                HttpRouting.builder().register(healthService.getHealthRootPath(), healthService);

        // Build the web server
        // TODO: make port server a configurable value.
        final WebServer webServer =
                webServerBuilder.port(8080).addRouting(grpcRouting).addRouting(httpRouting).build();

        // Update the serviceStatus with the web server
        serviceStatus.setWebServer(webServer);

        // Start the web server
        webServer.start();

        // Log the server status
        LOGGER.log(INFO, String.format("Block Node Server started at port: %d", webServer.port()));
    }
}
