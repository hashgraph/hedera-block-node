// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.config;

import com.google.auto.service.AutoService;
import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.ConsumerConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.swirlds.common.metrics.config.MetricsConfig;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;
import com.swirlds.config.api.ConfigurationExtension;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;

/** Sets up configuration for services. */
@AutoService(ConfigurationExtension.class)
public class SimulatorConfigExtension implements ConfigurationExtension {

    /** Explicitly defined constructor. */
    public SimulatorConfigExtension() {
        super();
    }

    @NonNull
    @Override
    public Set<Class<? extends Record>> getConfigDataTypes() {
        return Set.of(
                BlockStreamConfig.class,
                ConsumerConfig.class,
                GrpcConfig.class,
                BlockGeneratorConfig.class,
                MetricsConfig.class,
                PrometheusConfig.class);
    }
}
