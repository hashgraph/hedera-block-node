// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.mediator;

import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.common.utils.Preconditions;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Constructor to initialize the Mediator configuration.
 *
 * <p>MediatorConfig will set the ring buffer size for the mediator.
 *
 * @param ringBufferSize the size of the ring buffer used by the mediator
 * @param type use a predefined type string to replace the mediator component implementation.
 *  Non-PRODUCTION values should only be used for troubleshooting and development purposes.
 */
// 131072 works but not with persistence
@ConfigData("mediator")
public record MediatorConfig(
        @ConfigProperty(defaultValue = "4194304") int ringBufferSize,
        @ConfigProperty(defaultValue = "PRODUCTION") String type) {
    private static final System.Logger LOGGER = System.getLogger(MediatorConfig.class.getName());

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public MediatorConfig {
        Preconditions.requirePositive(ringBufferSize, "Mediator Ring Buffer Size must be positive");
        Preconditions.requirePowerOfTwo(ringBufferSize, "Mediator Ring Buffer Size must be a power of 2");
        LOGGER.log(INFO, "Mediator configuration mediator.ringBufferSize: " + ringBufferSize);
        LOGGER.log(INFO, "Mediator configuration mediator.type: " + type);
    }
}
