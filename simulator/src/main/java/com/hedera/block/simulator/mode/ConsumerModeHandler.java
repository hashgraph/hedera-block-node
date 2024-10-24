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

import static java.util.Objects.requireNonNull;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.generator.BlockStreamManager;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The {@code ConsumerModeHandler} class implements the {@link SimulatorModeHandler} interface
 * and provides the behavior for a mode where only consumption of block data
 * occurs.
 *
 * <p>This mode handles single operation in the block streaming process, utilizing the
 * {@link BlockStreamConfig} for configuration settings. It is designed for scenarios where
 * the simulator needs to handle the consumption of blocks.
 *
 * <p>For now, the actual start behavior is not implemented, as indicated by the
 * {@link UnsupportedOperationException}.
 */
public class ConsumerModeHandler implements SimulatorModeHandler {

    private final BlockStreamConfig blockStreamConfig;

    /**
     * Constructs a new {@code ConsumerModeHandler} with the specified block stream configuration.
     *
     * @param blockStreamConfig the configuration data for managing block streams
     */
    public ConsumerModeHandler(@NonNull final BlockStreamConfig blockStreamConfig) {
        requireNonNull(blockStreamConfig);
        this.blockStreamConfig = blockStreamConfig;
    }

    /**
     * Starts the simulator and initiate streaming, depending on the working mode.
     */
    @Override
    public void start(@NonNull BlockStreamManager blockStreamManager) {
        throw new UnsupportedOperationException();
    }
}
