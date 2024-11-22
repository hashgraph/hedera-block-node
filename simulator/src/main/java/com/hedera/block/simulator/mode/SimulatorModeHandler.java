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

import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import java.io.IOException;

/**
 * The {@code SimulatorModeHandler} interface defines the contract for implementing different
 * working modes of the Block Stream Simulator. Implementations of this interface handle
 * specific behaviors for starting the simulator and managing the streaming process,
 * depending on the selected mode.
 *
 * <p>Examples of working modes include:
 * <ul>
 *   <li>Consumer mode: The simulator consumes data from the block stream.</li>
 *   <li>Publisher mode: The simulator publishes data to the block stream.</li>
 *   <li>Combined mode: The simulator handles both consuming and publishing.</li>
 * </ul>
 */
public interface SimulatorModeHandler {

    /**
     * Initializes the handler by setting up required resources and connections.
     * This method should be called before {@link #start()}.
     */
    void init();

    /**
     * Starts the simulator and initiates the streaming process according to the configured mode.
     * The behavior of this method depends on the specific working mode (consumer, publisher, or combined).
     *
     * @throws BlockSimulatorParsingException if an error occurs while parsing blocks
     * @throws IOException if an I/O error occurs during block streaming
     * @throws InterruptedException if the thread running the simulator is interrupted
     */
    void start() throws BlockSimulatorParsingException, IOException, InterruptedException;

    /**
     * Gracefully stops the handler, cleaning up resources and terminating any active streams.
     *
     * @throws InterruptedException if the shutdown process is interrupted
     */
    void stop() throws InterruptedException;
}
