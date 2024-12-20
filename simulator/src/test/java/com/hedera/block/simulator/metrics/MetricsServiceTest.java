// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.metrics;

import static com.hedera.block.simulator.TestUtils.getTestMetrics;
import static com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter.LiveBlockItemsSent;
import static com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter.LiveBlocksSent;
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

    @Test
    void MetricsService_verifyLiveBlocksSentCounter() {

        for (int i = 0; i < 10; i++) {
            metricsService.get(LiveBlocksSent).increment();
        }

        assertEquals(
                LiveBlocksSent.grafanaLabel(),
                metricsService.get(LiveBlocksSent).getName());
        assertEquals(
                LiveBlocksSent.description(), metricsService.get(LiveBlocksSent).getDescription());
        assertEquals(10, metricsService.get(LiveBlocksSent).get());
    }
}
