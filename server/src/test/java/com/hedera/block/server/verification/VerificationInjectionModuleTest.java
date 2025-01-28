// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.verification;

import static com.hedera.block.server.verification.VerificationConfig.VerificationServiceType.NO_OP;
import static com.hedera.block.server.verification.session.BlockVerificationSessionType.ASYNC;

import com.hedera.block.server.ack.AckHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.service.BlockVerificationService;
import com.hedera.block.server.verification.service.BlockVerificationServiceImpl;
import com.hedera.block.server.verification.service.NoOpBlockVerificationService;
import com.hedera.block.server.verification.session.BlockVerificationSessionFactory;
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

    @Mock
    AckHandler ackHandlerMock;

    @Test
    void testProvideBlockVerificationService_enabled() throws IOException {
        // given
        VerificationConfig verificationConfig = new VerificationConfig(null, ASYNC, 32);
        // when
        BlockVerificationService blockVerificationService = VerificationInjectionModule.provideBlockVerificationService(
                verificationConfig, metricsService, sessionFactory, ackHandlerMock);
        // then
        Assertions.assertEquals(BlockVerificationServiceImpl.class, blockVerificationService.getClass());
    }

    @Test
    void testProvideBlockVerificationService_no_op() throws IOException {
        // given
        VerificationConfig verificationConfig = new VerificationConfig(NO_OP, ASYNC, 32);
        // when
        BlockVerificationService blockVerificationService = VerificationInjectionModule.provideBlockVerificationService(
                verificationConfig, metricsService, sessionFactory, ackHandlerMock);
        // then
        Assertions.assertEquals(NoOpBlockVerificationService.class, blockVerificationService.getClass());
    }

    @Test
    void testProvidesBlockVerificationSessionFactory() {
        // given
        VerificationConfig verificationConfig = new VerificationConfig(null, ASYNC, 32);
        // when
        BlockVerificationService blockVerificationService = VerificationInjectionModule.provideBlockVerificationService(
                verificationConfig, metricsService, sessionFactory, ackHandlerMock);
        // then
        Assertions.assertEquals(BlockVerificationServiceImpl.class, blockVerificationService.getClass());
    }
}
