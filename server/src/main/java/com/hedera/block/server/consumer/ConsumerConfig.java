// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Use this configuration across the consumer package.
 *
 * @param timeoutThresholdMillis after this time of inactivity, the consumer will be considered
 *     timed out and will be disconnected
 */
@ConfigData("consumer")
public record ConsumerConfig(@ConfigProperty(defaultValue = "1500") long timeoutThresholdMillis) {
    private static final System.Logger LOGGER = System.getLogger(ConsumerConfig.class.getName());

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public ConsumerConfig {
        if (timeoutThresholdMillis <= 0) {
            throw new IllegalArgumentException("Timeout threshold must be greater than 0");
        }

        LOGGER.log(
                System.Logger.Level.INFO, "Consumer configuration timeoutThresholdMillis: " + timeoutThresholdMillis);
    }
}
