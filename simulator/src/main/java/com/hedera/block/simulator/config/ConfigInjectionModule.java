// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.config;

import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.ConsumerConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;
import com.swirlds.config.api.Configuration;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** The module used to inject the configuration data into the application. */
@Module
public interface ConfigInjectionModule {

    /**
     * Provides the block stream configuration.
     *
     * @param configuration the configuration to be used by the block stream
     * @return the block stream configuration
     */
    @Singleton
    @Provides
    static BlockStreamConfig provideBlockStreamConfig(Configuration configuration) {
        return configuration.getConfigData(BlockStreamConfig.class);
    }

    /**
     * Provides the consumer configuration.
     *
     * @param configuration the configuration to be used by the block consumer
     * @return the block consumer configuration
     */
    @Singleton
    @Provides
    static ConsumerConfig provideConsumerConfig(Configuration configuration) {
        return configuration.getConfigData(ConsumerConfig.class);
    }

    /**
     * Provides the gRPC configuration.
     *
     * @param configuration the configuration to be used by the gRPC
     * @return the gRPC configuration
     */
    @Singleton
    @Provides
    static GrpcConfig provideGrpcConfig(Configuration configuration) {
        return configuration.getConfigData(GrpcConfig.class);
    }

    /**
     * Provides the block generator configuration.
     *
     * @param configuration the configuration to be used by the block generator
     * @return the block generator configuration
     */
    @Singleton
    @Provides
    static BlockGeneratorConfig provideBlockGeneratorConfig(Configuration configuration) {
        return configuration.getConfigData(BlockGeneratorConfig.class);
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
}
