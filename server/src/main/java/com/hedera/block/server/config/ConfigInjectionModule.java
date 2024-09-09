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
import com.hedera.block.server.mediator.MediatorConfig;
import com.hedera.block.server.notifier.NotifierConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.swirlds.common.metrics.config.MetricsConfig;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;
import com.swirlds.config.api.Configuration;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * A Dagger module for providing configuration dependencies, any specific configuration should be
 * part of this module.
 */
@Module
public interface ConfigInjectionModule {

    /**
     * Provides a persistence storage configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a persistence storage configuration singleton
     */
    @Singleton
    @Provides
    static PersistenceStorageConfig providePersistenceStorageConfig(Configuration configuration) {
        return configuration.getConfigData(PersistenceStorageConfig.class);
    }

    /**
     * Provides a metrics configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a metrics configuration singleton
     */
    @Singleton
    @Provides
    static MetricsConfig provideMetricsConfig(Configuration configuration) {
        return configuration.getConfigData(MetricsConfig.class);
    }

    /**
     * Provides a Prometheus configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a Prometheus configuration singleton
     */
    @Singleton
    @Provides
    static PrometheusConfig providePrometheusConfig(Configuration configuration) {
        return configuration.getConfigData(PrometheusConfig.class);
    }

    /**
     * Provides a consumer configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a consumer configuration singleton
     */
    @Singleton
    @Provides
    static ConsumerConfig provideConsumerConfig(Configuration configuration) {
        return configuration.getConfigData(ConsumerConfig.class);
    }

    @Singleton
    @Provides
    static MediatorConfig provideMediatorConfig(Configuration configuration) {
        return configuration.getConfigData(MediatorConfig.class);
    }

    @Singleton
    @Provides
    static NotifierConfig provideNotifierConfig(Configuration configuration) {
        return configuration.getConfigData(NotifierConfig.class);
    }
}
