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

package com.hedera.block.server.metrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.util.TestConfigUtil;
import com.swirlds.config.api.Configuration;
import com.swirlds.metrics.api.Metrics;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsInjectionModuleTest {

    @Mock private Metrics metrics;

    @Test
    void testProvideMetricsService() {
        // Call the method under test
        MetricsService metricsService = MetricsInjectionModule.provideMetricsService(metrics);

        // Verify that the metricsService is correctly instantiated
        assertNotNull(metricsService);
    }

    @Test
    void testProvideMetrics() throws IOException {
        BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        Configuration configuration = context.configuration();

        // Call the method under test
        Metrics providedMetrics = MetricsInjectionModule.provideMetrics(configuration);

        assertNotNull(providedMetrics);
    }
}
