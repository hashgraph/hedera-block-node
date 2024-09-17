/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

        LOGGER.log(
                System.Logger.Level.INFO,
                "Service configuration service.delayMillis: " + delayMillis);
    }
}
