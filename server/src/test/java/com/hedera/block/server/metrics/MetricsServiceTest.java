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

import static com.hedera.block.server.metrics.BlockNodeMetricNames.Counter.BlocksPersisted;
import static com.hedera.block.server.metrics.BlockNodeMetricNames.Counter.LiveBlockItems;
import static com.hedera.block.server.metrics.BlockNodeMetricNames.Counter.LiveBlockItemsConsumed;
import static com.hedera.block.server.metrics.BlockNodeMetricNames.Counter.SingleBlocksRetrieved;
import static com.hedera.block.server.metrics.BlockNodeMetricNames.Gauge.Subscribers;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.util.TestConfigUtil;
import com.swirlds.config.api.Configuration;
import dagger.BindsInstance;
import dagger.Component;
import java.io.IOException;
import javax.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetricsServiceTest {

    @Singleton
    @Component(modules = {MetricsInjectionModule.class})
    public interface MetricsServiceTestComponent {

        MetricsService getMetricsService();

        @Component.Factory
        interface Factory {
            MetricsServiceTestComponent create(@BindsInstance Configuration configuration);
        }
    }

    private MetricsService metricsService;

    @BeforeEach
    public void setUp() throws IOException {
        final BlockNodeContext context = TestConfigUtil.getTestBlockNodeContext();
        final Configuration configuration = context.configuration();
        final MetricsServiceTestComponent testComponent =
                DaggerMetricsServiceTest_MetricsServiceTestComponent.factory()
                        .create(configuration);
        this.metricsService = testComponent.getMetricsService();
    }

    @Test
    void MetricsService_verifyLiveBlockItemsCounter() {

        for (int i = 0; i < 10; i++) {
            metricsService.get(LiveBlockItems).increment();
        }

        assertEquals(LiveBlockItems.grafanaLabel(), metricsService.get(LiveBlockItems).getName());
        assertEquals(
                LiveBlockItems.description(), metricsService.get(LiveBlockItems).getDescription());
        assertEquals(10, metricsService.get(LiveBlockItems).get());
    }

    @Test
    void MetricsService_verifyBlocksPersistedCounter() {

        for (int i = 0; i < 10; i++) {
            metricsService.get(BlocksPersisted).increment();
        }

        assertEquals(BlocksPersisted.grafanaLabel(), metricsService.get(BlocksPersisted).getName());
        assertEquals(
                BlocksPersisted.description(),
                metricsService.get(BlocksPersisted).getDescription());
        assertEquals(10, metricsService.get(BlocksPersisted).get());
    }

    @Test
    void MetricsService_verifySingleBlocksRetrievedCounter() {

        for (int i = 0; i < 10; i++) {
            metricsService.get(SingleBlocksRetrieved).increment();
        }

        assertEquals(
                SingleBlocksRetrieved.grafanaLabel(),
                metricsService.get(SingleBlocksRetrieved).getName());
        assertEquals(
                SingleBlocksRetrieved.description(),
                metricsService.get(SingleBlocksRetrieved).getDescription());
        assertEquals(10, metricsService.get(SingleBlocksRetrieved).get());
    }

    @Test
    void MetricsService_verifyLiveBlockItemsConsumedCounter() {

        for (int i = 0; i < 10; i++) {
            metricsService.get(LiveBlockItemsConsumed).increment();
        }

        assertEquals(
                LiveBlockItemsConsumed.grafanaLabel(),
                metricsService.get(LiveBlockItemsConsumed).getName());
        assertEquals(
                LiveBlockItemsConsumed.description(),
                metricsService.get(LiveBlockItemsConsumed).getDescription());
        assertEquals(10, metricsService.get(LiveBlockItemsConsumed).get());
    }

    @Test
    void MetricsService_verifySubscribersGauge() {

        assertEquals(Subscribers.grafanaLabel(), metricsService.get(Subscribers).getName());
        assertEquals(Subscribers.description(), metricsService.get(Subscribers).getDescription());

        // Set the subscribers to various values and verify
        metricsService.get(Subscribers).set(10);
        assertEquals(10, metricsService.get(Subscribers).get());

        metricsService.get(Subscribers).set(3);
        assertEquals(3, metricsService.get(Subscribers).get());

        metricsService.get(Subscribers).set(0);
        assertEquals(0, metricsService.get(Subscribers).get());
    }
}
