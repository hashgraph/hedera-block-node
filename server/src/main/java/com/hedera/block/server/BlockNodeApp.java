// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server;

import static com.hedera.block.server.Constants.PBJ_PROTOCOL_PROVIDER_CONFIG_NAME;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.block.server.config.logging.ConfigurationLogging;
import com.hedera.block.server.health.HealthService;
import com.hedera.block.server.pbj.PbjBlockAccessService;
import com.hedera.block.server.pbj.PbjBlockStreamService;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.pbj.grpc.helidon.PbjRouting;
import com.hedera.pbj.grpc.helidon.config.PbjConfig;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
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
    private final WebServerConfig.Builder webServerBuilder;
    private final PbjBlockStreamService pbjBlockStreamService;
    private final PbjBlockAccessService pbjBlockAccessService;
    private final ServerConfig serverConfig;
    private final ConfigurationLogging configurationLogging;

    /**
     * Constructs a new BlockNodeApp with the specified dependencies.
     *
     * @param serviceStatus has the status of the service
     * @param healthService handles the health API requests
     * @param pbjBlockStreamService defines the Block Stream services
     * @param pbjBlockAccessService defines the Block Access services
     * @param webServerBuilder used to build the web server and start it
     * @param serverConfig has the server configuration
     */
    @Inject
    public BlockNodeApp(
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final HealthService healthService,
            @NonNull final PbjBlockStreamService pbjBlockStreamService,
            @NonNull final PbjBlockAccessService pbjBlockAccessService,
            @NonNull final WebServerConfig.Builder webServerBuilder,
            @NonNull final ServerConfig serverConfig,
            @NonNull final ConfigurationLogging configurationLogging) {
        this.serviceStatus = requireNonNull(serviceStatus);
        this.healthService = requireNonNull(healthService);
        this.pbjBlockStreamService = requireNonNull(pbjBlockStreamService);
        this.pbjBlockAccessService = requireNonNull(pbjBlockAccessService);
        this.webServerBuilder = requireNonNull(webServerBuilder);
        this.serverConfig = requireNonNull(serverConfig);
        this.configurationLogging = requireNonNull(configurationLogging);
    }

    /**
     * Starts the server and binds to the specified port.
     *
     * @throws IOException if the server cannot be started
     */
    public void start() throws IOException {

        // Log the configuration
        configurationLogging.log();

        final HttpRouting.Builder httpRouting =
                HttpRouting.builder().register(healthService.getHealthRootPath(), healthService);

        final PbjRouting.Builder pbjRouting =
                PbjRouting.builder().service(pbjBlockStreamService).service(pbjBlockAccessService);

        // Override the default message size
        final PbjConfig pbjConfig = PbjConfig.builder()
                .name(PBJ_PROTOCOL_PROVIDER_CONFIG_NAME)
                .maxMessageSizeBytes(serverConfig.maxMessageSizeBytes())
                .build();

        // Build the web server
        final WebServer webServer = webServerBuilder
                .port(serverConfig.port())
                .addProtocol(pbjConfig)
                .addRouting(pbjRouting)
                .addRouting(httpRouting)
                .build();

        // Update the serviceStatus with the web server
        serviceStatus.setWebServer(webServer);

        // Start the web server
        webServer.start();

        // Log the server status
        LOGGER.log(INFO, String.format("Block Node Server started at port: %d", webServer.port()));
    }
}
