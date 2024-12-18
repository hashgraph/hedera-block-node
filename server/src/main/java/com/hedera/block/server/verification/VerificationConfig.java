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

package com.hedera.block.server.verification;

import com.hedera.block.server.verification.session.BlockVerificationSessionType;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

@ConfigData("verification")
public record VerificationConfig(
        @ConfigProperty(defaultValue = "true") boolean enabled,
        @ConfigProperty(defaultValue = "ASYNC") BlockVerificationSessionType sessionType,
        @ConfigProperty(defaultValue = "32") int hashCombineBatchSize) {

    private static final System.Logger LOGGER = System.getLogger(VerificationConfig.class.getName());

    public VerificationConfig {
        // hashCombineBatchSize must be even and greater than 2
        if (hashCombineBatchSize % 2 != 0 || hashCombineBatchSize < 2) {
            throw new IllegalArgumentException("hashCombineBatchSize must be even and greater than 2");
        }

        // Log the actual configuration
        LOGGER.log(System.Logger.Level.INFO, "Verification configuration enabled: " + enabled);
        LOGGER.log(System.Logger.Level.INFO, "Verification configuration sessionType: " + sessionType);
        LOGGER.log(
                System.Logger.Level.INFO, "Verification configuration hashCombineBatchSize: " + hashCombineBatchSize);
    }
}
