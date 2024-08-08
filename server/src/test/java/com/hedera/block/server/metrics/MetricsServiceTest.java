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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.swirlds.metrics.api.Counter;
import com.swirlds.metrics.api.LongGauge;
import com.swirlds.metrics.api.Metrics;
import org.junit.jupiter.api.Test;

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
}
