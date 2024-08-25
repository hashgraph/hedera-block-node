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

import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import java.io.IOException;
import java.nio.file.Path;

/** Main class for the block node server */
public class Server {

    private static final Logger LOGGER = System.getLogger(Server.class.getName());
    private static final String APPLICATION_PROPERTIES = "app.properties";

    private Server() {}

    /**
     * Main entrypoint for the block node server
     *
     * @param args Command line arguments. Not used at present.
     * @throws IOException if there is an error starting the server
     */
    public static void main(final String[] args) throws IOException {
        LOGGER.log(INFO, "Starting BlockNode Server");

        // Init BlockNode Configuration
        Configuration configuration =
                ConfigurationBuilder.create()
                        .withSource(SystemEnvironmentConfigSource.getInstance())
                        .withSource(SystemPropertiesConfigSource.getInstance())
                        .withSource(new ClasspathFileConfigSource(Path.of(APPLICATION_PROPERTIES)))
                        .autoDiscoverExtensions()
                        .build();

        // Init Dagger DI Component, passing in the configuration.
        // this is where all the dependencies are wired up (magic happens)
        //        final BlockNodeAppInjectionComponent daggerComponent =
        //                DaggerBlockNodeAppInjectionComponent.factory().create(configuration);
        final BlockNodeAppInjectionComponent daggerComponent =
                DaggerBlockNodeAppInjectionComponent.builder().configuration(configuration).build();

        // Use Dagger DI Component to start the BlockNodeApp with all wired dependencies
        final BlockNodeApp blockNodeApp = daggerComponent.getBlockNodeApp();
        blockNodeApp.start();
    }
}
