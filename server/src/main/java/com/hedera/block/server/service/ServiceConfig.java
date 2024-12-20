// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.service;

import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Use this configuration across the service package.
 *
 * <p>ServiceConfig will set the default shutdown delay for the service.
 *
 * @param delayMillis the delay in milliseconds for the service
 */
@ConfigData("service")
public record ServiceConfig(@ConfigProperty(defaultValue = "500") int delayMillis) {
    private static final System.Logger LOGGER = System.getLogger(ServiceConfig.class.getName());

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public ServiceConfig {
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("Delay milliseconds must be greater than 0");
        }

        LOGGER.log(System.Logger.Level.INFO, "Service configuration service.delayMillis: " + delayMillis);
    }
}
