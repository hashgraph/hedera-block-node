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

package com.hedera.block.simulator;

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.block.simulator.generator.BlockStreamManager;
import com.swirlds.config.api.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockStreamSimulatorAppTest {

    @Mock private Configuration configuration;

    @Mock private BlockStreamManager blockStreamManager;

    @InjectMocks private BlockStreamSimulatorApp blockStreamSimulator;

    @BeforeEach
    void setUp() {
        blockStreamSimulator = new BlockStreamSimulatorApp(configuration, blockStreamManager);
    }

    @AfterEach
    void tearDown() {
        blockStreamSimulator.stop();
    }

    @Test
    void start_logsStartedMessage() {
        blockStreamSimulator.start();
        assertTrue(blockStreamSimulator.isRunning());
    }

    @Test
    void stop_doesNotThrowException() {
        assertDoesNotThrow(() -> blockStreamSimulator.stop());
    }
}
