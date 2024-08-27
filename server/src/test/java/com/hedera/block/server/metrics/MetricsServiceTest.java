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
    void MetricsService_initializesLiveBlockItemsCounter() {
        Metrics metrics = mock(Metrics.class);
        Counter liveBlockItems = mock(Counter.class);
        when(metrics.getOrCreate(any(Counter.Config.class))).thenReturn(liveBlockItems);

        MetricsService service = new MetricsService(metrics);

        assertEquals(liveBlockItems, service.liveBlockItems());

        service.liveBlockItems().increment();
        verify(liveBlockItems, times(1)).increment();
    }

    @Test
    void MetricsService_initializesBlocksPersistedCounter() {
        Metrics metrics = mock(Metrics.class);
        Counter blocksPersisted = mock(Counter.class);
        when(metrics.getOrCreate(any(Counter.Config.class))).thenReturn(blocksPersisted);

        MetricsService service = new MetricsService(metrics);

        assertEquals(blocksPersisted, service.blocksPersisted());

        service.blocksPersisted().increment();
        verify(blocksPersisted, times(1)).increment();
    }

    @Test
    void MetricsService_initializesSingleBlocksRetrievedCounter() {
        Metrics metrics = mock(Metrics.class);
        Counter singleBlocksRetrieved = mock(Counter.class);
        when(metrics.getOrCreate(any(Counter.Config.class))).thenReturn(singleBlocksRetrieved);

        MetricsService service = new MetricsService(metrics);

        assertEquals(singleBlocksRetrieved, service.singleBlocksRetrieved());

        service.singleBlocksRetrieved().increment();
        verify(singleBlocksRetrieved, times(1)).increment();
    }

    @Test
    void MetricsService_initializesSubscribersGauge() {
        Metrics metrics = mock(Metrics.class);
        LongGauge subscribers = mock(LongGauge.class);
        when(metrics.getOrCreate(any(LongGauge.Config.class))).thenReturn(subscribers);

        MetricsService service = new MetricsService(metrics);

        assertEquals(subscribers, service.subscribers());

        service.subscribers().set(5);
        verify(subscribers, times(1)).set(5);
    }
}
