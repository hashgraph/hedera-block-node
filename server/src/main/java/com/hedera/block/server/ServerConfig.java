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
        @ConfigProperty(defaultValue = defaultMaxMessageSizeBytes)
                @Min(minMaxMessageSizeBytes)
                @Max(maxMaxMessageSizeBytes)
                int maxMessageSizeBytes,
        @ConfigProperty(defaultValue = defaultPort) @Min(minPort) @Max(maxPort) int port) {
    private static final System.Logger LOGGER = System.getLogger(ServerConfig.class.getName());

    // Constants for maxMessageSizeBytes property
    private static final String defaultMaxMessageSizeBytes = "4_194_304";
    private static final long minMaxMessageSizeBytes = 10_240;
    private static final long maxMaxMessageSizeBytes = 16_777_215;

    // Constants for port property
    private static final String defaultPort = "8080";
    private static final int minPort = 1024;
    private static final int maxPort = 65_535;

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
        if (port < minPort) {
            throw new IllegalArgumentException("port must be greater than " + minPort);
        }

        if (port > maxPort) {
            throw new IllegalArgumentException("port must be less than " + maxPort);
        }
    }

    private void validateMaxMessageSizeBytes(int maxMessageSizeBytes) {
        if (maxMessageSizeBytes < minMaxMessageSizeBytes) {
            throw new IllegalArgumentException("maxMessageSizeBytes must be greater than " + minMaxMessageSizeBytes);
        }

        if (maxMessageSizeBytes > maxMaxMessageSizeBytes) {
            throw new IllegalArgumentException("maxMessageSizeBytes must be less than " + maxMaxMessageSizeBytes);
        }
    }
}
