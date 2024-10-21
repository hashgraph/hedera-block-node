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

import com.hedera.block.simulator.TestUtils;
import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfigInjectionModuleTest {

    static Configuration configuration;

    @BeforeAll
    static void setUpAll() throws IOException {
        configuration =
                ConfigurationBuilder.create()
                        .withSource(new ClasspathFileConfigSource(Path.of("app.properties")))
                        .autoDiscoverExtensions()
                        .build();
        configuration =
                TestUtils.getTestConfiguration(
                        Map.of("generator.managerImplementation", "BlockAsFileBlockStreamManager"));
    }

    @Test
    void provideBlockStreamConfig() {

        BlockStreamConfig blockStreamConfig =
                ConfigInjectionModule.provideBlockStreamConfig(configuration);

        Assertions.assertNotNull(blockStreamConfig);
        Assertions.assertEquals(1000, blockStreamConfig.blockItemsBatchSize());
    }

    @Test
    void provideGrpcConfig() {
        GrpcConfig grpcConfig = ConfigInjectionModule.provideGrpcConfig(configuration);

        Assertions.assertNotNull(grpcConfig);
        Assertions.assertEquals("localhost", grpcConfig.serverAddress());
        Assertions.assertEquals(8080, grpcConfig.port());
    }

    @Test
    void provideBlockGeneratorConfig() {
        BlockGeneratorConfig blockGeneratorConfig =
                ConfigInjectionModule.provideBlockGeneratorConfig(configuration);

        Assertions.assertNotNull(blockGeneratorConfig);
        Assertions.assertEquals(
                "BlockAsFileBlockStreamManager", blockGeneratorConfig.managerImplementation());
    }
}
