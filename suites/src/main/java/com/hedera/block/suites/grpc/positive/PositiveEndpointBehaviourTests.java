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

package com.hedera.block.suites.grpc.positive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.block.simulator.BlockStreamSimulatorApp;
import com.hedera.block.simulator.config.data.StreamStatus;
import com.hedera.block.suites.BaseSuite;
import java.io.IOException;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for verifying the positive scenarios for server availability, specifically related to
 * the gRPC server. This class contains tests to check that the gRPC server and the exposed endpoints are working as expected
 * and returning correct responses on valid requests.
 *
 * <p>Inherits from {@link BaseSuite} to reuse the container setup and teardown logic for the Block
 * Node.
 */
@DisplayName("Positive Endpoint Behaviour Tests")
public class PositiveEndpointBehaviourTests extends BaseSuite {

    private BlockStreamSimulatorApp blockStreamSimulatorApp;

    private Future<?> simulatorThread;

    @AfterEach
    void teardownEnvironment() {
        if (simulatorThread != null && !simulatorThread.isCancelled()) {
            simulatorThread.cancel(true);
        }
    }
    /** Default constructor for the {@link PositiveEndpointBehaviourTests} class. */
    public PositiveEndpointBehaviourTests() {}

    @Test
    void verifyPublishBlockStreamEndpoint() throws IOException, InterruptedException {
        blockStreamSimulatorApp = createBlockSimulator();
        simulatorThread = startSimulatorInThread(blockStreamSimulatorApp);
        Thread.sleep(5000);
        blockStreamSimulatorApp.stop();
        StreamStatus streamStatus = blockStreamSimulatorApp.getStreamStatus();
        assertTrue(streamStatus.publishedBlocks() > 0);
        assertEquals(
                streamStatus.publishedBlocks(),
                streamStatus.lastKnownPublisherStatuses().size());

        // Verify each status contains the word "acknowledgement"
        streamStatus
                .lastKnownPublisherStatuses()
                .forEach(status -> assertTrue(status.toLowerCase().contains("acknowledgement")));
    }
}
