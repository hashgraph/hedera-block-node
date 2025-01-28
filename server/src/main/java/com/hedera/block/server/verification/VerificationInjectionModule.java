// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.verification;

import com.hedera.block.server.ack.AckHandler;
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
            @NonNull final BlockVerificationSessionFactory blockVerificationSessionFactory,
            @NonNull final AckHandler ackHandler) {
        if (verificationConfig.type() == VerificationConfig.VerificationServiceType.NO_OP) {
            return new NoOpBlockVerificationService();
        } else {
            return new BlockVerificationServiceImpl(metricsService, blockVerificationSessionFactory, ackHandler);
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
