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

package com.hedera.block.simulator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfigInjectionModuleTest {

    static Configuration configuration;

    @BeforeAll
    static void setUpAll() throws IOException {
        configuration = ConfigurationBuilder.create()
                .withSource(new ClasspathFileConfigSource(Path.of("app.properties")))
                .autoDiscoverExtensions()
                .build();
        configuration = TestUtils.getTestConfiguration(
                Map.of("generator.managerImplementation", "BlockAsFileBlockStreamManager"));
    }

    @Test
    void provideBlockStreamConfig() {

        BlockStreamConfig blockStreamConfig = ConfigInjectionModule.provideBlockStreamConfig(configuration);

        assertNotNull(blockStreamConfig);
        assertEquals(1000, blockStreamConfig.blockItemsBatchSize());
    }

    @Test
    void provideGrpcConfig() {
        GrpcConfig grpcConfig = ConfigInjectionModule.provideGrpcConfig(configuration);

        assertNotNull(grpcConfig);
        assertEquals("localhost", grpcConfig.serverAddress());
        assertEquals(8080, grpcConfig.port());
    }

    @Test
    void provideBlockGeneratorConfig() {
        BlockGeneratorConfig blockGeneratorConfig = ConfigInjectionModule.provideBlockGeneratorConfig(configuration);

        assertNotNull(blockGeneratorConfig);
        assertEquals("BlockAsFileBlockStreamManager", blockGeneratorConfig.managerImplementation());
    }

    @Test
    void providePrometheusConfig() {
        PrometheusConfig prometheusConfig = ConfigInjectionModule.providePrometheusConfig(configuration);

        assertNotNull(prometheusConfig);
        assertFalse(prometheusConfig.endpointEnabled());
        assertEquals(9999, prometheusConfig.endpointPortNumber());
    }
}