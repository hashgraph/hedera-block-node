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

package com.hedera.block.suites;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * BaseSuite is an abstract class that provides common setup and teardown functionality for test
 * suites using Testcontainers to manage a Docker container for the Block Node server.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Starting a Docker container running the Block Node server with a specified version.
 *   <li>Stopping the container after tests have been executed.
 * </ul>
 *
 * <p>The Block Node server version is retrieved dynamically from an environment file (.env).
 */
public abstract class BaseSuite {

    /** Container running the Block Node server */
    protected static GenericContainer<?> blockNodeContainer;

    /**
     * Default constructor for the BaseSuite class.
     *
     * <p>This constructor can be used by subclasses or the testing framework to initialize the
     * BaseSuite. It does not perform any additional setup.
     */
    public BaseSuite() {
        // No additional setup required
    }

    /**
     * Setup method to be executed before all tests.
     *
     * <p>This method initializes the Block Node server container using Testcontainers.
     */
    @BeforeAll
    public static void setup() {
        String blockNodeVersion = BaseSuite.getBlockNodeVersion();
        blockNodeContainer =
                new GenericContainer<>(
                                DockerImageName.parse("block-node-server:" + blockNodeVersion))
                        .withExposedPorts(8080)
                        .withEnv("VERSION", blockNodeVersion)
                        .waitingFor(Wait.forListeningPort());
        blockNodeContainer.start();
    }

    /**
     * Teardown method to be executed after all tests.
     *
     * <p>This method stops the Block Node server container if it is running. It ensures that
     * resources are cleaned up after the test suite execution is complete.
     */
    @AfterAll
    public static void teardown() {
        if (blockNodeContainer != null) {
            blockNodeContainer.stop();
        }
    }

    /**
     * Retrieves the Block Node server version from the .env file.
     *
     * <p>This method loads the .env file from the "../server/docker" directory and extracts the
     * value of the "VERSION" environment variable, which represents the version of the Block Node
     * server to be used in the container.
     *
     * @return the version of the Block Node server as a string
     */
    private static String getBlockNodeVersion() {
        Dotenv dotenv = Dotenv.configure().directory("../server/docker").filename(".env").load();

        return dotenv.get("VERSION");
    }
}
