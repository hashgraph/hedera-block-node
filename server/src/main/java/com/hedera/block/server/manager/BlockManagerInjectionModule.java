// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.verification.VerificationConfig;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Singleton;

@Module
public interface BlockManagerInjectionModule {

    /**
     * Provides a {@link BlockManager} instance.
     *
     * @param notifier the {@link Notifier} instance
     * @param persistenceStorageConfig the {@link PersistenceStorageConfig} instance
     * @param verificationConfig the {@link VerificationConfig} instance
     * @return a {@link BlockManager} instance
     */
    @Provides
    @Singleton
    static BlockManager provideBlockManager(
            @NonNull final Notifier notifier,
            @NonNull final PersistenceStorageConfig persistenceStorageConfig,
            @NonNull final VerificationConfig verificationConfig) {

        boolean skipPersistence = persistenceStorageConfig.type().equals(PersistenceStorageConfig.StorageType.NO_OP);
        boolean skipVerification = verificationConfig.type().equals(VerificationConfig.VerificationServiceType.NO_OP);

        return new BlockManagerImpl(notifier, skipPersistence | skipVerification);
    }
}
