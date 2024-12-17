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

import com.hedera.block.server.metrics.MetricsService;
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

@Module
public interface VerificationInjectionModule {

    @Provides
    static ExecutorService provideExecutorService() {
        return ForkJoinPool.commonPool();
    }

    @Binds
    @Singleton
    SignatureVerifier bindSignatureVerifier(SignatureVerifierDummy signatureVerifier);

    @Provides
    @Singleton
    static BlockVerificationService provideBlockVerificationService(
            @NonNull final VerificationConfig verificationConfig,
            @NonNull final MetricsService metricsService,
            @NonNull final BlockVerificationSessionFactory blockVerificationSessionFactory) {
        if (verificationConfig.enabled()) {
            return new BlockVerificationServiceImpl(metricsService, blockVerificationSessionFactory);
        } else {
            return new BlockVerificationServiceNoOp();
        }
    }
}
