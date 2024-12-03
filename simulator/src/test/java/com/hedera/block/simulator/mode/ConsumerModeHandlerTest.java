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

package com.hedera.block.simulator.mode;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hedera.block.simulator.grpc.ConsumerStreamGrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsumerModeHandlerTest {

    private ConsumerStreamGrpcClient consumerStreamGrpcClient;
    private ConsumerModeHandler consumerModeHandler;

    @BeforeEach
    void setUp() {
        consumerStreamGrpcClient = mock(ConsumerStreamGrpcClient.class);

        consumerModeHandler = new ConsumerModeHandler(consumerStreamGrpcClient);
    }

    @Test
    void testConstructorWithNullArguments() {
        assertThrows(NullPointerException.class, () -> new ConsumerModeHandler(null));
    }

    @Test
    void testInit() {
        consumerModeHandler.init();

        verify(consumerStreamGrpcClient).init();
    }

    @Test
    void testStart() throws InterruptedException {
        consumerModeHandler.start();
        verify(consumerStreamGrpcClient).requestBlocks(0, 0);
    }

    @Test
    void testStart_throwsExceptionDuringConsuming() throws InterruptedException {
        consumerModeHandler.start();

        doThrow(new InterruptedException("Test exception"))
                .when(consumerStreamGrpcClient)
                .requestBlocks(0, 0);
        assertThrows(InterruptedException.class, () -> consumerModeHandler.start());
    }

    @Test
    void testStop() throws InterruptedException {
        consumerModeHandler.stop();

        verify(consumerStreamGrpcClient).completeStreaming();
    }

    @Test
    void testStop_throwsExceptionDuringCompleteStreaming() throws InterruptedException {
        consumerModeHandler.stop();
        doThrow(new InterruptedException("Test exception"))
                .when(consumerStreamGrpcClient)
                .completeStreaming();

        assertThrows(InterruptedException.class, () -> consumerModeHandler.stop());
    }
}
