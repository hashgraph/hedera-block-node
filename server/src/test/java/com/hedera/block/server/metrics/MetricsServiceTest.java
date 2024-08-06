package com.hedera.block.server.metrics;

import com.swirlds.metrics.api.Counter;
import com.swirlds.metrics.api.LongGauge;
import com.swirlds.metrics.api.Metrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetricsServiceTest {

    @Test
    void MetricsService_initializesExampleGauge() {
        Metrics metrics = mock(Metrics.class);
        LongGauge exampleGauge = mock(LongGauge.class);
        when(metrics.getOrCreate(any(LongGauge.Config.class))).thenReturn(exampleGauge);

        MetricsService service = new MetricsService(metrics);

        assertEquals(exampleGauge, service.exampleGauge);
    }

    @Test
    void MetricsService_initializesExampleCounter() {
        Metrics metrics = mock(Metrics.class);
        Counter exampleCounter = mock(Counter.class);
        when(metrics.getOrCreate(any(Counter.Config.class))).thenReturn(exampleCounter);

        MetricsService service = new MetricsService(metrics);

        assertEquals(exampleCounter, service.exampleCounter);
    }

    @Test
    void MetricsService_handlesNullMetrics() {
        assertThrows(NullPointerException.class, () -> new MetricsService(null));
    }
}
