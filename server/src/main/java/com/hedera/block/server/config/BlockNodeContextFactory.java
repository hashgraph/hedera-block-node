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

package com.hedera.block.server.config;

import com.hedera.block.server.metrics.MetricsService;
import com.swirlds.common.metrics.platform.DefaultMetricsProvider;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import com.swirlds.metrics.api.Metrics;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Use the static method create() to obtain a new {@link BlockNodeContext} when initializing the
 * application.
 *
 * <p>When a context is created all enabled sources of configuration information will be read and
 * applicable values made available through the context created.<br>
 * The application should use the context to obtain configuration and metrics support.
 */
public class BlockNodeContextFactory {

    /**
     * A resource file name from which application configuration is read. The properties in this
     * file are available as configuration from the {@link BlockNodeContext}.
     */
    private static final String APPLICATION_PROPERTIES = "app.properties";

    private BlockNodeContextFactory() {}

    /**
     * Create a new application context for use in the system. The context is needed at
     * initialization.
     *
     * @return a context with configuration and metrics.
     * @throws IOException when the java libraries fail to read information from a configuration
     *     source.
     */
    @NonNull
    public static BlockNodeContext create() throws IOException {
        final Configuration configuration = getConfiguration();
        final Metrics metrics = getMetrics(configuration);
        final MetricsService metricsService = new MetricsService(metrics);
        return new BlockNodeContext(metrics, metricsService, configuration);
    }

    /**
     * Read configuration for this system. The configuration sources will include environment
     * variables, system properties, and the properties file named {@value APPLICATION_PROPERTIES}.
     *
     * @return the configuration as read from environment, properties, and files.
     * @throws IOException when the java libraries fail to read information from a configuration
     *     source.
     */
    @NonNull
    private static Configuration getConfiguration() throws IOException {

        return ConfigurationBuilder.create()
                .withSource(SystemEnvironmentConfigSource.getInstance())
                .withSource(SystemPropertiesConfigSource.getInstance())
                .withSource(new ClasspathFileConfigSource(Path.of(APPLICATION_PROPERTIES)))
                .autoDiscoverExtensions()
                .build();
    }

    @NonNull
    private static Metrics getMetrics(@NonNull final Configuration configuration) {
        final DefaultMetricsProvider metricsProvider = new DefaultMetricsProvider(configuration);
        final Metrics metrics = metricsProvider.createGlobalMetrics();
        metricsProvider.start();
        return metrics;
    }
}
