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

package com.hedera.block.simulator;

import com.hedera.block.simulator.config.ConfigProvider;
import com.hedera.block.simulator.config.ConfigProviderImpl;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.swirlds.config.api.Configuration;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.System.Logger;
import javax.inject.Inject;

/** The BlockStreamSimulator class defines the simulator for the block stream. */
public class BlockStreamSimulator {
    private static final Logger LOGGER = System.getLogger(BlockStreamSimulator.class.getName());

    Configuration configuration;
    BlockStreamManager blockStreamManager;
    boolean isRunning = false;

    /**
     * Creates a new BlockStreamSimulator instance.
     *
     * @param configuration the configuration to be used by the block stream simulator
     * @param blockStreamManager the block stream manager to be used by the block stream simulator
     */
    @Inject
    public BlockStreamSimulator(
            @NonNull Configuration configuration, @NonNull BlockStreamManager blockStreamManager) {
        this.configuration = configuration;
        this.blockStreamManager = blockStreamManager;
    }

    /**
     * The main entry point for the block stream simulator.
     *
     * @param args the arguments to be passed to the block stream simulator
     */
    public static void main(String[] args) {

        LOGGER.log(Logger.Level.INFO, "Starting Block Stream Simulator");

        ConfigProvider configProvider = new ConfigProviderImpl();
        Configuration configuration = configProvider.getConfiguration();
        BlockStreamSimulatorInjectionComponent DIComponent =
                DaggerBlockStreamSimulatorInjectionComponent.factory().create(configuration);

        BlockStreamSimulator blockStreamSimulator = DIComponent.getBlockStreamSimulator();
        blockStreamSimulator.start();
    }

    /** Starts the block stream simulator. */
    public void start() {

        // use blockStreamManager to get block stream

        // use PublishStreamGrpcClient to stream it to the block-node.
        isRunning = true;
        LOGGER.log(Logger.Level.INFO, "Block Stream Simulator has started");
    }

    /**
     * Returns whether the block stream simulator is running.
     *
     * @return true if the block stream simulator is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /** Stops the block stream simulator. */
    public void stop() {
        isRunning = false;
        LOGGER.log(Logger.Level.INFO, "Block Stream Simulator has stopped");
    }
}
