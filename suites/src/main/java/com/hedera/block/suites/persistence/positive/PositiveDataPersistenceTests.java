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

import com.hedera.block.simulator.BlockStreamSimulatorInjectionComponent;
import com.hedera.block.simulator.DaggerBlockStreamSimulatorInjectionComponent;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.suites.BaseSuite;
import com.swirlds.config.api.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Positive Data Persistence Tests")
public class PositiveDataPersistenceTests extends BaseSuite {
    private final String[] GET_BLOCKS_COMMAND = new String[] {"ls", "data", "-1"};
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
     * @throws IOException if an error occurs within the block simulator
     */
    @Test
    public void verifyBlockDataSavedInCorrectDirectory()
            throws
            InterruptedException,
            BlockSimulatorParsingException,
            IOException {
        String savedBlocksFolderBefore = getContainerCommandResult(GET_BLOCKS_COMMAND);
        int savedBlocksCountBefore = getSavedBlocksCount(savedBlocksFolderBefore);
        BlockStreamSimulatorInjectionComponent DIComponent =
                DaggerBlockStreamSimulatorInjectionComponent.factory()
                        .create(loadDefaultConfiguration());
        blockStreamSimulatorApp = DIComponent.getBlockStreamSimulatorApp();
        blockStreamSimulatorApp.start();

        Thread.sleep(10000);

        String savedBlocksFolderAfter = getContainerCommandResult(GET_BLOCKS_COMMAND);
        int savedBlocksCountAfter = getSavedBlocksCount(savedBlocksFolderAfter);

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
