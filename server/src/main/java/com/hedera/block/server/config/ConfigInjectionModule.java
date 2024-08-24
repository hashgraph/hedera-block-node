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

import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.swirlds.common.config.BasicCommonConfig;
import com.swirlds.common.metrics.config.MetricsConfig;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Singleton;

@Module
public interface ConfigInjectionModule {

    static final String APPLICATION_PROPERTIES = "app.properties";

    @Singleton
    @Provides
    static Configuration provideConfiguration() {
        try {
            return ConfigurationBuilder.create()
                    .withSource(SystemEnvironmentConfigSource.getInstance())
                    .withSource(SystemPropertiesConfigSource.getInstance())
                    .withSource(new ClasspathFileConfigSource(Path.of(APPLICATION_PROPERTIES)))
                    .autoDiscoverExtensions()
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create configuration", e);
        }
    }

    @Singleton
    @Provides
    static PersistenceStorageConfig providePersistenceStorageConfig(
            @NonNull Configuration configuration) {
        return configuration.getConfigData(PersistenceStorageConfig.class);
    }

    @Singleton
    @Provides
    static MetricsConfig provideMetricsConfig(@NonNull Configuration configuration) {
        return configuration.getConfigData(MetricsConfig.class);
    }

    @Singleton
    @Provides
    static PrometheusConfig providePrometheusConfig(@NonNull Configuration configuration) {
        return configuration.getConfigData(PrometheusConfig.class);
    }

    @Singleton
    @Provides
    static ConsumerConfig provideConsumerConfig(@NonNull Configuration configuration) {
        return configuration.getConfigData(ConsumerConfig.class);
    }

    @Singleton
    @Provides
    static BasicCommonConfig provideBasicCommonConfig(@NonNull Configuration configuration) {
        return configuration.getConfigData(BasicCommonConfig.class);
    }
}
