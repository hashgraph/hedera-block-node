/*
 * Copyright (C) 2024-2025 Hedera Hashgraph, LLC
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

import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.service.BlockVerificationService;
import com.hedera.block.server.verification.service.BlockVerificationServiceImpl;
import com.hedera.block.server.verification.service.NoOpBlockVerificationService;
import com.hedera.block.server.verification.session.BlockVerificationSessionFactory;
import com.hedera.block.server.verification.signature.SignatureVerifier;
import com.hedera.block.server.verification.signature.SignatureVerifierDummy;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import javax.inject.Singleton;

/**
 * The module used to inject the verification service and signature verifier into the application.
 */
@Module
public interface VerificationInjectionModule {

    /**
     * Provides the signature verifier.
     *
     * @param signatureVerifier the signature verifier to be used
     * @return the signature verifier
     */
    @Binds
    @Singleton
    SignatureVerifier bindSignatureVerifier(SignatureVerifierDummy signatureVerifier);

    /**
     * Provides the block verification service.
     *
     * @param verificationConfig the verification configuration to be used
     * @param metricsService the metrics service to be used
     * @param blockVerificationSessionFactory the block verification session factory to be used
     * @return the block verification service
     */
    @Provides
    @Singleton
    static BlockVerificationService provideBlockVerificationService(
            @NonNull final VerificationConfig verificationConfig,
            @NonNull final MetricsService metricsService,
            @NonNull final BlockVerificationSessionFactory blockVerificationSessionFactory) {
        if (verificationConfig.enabled()) {
            return new BlockVerificationServiceImpl(metricsService, blockVerificationSessionFactory);
        } else {
            return new NoOpBlockVerificationService();
        }
    }

    /**
     * Provides the block verification session factory.
     * Uses the common fork join pool for the executor service, of the concurrent hashing tree for now.
     *
     * @param verificationConfig the verification configuration to be used
     * @param metricsService the metrics service to be used
     * @param signatureVerifier the signature verifier to be used
     * @return the block verification session factory
     */
    @Provides
    @Singleton
    static BlockVerificationSessionFactory provideBlockVerificationSessionFactory(
            @NonNull final VerificationConfig verificationConfig,
            @NonNull final MetricsService metricsService,
            @NonNull final SignatureVerifier signatureVerifier) {
        final ExecutorService executorService = ForkJoinPool.commonPool();
        return new BlockVerificationSessionFactory(
                verificationConfig, metricsService, signatureVerifier, executorService);
    }
}
