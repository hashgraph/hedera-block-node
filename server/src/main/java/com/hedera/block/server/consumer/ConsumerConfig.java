package com.hedera.block.server.consumer;

import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

@ConfigData("consumer")
public record ConsumerConfig(
        @ConfigProperty(defaultValue = "1500") long timeoutThresholdMillis
) {}
