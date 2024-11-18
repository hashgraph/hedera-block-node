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
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The {@code CombinedModeHandler} class implements the {@link SimulatorModeHandler} interface
 * and provides the behavior for a mode where both consuming and publishing of block data
 * occur simultaneously.
 *
 * <p>This mode handles dual operations in the block streaming process, utilizing the
 * {@link BlockStreamConfig} for configuration settings. It is designed for scenarios where
 * the simulator needs to handle both the consumption and publication of blocks in parallel.
 *
 * <p>For now, the actual start behavior is not implemented, as indicated by the
 * {@link UnsupportedOperationException}.
 */
public class CombinedModeHandler implements SimulatorModeHandler {
    private final BlockStreamConfig blockStreamConfig;

    /**
     * Constructs a new {@code CombinedModeHandler} with the specified block stream configuration.
     *
     * @param blockStreamConfig the configuration data for managing block streams
     */
    public CombinedModeHandler(@NonNull final BlockStreamConfig blockStreamConfig) {
        this.blockStreamConfig = requireNonNull(blockStreamConfig);
    }

    /**
     * Starts the simulator in combined mode, handling both consumption and publication
     * of block stream. However, this method is currently not implemented, and will throw
     * an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException as the method is not yet implemented
     */
    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    /**
     * Stops the handler and manager from streaming.
     */
    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }
}
