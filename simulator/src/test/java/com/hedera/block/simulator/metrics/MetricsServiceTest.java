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

package com.hedera.block.simulator.metrics;

import static com.hedera.block.simulator.TestUtils.getTestMetrics;
import static com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter.LiveBlockItemsSent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hedera.block.simulator.TestUtils;
import com.swirlds.config.api.Configuration;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetricsServiceTest {

    private MetricsService metricsService;

    @BeforeEach
    public void setUp() throws IOException {
        Configuration config = TestUtils.getTestConfiguration();
        metricsService = new MetricsServiceImpl(getTestMetrics(config));
    }

    @Test
    void MetricsService_verifyLiveBlockItemsSentCounter() {

        for (int i = 0; i < 10; i++) {
            metricsService.get(LiveBlockItemsSent).increment();
        }

        assertEquals(
                LiveBlockItemsSent.grafanaLabel(),
                metricsService.get(LiveBlockItemsSent).getName());
        assertEquals(
                LiveBlockItemsSent.description(),
                metricsService.get(LiveBlockItemsSent).getDescription());
        assertEquals(10, metricsService.get(LiveBlockItemsSent).get());
    }
}
