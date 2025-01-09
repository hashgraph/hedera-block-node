// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.verification;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.verification.session.BlockVerificationSessionType;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

/**
 * Configuration for the verification module.
 *
 * @param enabled whether the verification module is enabled
 * @param sessionType the type of the verification session
 * @param hashCombineBatchSize the size of the batch used to combine hashes
 */
@ConfigData("verification")
public record VerificationConfig(
        @ConfigProperty(defaultValue = "true") boolean enabled,
        @ConfigProperty(defaultValue = "ASYNC") BlockVerificationSessionType sessionType,
        @ConfigProperty(defaultValue = "32") int hashCombineBatchSize) {

    /**
     * Constructs a new instance of {@link VerificationConfig}.
     */
    private static final System.Logger LOGGER = System.getLogger(VerificationConfig.class.getName());

    /**
     * Constructs a new instance of {@link VerificationConfig}.
     *
     * @param enabled              whether the verification module is enabled
     * @param sessionType          the type of the verification session
     * @param hashCombineBatchSize the size of the batch used to combine hashes
     */
    public VerificationConfig {
        // hashCombineBatchSize must be even and greater than 2
        Preconditions.requirePositive(hashCombineBatchSize, "[VERIFICATION_HASH_COMBINE_BATCH_SIZE] must be positive");
        Preconditions.requireEven(
                hashCombineBatchSize, "[VERIFICATION_HASH_COMBINE_BATCH_SIZE] must be even and greater than 2");

        // Log the actual configuration
        LOGGER.log(System.Logger.Level.INFO, "Verification Properties:");
        LOGGER.log(System.Logger.Level.INFO, "verification.enabled - VERIFICATION_ENABLED : " + enabled);
        LOGGER.log(System.Logger.Level.INFO, "verification.sessionType - VERIFICATION_SESSION_TYPE : " + sessionType);
        LOGGER.log(
                System.Logger.Level.INFO,
                "verification.hashCombineBatchSize - VERIFICATION_HASH_COMBINE_BATCH_SIZE : " + hashCombineBatchSize);
    }
}
