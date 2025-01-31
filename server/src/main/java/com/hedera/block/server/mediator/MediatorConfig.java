// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.mediator;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.config.logging.Loggable;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Constructor to initialize the Mediator configuration.
 *
 * <p>MediatorConfig will set the ring buffer size for the mediator.
 *
 * @param ringBufferSize the number of available "slots" the ring buffer uses internally to store
 *                       events.
 * @param type use a predefined type string to replace the mediator component implementation.
 *  Non-PRODUCTION values should only be used for troubleshooting and development purposes.
 */
@ConfigData("mediator")
public record MediatorConfig(
        @Loggable @ConfigProperty(defaultValue = "1_048_576") int ringBufferSize,
        @Loggable @ConfigProperty(defaultValue = "PRODUCTION") MediatorType type) {

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public MediatorConfig {
        Preconditions.requirePositive(ringBufferSize, "Mediator Ring Buffer Size must be positive");
        Preconditions.requirePowerOfTwo(ringBufferSize, "Mediator Ring Buffer Size must be a power of 2");
    }

    /**
     * The type of mediator to use - PRODUCTION or NO_OP.
     */
    public enum MediatorType {
        PRODUCTION,
        NO_OP,
    }
}
