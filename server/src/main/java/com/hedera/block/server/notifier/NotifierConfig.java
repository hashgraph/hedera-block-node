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

package com.hedera.block.server.notifier;

import static java.lang.System.Logger.Level.INFO;

import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Constructor to initialize the Notifier configuration.
 *
 * <p>NotifierConfig will set the ring buffer size for the notifier.
 *
 * @param ringBufferSize the size of the ring buffer used by the notifier
 */
// TODO: defaultValue here should be 2048
@ConfigData("notifier")
public record NotifierConfig(@ConfigProperty(defaultValue = "1024") int ringBufferSize) {
    private static final System.Logger LOGGER = System.getLogger(NotifierConfig.class.getName());

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public NotifierConfig {
        if (ringBufferSize <= 0) {
            throw new IllegalArgumentException("Ring buffer size must be greater than 0");
        }

        LOGGER.log(INFO, "Notifier configuration notifier.ringBufferSize: " + ringBufferSize);
    }
}
