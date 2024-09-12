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

import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Use this configuration across the notifier package.
 *
 * @param ringBufferSize the size of the ring buffer used by the notifier
 */
@ConfigData("notifier")
public record NotifierConfig(@ConfigProperty(defaultValue = "2048") int ringBufferSize) {
    public NotifierConfig {
        if (ringBufferSize <= 0) {
            throw new IllegalArgumentException("Ring buffer size must be greater than 0");
        }
    }
}
