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

/** Static factory that creates {@link BlockNodeContext} */
public class BlockNodeContextFactory {

    private static final String APPLICATION_PROPERTIES_1 = "app.properties";

    private BlockNodeContextFactory() {}

    /**
     * Use the create method to build a singleton block node context to manage system-wide metrics.
     *
     * @return an instance of {@link BlockNodeContext} which holds {@link Configuration}, {@link
     *     Metrics} and {@link MetricsService} for the rest of the application to use.
     * @throws IOException when the java libraries fail to read information from a configuration
     *     source.
     */
    public static BlockNodeContext create() throws IOException {
        final Configuration configuration = getConfiguration();
        final Metrics metrics = getMetrics(configuration);
        final MetricsService metricsService = new MetricsService(metrics);
        return new BlockNodeContext(metrics, metricsService, configuration);
    }

    private static Configuration getConfiguration() throws IOException {

        return ConfigurationBuilder.create()
                .withSource(SystemEnvironmentConfigSource.getInstance())
                .withSource(SystemPropertiesConfigSource.getInstance())
                .withSource(new ClasspathFileConfigSource(Path.of(APPLICATION_PROPERTIES_1)))
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
