// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.notifier;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.config.logging.Loggable;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Constructor to initialize the Notifier configuration.
 *
 * <p>NotifierConfig will set the ring buffer size for the notifier.
 *
 * @param ringBufferSize the number of available "slots" the ring buffer uses internally to store
 *                       events.
 */
@ConfigData("notifier")
public record NotifierConfig(@Loggable @ConfigProperty(defaultValue = "1024") int ringBufferSize) {

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public NotifierConfig {
        Preconditions.requirePositive(ringBufferSize, "Notifier Ring Buffer Size must be positive");
        Preconditions.requirePowerOfTwo(ringBufferSize, "Notifier Ring Buffer Size must be a power of 2");
    }
}
