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

package com.hedera.block.server;

import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;
import com.swirlds.config.api.validation.annotation.Max;
import com.swirlds.config.api.validation.annotation.Min;

/**
 * Use this configuration across the server features
 *
 * <p>ServerConfig will have settings for the server.
 *
 * @param maxMessageSizeBytes the http2 max message/frame size in bytes
 * @param port the port the server will listen on
 */
@ConfigData("server")
public record ServerConfig(
        @ConfigProperty(defaultValue = "4_194_304") @Min(10_240) @Max(16_777_215) int maxMessageSizeBytes,
        @ConfigProperty(defaultValue = "8080") @Min(1024) @Max(65_535) int port) {
    private static final System.Logger LOGGER = System.getLogger(ServerConfig.class.getName());

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public ServerConfig {

        validateMaxMessageSizeBytes(maxMessageSizeBytes);
        validatePort(port);

        LOGGER.log(System.Logger.Level.INFO, "Server configuration server.maxMessageSizeBytes: " + maxMessageSizeBytes);
        LOGGER.log(System.Logger.Level.INFO, "Server configuration server.port: " + port);
    }

    private void validatePort(int port) {
        if (port < 1024) {
            throw new IllegalArgumentException("port must be greater than 1024");
        }

        if (port > 65_535) {
            throw new IllegalArgumentException("port must be less than 65_535");
        }
    }

    private void validateMaxMessageSizeBytes(int maxMessageSizeBytes) {
        if (maxMessageSizeBytes < 10_240) {
            throw new IllegalArgumentException("maxMessageSizeBytes must be greater than 10_240");
        }

        if (maxMessageSizeBytes > 16_777_215) {
            throw new IllegalArgumentException("maxMessageSizeBytes must be less than 16_777_215");
        }
    }
}
