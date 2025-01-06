// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.swirlds.config.api.Configuration;
import dagger.Module;
import dagger.Provides;
import io.helidon.webserver.WebServerConfig;
import javax.inject.Singleton;

/**
 * A Dagger Module for interfaces that are at the BlockNodeApp Level, should be temporary and
 * everything should be inside its own modules.
 */
@Module
public interface BlockNodeAppInjectionModule {

    /**
     * Provides a block node context singleton.
     *
     * @param config should come from DI
     * @param metricsService should come from DI
     * @return a block node context singleton
     */
    @Singleton
    @Provides
    static BlockNodeContext provideBlockNodeContext(
            Configuration config, MetricsService metricsService) {
        return new BlockNodeContext(metricsService, config);
    }

    /**
     * Provides a web server config builder singleton using DI.
     *
     * @return a web server config builder singleton
     */
    @Singleton
    @Provides
    static WebServerConfig.Builder provideWebServerConfigBuilder() {
        return WebServerConfig.builder();
    }
}
