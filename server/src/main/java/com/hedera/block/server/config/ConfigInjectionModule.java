// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.config;

import com.hedera.block.server.ServerConfig;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.mediator.MediatorConfig;
import com.hedera.block.server.notifier.NotifierConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.producer.ProducerConfig;
import com.hedera.block.server.verification.VerificationConfig;
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

    /**
     * Provides a mediator configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a mediator configuration singleton
     */
    @Singleton
    @Provides
    static MediatorConfig provideMediatorConfig(Configuration configuration) {
        return configuration.getConfigData(MediatorConfig.class);
    }

    /**
     * Provides a notifier configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a notifier configuration singleton
     */
    @Singleton
    @Provides
    static NotifierConfig provideNotifierConfig(Configuration configuration) {
        return configuration.getConfigData(NotifierConfig.class);
    }

    /**
     * Provides a producer configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a producer configuration singleton
     */
    @Singleton
    @Provides
    static ProducerConfig provideProducerConfig(Configuration configuration) {
        return configuration.getConfigData(ProducerConfig.class);
    }

    /**
     * Provides a server configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a server configuration singleton
     */
    @Singleton
    @Provides
    static ServerConfig provideServerConfig(Configuration configuration) {
        return configuration.getConfigData(ServerConfig.class);
    }

    /**
     * Provides a verification configuration singleton using the configuration.
     *
     * @param configuration is the configuration singleton
     * @return a verification configuration singleton
     */
    @Singleton
    @Provides
    static VerificationConfig provideVerificationConfig(Configuration configuration) {
        return configuration.getConfigData(VerificationConfig.class);
    }
}
