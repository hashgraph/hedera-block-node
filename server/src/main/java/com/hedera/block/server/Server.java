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

import static com.hedera.block.server.Constants.APPLICATION_PROPERTIES;
import static com.hedera.block.server.Constants.LOGGING_PROPERTIES;
import static io.helidon.config.ConfigSources.file;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;

import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SimpleConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import io.helidon.config.Config;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/** Main class for the block node server */
public class Server {

    private static final Logger LOGGER = System.getLogger(Server.class.getName());

    private Server() {}

    /**
     * Main entrypoint for the block node server
     *
     * @param args Command line arguments. Not used at present.
     * @throws IOException if there is an error starting the server
     */
    public static void main(final String[] args) throws IOException {
        LOGGER.log(INFO, "Starting BlockNode Server");

        // Set the global configuration
        final Config config =
                Config.builder()
                        .sources(file(Paths.get("/app", LOGGING_PROPERTIES)).optional())
                        .build();

        Config.global(config);

        // Init BlockNode Configuration
        ConfigurationBuilder builder =
                ConfigurationBuilder.create()
                        .withSource(SystemEnvironmentConfigSource.getInstance())
                        .withSource(SystemPropertiesConfigSource.getInstance())
                        .withSources(new ClasspathFileConfigSource(Path.of(APPLICATION_PROPERTIES)))
                        .autoDiscoverExtensions();

        Map<String, String> configArgs = parseArgs(args);
        if (!configArgs.isEmpty()) {
            builder.withSource(new SimpleConfigSource(configArgs));
        }

        final Configuration configuration = builder.build();

        // Init Dagger DI Component, passing in the configuration.
        // this is where all the dependencies are wired up (magic happens)
        final BlockNodeAppInjectionComponent daggerComponent =
                DaggerBlockNodeAppInjectionComponent.factory().create(configuration);

        // Use Dagger DI Component to start the BlockNodeApp with all wired dependencies
        final BlockNodeApp blockNodeApp = daggerComponent.getBlockNodeApp();
        blockNodeApp.start();
    }

    private static Map<String, String> parseArgs(final String[] args) {
        final Map<String, String> configArgs = new HashMap<>();
        for (final String arg : args) {
            final String[] split = arg.split("=");
            if (split.length == 2) {
                configArgs.put(split[0], split[1]);
            }
        }

        return configArgs;
    }
}
