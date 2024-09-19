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

package com.hedera.block.simulator;

import com.hedera.block.simulator.config.TestConfigBuilder;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class TestUtils {

    private static final String TEST_APP_PROPERTIES_FILE = "app.properties";

    public static Configuration getTestConfiguration(@NonNull Map<String, String> customProperties)
            throws IOException {
        // create test configuration
        TestConfigBuilder testConfigBuilder =
                new TestConfigBuilder(true)
                        .withSource(
                                new ClasspathFileConfigSource(Path.of(TEST_APP_PROPERTIES_FILE)));

        for (Map.Entry<String, String> entry : customProperties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            testConfigBuilder = testConfigBuilder.withValue(key, value);
        }

        testConfigBuilder = testConfigBuilder.withConfigDataType(BlockStreamConfig.class);
        testConfigBuilder = testConfigBuilder.withConfigDataType(GrpcConfig.class);

        return testConfigBuilder.getOrCreateConfig();
    }

    public static Configuration getTestConfiguration() throws IOException {
        return getTestConfiguration(Map.of());
    }
}