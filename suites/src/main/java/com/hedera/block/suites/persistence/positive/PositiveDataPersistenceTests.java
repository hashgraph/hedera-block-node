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

package com.hedera.block.suites.persistence.positive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.block.simulator.BlockStreamSimulatorApp;
import com.hedera.block.suites.BaseSuite;
import java.io.IOException;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

/**
 * Test class for verifying the positive scenarios for data persistence.
 *
 * <p>Inherits from {@link BaseSuite} to reuse the container setup and teardown logic for the Block
 * Node.
 */
@DisplayName("Positive Data Persistence Tests")
public class PositiveDataPersistenceTests extends BaseSuite {
    // @todo(#371) - the default life/archive root path must be absolute starting from /opt
    private final String[] GET_BLOCKS_COMMAND = new String[] {"ls", "hashgraph/blocknode/data/live", "-1"};

    private Future<?> simulatorThread;

    /** Default constructor for the {@link PositiveDataPersistenceTests} class. */
    public PositiveDataPersistenceTests() {}

    @AfterEach
    void teardownEnvironment() {
        if (simulatorThread != null && !simulatorThread.isCancelled()) {
            simulatorThread.cancel(true);
        }
    }

    /**
     * Verifies that block data is saved in the correct directory by comparing the count of saved
     * blocks before and after running the simulator. The test asserts that the number of saved
     * blocks increases after the simulator runs.
     *
     * @throws IOException if an I/O error occurs during execution in the container
     * @throws InterruptedException if the thread is interrupted while sleeping or executing
     *     commands
     */
    @Test
    public void verifyBlockDataSavedInCorrectDirectory() throws InterruptedException, IOException {
        String savedBlocksFolderBefore = getContainerCommandResult(GET_BLOCKS_COMMAND);
        int savedBlocksCountBefore = getSavedBlocksCount(savedBlocksFolderBefore);

        final BlockStreamSimulatorApp blockStreamSimulatorApp = createBlockSimulator();
        simulatorThread = startSimulatorInThread(blockStreamSimulatorApp);
        Thread.sleep(5000);
        blockStreamSimulatorApp.stop();

        String savedBlocksFolderAfter = getContainerCommandResult(GET_BLOCKS_COMMAND);
        int savedBlocksCountAfter = getSavedBlocksCount(savedBlocksFolderAfter);

        assertTrue(savedBlocksFolderBefore.isEmpty());
        assertFalse(savedBlocksFolderAfter.isEmpty());
        assertTrue(savedBlocksCountAfter > savedBlocksCountBefore);
        assertTrue(blockStreamSimulatorApp.getStreamStatus().publishedBlocks() > 0);
    }

    private int getSavedBlocksCount(String blocksFolders) {
        String[] blocksArray = blocksFolders.split("\\n");
        return blocksArray.length;
    }

    private String getContainerCommandResult(String[] command) throws IOException, InterruptedException {
        Container.ExecResult result = blockNodeContainer.execInContainer(command);
        return result.getStdout();
    }
}
