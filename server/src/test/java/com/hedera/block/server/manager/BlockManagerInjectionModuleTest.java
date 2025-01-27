// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.verification.VerificationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockManagerInjectionModuleTest {

    @Mock
    private ServiceStatus serviceStatus;

    @Test
    void testProvideBlockManager() {
        // given
        Notifier notifier = mock(Notifier.class);
        PersistenceStorageConfig persistenceStorageConfig = new PersistenceStorageConfig(
                "",
                "",
                PersistenceStorageConfig.StorageType.BLOCK_AS_LOCAL_FILE,
                PersistenceStorageConfig.CompressionType.NONE,
                0);
        VerificationConfig verificationConfig = mock(VerificationConfig.class);
        when(verificationConfig.type()).thenReturn(VerificationConfig.VerificationServiceType.PRODUCTION);

        // when
        BlockManager blockManager = BlockManagerInjectionModule.provideBlockManager(
                notifier, persistenceStorageConfig, verificationConfig, serviceStatus);

        // then
        // BlockManagerImpl is the default and only implementation
        // so we should get an instance of it
        assertEquals(BlockManagerImpl.class, blockManager.getClass());
    }
}
