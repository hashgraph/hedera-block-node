package com.hedera.block.server.config;

import com.google.auto.service.AutoService;
import com.swirlds.common.config.BasicCommonConfig;
import com.swirlds.common.metrics.config.MetricsConfig;
import com.swirlds.config.api.ConfigurationExtension;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;

import java.util.Set;

@AutoService(ConfigurationExtension.class)
public class BlockNodeConfigExtension implements ConfigurationExtension {

    public Set<Class<? extends Record>> getConfigDataTypes() {
        return Set.of(
                BasicCommonConfig.class,
                MetricsConfig.class,
                PrometheusConfig.class
        );
    }
}
