// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.notifier;

import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.common.utils.Preconditions;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Constructor to initialize the Notifier configuration.
 *
 * <p>NotifierConfig will set the ring buffer size for the notifier.
 *
 * @param ringBufferSize the size of the ring buffer used by the notifier
 */
@ConfigData("notifier")
public record NotifierConfig(@ConfigProperty(defaultValue = "1024") int ringBufferSize) {
    private static final System.Logger LOGGER = System.getLogger(NotifierConfig.class.getName());

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public NotifierConfig {
        Preconditions.requirePositive(ringBufferSize, "Notifier Ring Buffer Size must be positive");
        Preconditions.requirePowerOfTwo(ringBufferSize, "Notifier Ring Buffer Size must be a power of 2");
        LOGGER.log(INFO, "Notifier configuration notifier.ringBufferSize: " + ringBufferSize);
    }
}
