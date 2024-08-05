package com.hedera.block.server.metrics;

import com.swirlds.metrics.api.Counter;
import com.swirlds.metrics.api.LongGauge;
import com.swirlds.metrics.api.Metrics;

public class MetricsService {

    private static final LongGauge.Config EXAMPLE_GAUGE = new LongGauge.Config("app", "exampleGauge")
            .withDescription("An example gauge");

    public final LongGauge exampleGauge;

    private static final Counter.Config EXAMPLE_COUNTER = new Counter.Config("app", "exampleCounter")
            .withDescription("An example counter");

    public final Counter exampleCounter;

    public MetricsService(final Metrics metrics) {
        this.exampleGauge = metrics.getOrCreate(EXAMPLE_GAUGE);
        this.exampleCounter = metrics.getOrCreate(EXAMPLE_COUNTER);
    }

}

