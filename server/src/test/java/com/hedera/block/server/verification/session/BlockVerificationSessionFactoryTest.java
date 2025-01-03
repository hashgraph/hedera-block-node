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

package com.hedera.block.server.verification.session;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.VerificationConfig;
import com.hedera.block.server.verification.signature.SignatureVerifier;
import com.hedera.hapi.block.stream.output.BlockHeader;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BlockVerificationSessionFactoryTest {

    @Mock
    private MetricsService mockMetricsService;

    @Mock
    private SignatureVerifier mockSignatureVerifier;

    @Mock
    private ExecutorService mockExecutorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Default configuration stubs

    }

    @Test
    void createSession_whenSessionTypeIsAsync_returnsBlockVerificationSessionAsync() {
        // Given
        VerificationConfig config = new VerificationConfig(true, BlockVerificationSessionType.ASYNC, 32);
        BlockHeader blockHeader = BlockHeader.newBuilder().number(1L).build();

        BlockVerificationSessionFactory sessionFactory = new BlockVerificationSessionFactory(
                config, mockMetricsService, mockSignatureVerifier, mockExecutorService);

        // When
        var session = sessionFactory.createSession(blockHeader);

        // Then
        assertNotNull(session, "Session should not be null");
        assertInstanceOf(
                BlockVerificationSessionAsync.class,
                session,
                "Session should be an instance of BlockVerificationSessionAsync");
    }

    @Test
    void createSession_whenSessionTypeIsSync_returnsBlockVerificationSessionSync() {
        // Given
        VerificationConfig config = new VerificationConfig(true, BlockVerificationSessionType.SYNC, 32);
        BlockHeader blockHeader = BlockHeader.newBuilder().number(1L).build();

        BlockVerificationSessionFactory sessionFactory = new BlockVerificationSessionFactory(
                config, mockMetricsService, mockSignatureVerifier, mockExecutorService);

        // When
        var session = sessionFactory.createSession(blockHeader);

        // Then
        assertNotNull(session, "Session should not be null");
        assertInstanceOf(
                BlockVerificationSessionSync.class,
                session,
                "Session should be an instance of BlockVerificationSessionSync");
    }
}
