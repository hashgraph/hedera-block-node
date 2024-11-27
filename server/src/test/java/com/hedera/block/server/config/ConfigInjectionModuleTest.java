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

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.block.server.ServerConfig;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.mediator.MediatorConfig;
import com.hedera.block.server.notifier.NotifierConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.util.TestConfigUtil;
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
}
