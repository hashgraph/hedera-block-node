/*
 * Copyright (C) 2022-2024 Hedera Hashgraph, LLC
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

import com.hedera.block.suites.BaseSuite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for verifying the positive scenarios for server availability, specifically related to
 * the gRPC server. This class contains tests to check that the gRPC server starts successfully and
 * listens on the correct port.
 *
 * <p>Inherits from {@link BaseSuite} to reuse the container setup and teardown logic for the Block
 * Node.
 */
@DisplayName("Positive Server Availability Tests")
public class PositiveServerAvailabilityTests extends BaseSuite {

    /** Default constructor for the {@link PositiveServerAvailabilityTests} class. */
    public PositiveServerAvailabilityTests() {}

    /**
     * Test to verify that the gRPC server starts successfully.
     *
     * <p>The test checks if the Block Node container is  running and marked as healthy.
     */
    @Test
    public void verifyGrpcServerStartsSuccessfully() {
        assertTrue(blockNodeContainer.isRunning(), "Block Node container should be running.");
        assertTrue(blockNodeContainer.isHealthy(), "Block Node container should be healthy.");
    }

    /**
     * Test to verify that the gRPC server is listening on the correct port.
     *
     * <p>The test asserts that the container is running, exposes exactly one port, and that the
     * exposed port matches the expected gRPC server port.
     */
    @Test
    public void verifyGrpcServerListeningOnCorrectPort() {
        assertTrue(blockNodeContainer.isRunning(), "Block Node container should be running.");
        assertEquals(1, blockNodeContainer.getExposedPorts().size(), "There should be exactly one exposed port.");
        assertEquals(
                blockNodePort,
                blockNodeContainer.getExposedPorts().getFirst(),
                "The exposed port should match the expected gRPC server port.");
    }
}
