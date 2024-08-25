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

import com.swirlds.common.metrics.platform.DefaultMetricsProvider;
import com.swirlds.config.api.Configuration;
import com.swirlds.metrics.api.Metrics;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** The module used to inject the metrics service and metrics into the application. */
@Module
public interface MetricsInjectionModule {

    /**
     * Provides the metrics service.
     *
     * @param metrics the metrics to be used by the service
     * @return the metrics service
     */
    @Singleton
    @Provides
    static MetricsService provideMetricsService(Metrics metrics) {
        return new MetricsService(metrics);
    }

    /**
     * Provides the metrics.
     *
     * @param configuration the configuration to be used by the metrics
     * @return the metrics
     */
    @Singleton
    @Provides
    static Metrics provideMetrics(Configuration configuration) {
        final DefaultMetricsProvider metricsProvider = new DefaultMetricsProvider(configuration);
        final Metrics metrics = metricsProvider.createGlobalMetrics();
        metricsProvider.start();
        return metrics;
    }
}
