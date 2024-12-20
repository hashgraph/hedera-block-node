// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.metrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedera.block.simulator.TestUtils;
import com.swirlds.config.api.Configuration;
import com.swirlds.metrics.api.Metrics;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class MetricsInjectionModuleTest {

    @Test
    void testProvideMetrics() throws IOException {
        Configuration configuration = TestUtils.getTestConfiguration();

        // Call the method under test
        Metrics providedMetrics = MetricsInjectionModule.provideMetrics(configuration);

        assertNotNull(providedMetrics);
    }
}
