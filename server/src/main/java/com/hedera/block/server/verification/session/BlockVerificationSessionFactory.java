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

package com.hedera.block.server.verification.session;

import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.VerificationConfig;
import com.hedera.block.server.verification.signature.SignatureVerifier;
import com.hedera.hapi.block.stream.output.BlockHeader;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public class BlockVerificationSessionFactory {

    private final VerificationConfig config;
    private final MetricsService metricsService;
    private final SignatureVerifier signatureVerifier;
    private final ExecutorService executorService;
    private final int hashCombineBatchSize;

    @Inject
    public BlockVerificationSessionFactory(
            @NonNull final VerificationConfig verificationConfig,
            @NonNull final MetricsService metricsService,
            @NonNull final SignatureVerifier signatureVerifier,
            @NonNull final ExecutorService executorService) {
        this.config = verificationConfig;
        this.metricsService = metricsService;
        this.signatureVerifier = signatureVerifier;
        this.executorService = executorService;
        this.hashCombineBatchSize = verificationConfig.hashCombineBatchSize();
    }

    public BlockVerificationSession createSession(@NonNull final BlockHeader blockHeader) {

        BlockVerificationSessionType type =
                BlockVerificationSessionType.valueOf(config.sessionType().name());

        return switch (type) {
            case ASYNC -> new BlockVerificationSessionAsync(
                    blockHeader, metricsService, signatureVerifier, executorService, hashCombineBatchSize);
            case SYNC -> new BlockVerificationSessionSync(blockHeader, metricsService, signatureVerifier);
            default -> throw new IllegalArgumentException("Unsupported session type: " + type);
        };
    }
}
