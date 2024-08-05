package com.hedera.block.server.config;

import com.swirlds.config.api.Configuration;
import com.swirlds.metrics.api.Metrics;

public record BlockNodeContext(Metrics metrics, Configuration configuration) {}
