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
import com.hedera.block.server.verification.session.BlockVerificationSessionType;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationInjectionModuleTest {

    @Mock
    BlockVerificationSessionFactory sessionFactory;

    @Mock
    MetricsService metricsService;

    @Test
    void testProvideBlockVerificationService_enabled() throws IOException {
        // given
        VerificationConfig verificationConfig = new VerificationConfig(true, BlockVerificationSessionType.ASYNC, 32);
        // when
        BlockVerificationService blockVerificationService = VerificationInjectionModule.provideBlockVerificationService(
                verificationConfig, metricsService, sessionFactory);
        // then
        Assertions.assertEquals(BlockVerificationServiceImpl.class, blockVerificationService.getClass());
    }

    @Test
    void testProvideBlockVerificationService_disabled() throws IOException {
        // given
        VerificationConfig verificationConfig = new VerificationConfig(false, BlockVerificationSessionType.ASYNC, 32);
        // when
        BlockVerificationService blockVerificationService = VerificationInjectionModule.provideBlockVerificationService(
                verificationConfig, metricsService, sessionFactory);
        // then
        Assertions.assertEquals(NoOpBlockVerificationService.class, blockVerificationService.getClass());
    }

    @Test
    void testProvidesBlockVerificationSessionFactory() {
        // given
        VerificationConfig verificationConfig = new VerificationConfig(true, BlockVerificationSessionType.ASYNC, 32);
        // when
        BlockVerificationService blockVerificationService = VerificationInjectionModule.provideBlockVerificationService(
                verificationConfig, metricsService, sessionFactory);
        // then
        Assertions.assertEquals(BlockVerificationServiceImpl.class, blockVerificationService.getClass());
    }
}
