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
import com.hedera.block.server.config.TestConfigBuilder;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.metrics.MetricsServiceImpl;
import com.swirlds.common.metrics.platform.DefaultMetricsProvider;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.metrics.api.Metrics;
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

        Metrics metrics = getTestMetrics(testConfiguration);

        MetricsService metricsService = new MetricsServiceImpl(metrics);

        return new BlockNodeContext(metricsService, testConfiguration);
    }

    public static BlockNodeContext getTestBlockNodeContext() throws IOException {
        return getTestBlockNodeContext(Collections.emptyMap());
    }

    // TODO: we need to create a Mock metrics, and avoid using the real metrics for tests
    public static Metrics getTestMetrics(@NonNull Configuration configuration) {
        final DefaultMetricsProvider metricsProvider = new DefaultMetricsProvider(configuration);
        final Metrics metrics = metricsProvider.createGlobalMetrics();
        metricsProvider.start();
        return metrics;
    }
}
