package com.hedera.block.server.config;

import com.swirlds.common.metrics.platform.DefaultMetricsProvider;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import com.swirlds.metrics.api.Metrics;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

public class BlockNodeContextFactory {
    // private static final Logger logger = LogManager.getLogger(ContextFactory.class);

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
            // logger.error("Error reading configuration", e);
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
