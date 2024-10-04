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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.pbj.PbjBlockStreamService;
import com.hedera.block.server.pbj.PbjBlockStreamServiceProxy;
import com.swirlds.config.api.Configuration;
import dagger.Binds;
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
     * Provides a block stream service singleton using DI.
     *
     * @param pbjBlockStreamService should come from DI
     * @return a block stream service singleton
     */
    @Singleton
    @Binds
    PbjBlockStreamService bindPbjBlockStreamService(
            PbjBlockStreamServiceProxy pbjBlockStreamService);

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
