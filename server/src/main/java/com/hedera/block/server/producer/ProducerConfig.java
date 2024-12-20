// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.producer;

import static java.lang.System.Logger.Level.INFO;

import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Use this configuration across the producer package
 *
 * @param type use a predefined type string to replace the producer component implementation.
 *     Non-PRODUCTION values should only be used for troubleshooting and development purposes.
 */
@ConfigData("producer")
public record ProducerConfig(@ConfigProperty(defaultValue = "PRODUCTION") String type) {
    private static final System.Logger LOGGER = System.getLogger(ProducerConfig.class.getName());

    /**
     * Creates a new ProducerConfig instance.
     *
     * @param type the producer type
     */
    public ProducerConfig {
        LOGGER.log(INFO, "Producer configuration producer.type: " + type);
    }
}
