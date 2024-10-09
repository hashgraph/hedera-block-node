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

package com.hedera.block.suites.persistance.positive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.block.simulator.BlockStreamSimulatorApp;
import com.hedera.block.simulator.exception.BlockSimulatorException;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.suites.BaseSuite;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.Container;

@DisplayName("Positive Data Persistence Tests")
public class PositiveDataPersistenceTests extends BaseSuite {
    /** Default constructor for the {@link PositiveDataPersistenceTests} class. */
    public PositiveDataPersistenceTests() {}

    /**
     * Verifies that block data is saved in the correct directory by comparing the count of saved
     * blocks before and after running the simulator. The test asserts that the number of saved
     * blocks increases after the simulator runs.
     *
     * @throws IOException if an I/O error occurs during execution in the container
     * @throws InterruptedException if the thread is interrupted while sleeping or executing
     *     commands
     * @throws BlockSimulatorParsingException if there is an error parsing the block simulator data
     * @throws BlockSimulatorException if an error occurs within the block simulator
     */
    public void verifyBlockDataSavedInCorrectDirectory()
            throws IOException,
                    InterruptedException,
                    BlockSimulatorParsingException,
                    BlockSimulatorException {
        String[] getBlocksCommand = new String[] {"ls", "data", "-1"};
        String savedBlocksFolderBefore = getContainerCommandResult(getBlocksCommand);
        int savedBlocksCountBefore = getSavedBlocksCount(savedBlocksFolderBefore);
        blockStreamSimulatorApp = new BlockStreamSimulatorApp();
        blockStreamSimulatorApp.start();
        Thread.sleep(5000);
        blockStreamSimulatorApp.stop();

        String savedBlocksFolderAfter = getContainerCommandResult(getBlocksCommand);
        int savedBlocksCountAfter = getSavedBlocksCount(savedBlocksFolderBefore);

        assertTrue(savedBlocksFolderBefore.isEmpty());
        assertFalse(savedBlocksFolderAfter.isEmpty());
        assertTrue(savedBlocksCountAfter > savedBlocksCountBefore);
    }

    private int getSavedBlocksCount(String blocksFolders) {
        String[] blocksArray = blocksFolders.split("\\n");
        return blocksArray.length;
    }

    private String getContainerCommandResult(String[] command)
            throws IOException, InterruptedException {
        Container.ExecResult result = blockNodeContainer.execInContainer(command);
        return result.getStdout();
    }
}
