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
