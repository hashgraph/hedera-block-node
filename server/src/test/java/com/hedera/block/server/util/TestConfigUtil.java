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

package com.hedera.block.server.util;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.config.BlockNodeContextFactory;
import com.hedera.block.server.config.TestConfigBuilder;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public class TestConfigUtil {

    public static final String CONSUMER_TIMEOUT_THRESHOLD_KEY = "consumer.timeoutThresholdMillis";

    private static final String TEST_APP_PROPERTIES_FILE = "app.properties";

    private TestConfigUtil() {}

    @NonNull
    public static BlockNodeContext getTestBlockNodeContext(
            @NonNull Map<String, String> customProperties) throws IOException {

        // we still use the BlockNodeContextFactory to create the BlockNodeContext temporally,
        // but we will replace the configuration with a test configuration
        // sooner we will need to create a metrics mock, and never use the BlockNodeContextFactory.
        BlockNodeContext blockNodeContext = BlockNodeContextFactory.create();

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

        testConfigBuilder = testConfigBuilder.withConfigDataType(ConsumerConfig.class);

        Configuration testConfiguration = testConfigBuilder.getOrCreateConfig();

        return new BlockNodeContext(
                blockNodeContext.metrics(), blockNodeContext.metricsService(), testConfiguration);
    }

    public static BlockNodeContext getTestBlockNodeContext() throws IOException {
        return getTestBlockNodeContext(Collections.emptyMap());
    }
}
