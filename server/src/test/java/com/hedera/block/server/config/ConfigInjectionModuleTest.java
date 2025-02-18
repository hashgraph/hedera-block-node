// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.config;

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.block.server.ServerConfig;
import com.hedera.block.server.config.logging.ConfigurationLogging;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.mediator.MediatorConfig;
import com.hedera.block.server.notifier.NotifierConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.producer.ProducerConfig;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.verification.VerificationConfig;
import com.swirlds.common.metrics.config.MetricsConfig;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;
import com.swirlds.config.api.Configuration;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ConfigInjectionModuleTest {

    @Test
    void testProvidePersistenceStorageConfig() throws IOException {

        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        PersistenceStorageConfig persistenceStorageConfig = configuration.getConfigData(PersistenceStorageConfig.class);

        // Call the method under test
        PersistenceStorageConfig providedConfig = ConfigInjectionModule.providePersistenceStorageConfig(configuration);

        // Verify that the correct config data is returned
        assertNotNull(providedConfig);
        assertSame(persistenceStorageConfig, providedConfig);
    }

    @Test
    void testProvideMetricsConfig() throws IOException {

        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        MetricsConfig metricsConfig = configuration.getConfigData(MetricsConfig.class);

        // Call the method under test
        MetricsConfig providedConfig = ConfigInjectionModule.provideMetricsConfig(configuration);

        // Verify that the correct config data is returned
        assertNotNull(providedConfig);
        assertSame(metricsConfig, providedConfig);
    }

    @Test
    void testProvidePrometheusConfig() throws IOException {

        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        PrometheusConfig prometheusConfig = configuration.getConfigData(PrometheusConfig.class);

        // Call the method under test
        PrometheusConfig providedConfig = ConfigInjectionModule.providePrometheusConfig(configuration);

        // Verify that the correct config data is returned
        assertNotNull(providedConfig);
        assertSame(prometheusConfig, providedConfig);
    }

    @Test
    void testProvideConsumerConfig() throws IOException {

        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        ConsumerConfig consumerConfig = configuration.getConfigData(ConsumerConfig.class);

        // Call the method under test
        ConsumerConfig providedConfig = ConfigInjectionModule.provideConsumerConfig(configuration);

        // Verify that the correct config data is returned
        assertNotNull(providedConfig);
        assertSame(consumerConfig, providedConfig);
    }

    @Test
    void testProvideMediatorConfig() throws IOException {
        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        MediatorConfig mediatorConfig = configuration.getConfigData(MediatorConfig.class);

        MediatorConfig providedConfig = ConfigInjectionModule.provideMediatorConfig(configuration);

        // Verify the config
        assertNotNull(providedConfig);
        assertSame(mediatorConfig, providedConfig);
    }

    @Test
    void testProvideNotifierConfig() throws IOException {
        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        NotifierConfig notifierConfig = configuration.getConfigData(NotifierConfig.class);

        NotifierConfig providedConfig = ConfigInjectionModule.provideNotifierConfig(configuration);

        // Verify the config
        assertNotNull(providedConfig);
        assertSame(notifierConfig, providedConfig);
    }

    @Test
    void testServerConfig() throws IOException {
        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        ServerConfig serverConfig = configuration.getConfigData(ServerConfig.class);

        ServerConfig providedConfig = ConfigInjectionModule.provideServerConfig(configuration);

        // Verify the config
        assertNotNull(providedConfig);
        assertSame(serverConfig, providedConfig);
    }

    @Test
    void testVerificationConfig() throws IOException {
        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        VerificationConfig verificationConfig = configuration.getConfigData(VerificationConfig.class);

        VerificationConfig providedConfig = ConfigInjectionModule.provideVerificationConfig(configuration);

        // Verify the config
        assertNotNull(providedConfig);
        assertSame(verificationConfig, providedConfig);
    }

    @Test
    void testProducerConfig() throws IOException {
        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        ProducerConfig producerConfig = configuration.getConfigData(ProducerConfig.class);

        ProducerConfig providedConfig = ConfigInjectionModule.provideProducerConfig(configuration);

        // Verify the config
        assertNotNull(providedConfig);
        assertSame(producerConfig, providedConfig);
    }

    @Test
    void testConfigurationLogging() throws IOException {
        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();
        ConfigurationLogging providedConfigurationLogging =
                ConfigInjectionModule.provideConfigurationLogging(configuration);

        assertNotNull(providedConfigurationLogging);
    }
}
