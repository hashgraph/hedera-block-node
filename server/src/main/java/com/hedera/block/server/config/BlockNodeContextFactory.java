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

import com.swirlds.common.metrics.platform.DefaultMetricsProvider;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import com.swirlds.metrics.api.Metrics;
import java.io.IOException;
import java.nio.file.Path;

public class BlockNodeContextFactory {
    private static final System.Logger logger =
            System.getLogger(BlockNodeContextFactory.class.getName());

    private static final String APPLICATION_PROPERTIES_1 = "app.properties";

    private BlockNodeContextFactory() {}

    public static BlockNodeContext create() {
        final Configuration configuration = getConfiguration();
        final Metrics metrics = getMetrics(configuration);
        return new BlockNodeContext(metrics, configuration);
    }

    private static Configuration getConfiguration() {
        try {
            return ConfigurationBuilder.create()
                    .withSource(SystemEnvironmentConfigSource.getInstance())
                    .withSource(SystemPropertiesConfigSource.getInstance())
                    .withSource(new ClasspathFileConfigSource(Path.of(APPLICATION_PROPERTIES_1)))
                    .autoDiscoverExtensions()
                    .build();
        } catch (IOException e) {
            logger.log(System.Logger.Level.ERROR, "Error reading configuration", e);
            throw new RuntimeException("Error reading configuration", e);
        }
    }

    private static Metrics getMetrics(final Configuration configuration) {
        final DefaultMetricsProvider metricsProvider = new DefaultMetricsProvider(configuration);
        final Metrics metrics = metricsProvider.createGlobalMetrics();
        metricsProvider.start();
        return metrics;
    }
}
