package com.hedera.block.server.config;

import com.swirlds.config.api.Configuration;
import com.swirlds.metrics.api.Metrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockNodeContextTest {

    @Test
    void BlockNodeContext_initializesWithMetricsAndConfiguration() {
        Metrics metrics = mock(Metrics.class);
        Configuration configuration = mock(Configuration.class);

        BlockNodeContext context = new BlockNodeContext(metrics, configuration);

        assertEquals(metrics, context.metrics());
        assertEquals(configuration, context.configuration());
    }
}
