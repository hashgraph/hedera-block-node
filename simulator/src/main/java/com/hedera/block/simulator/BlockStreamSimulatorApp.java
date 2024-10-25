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

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.SimulatorMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.block.simulator.mode.CombinedModeHandler;
import com.hedera.block.simulator.mode.ConsumerModeHandler;
import com.hedera.block.simulator.mode.PublisherModeHandler;
import com.hedera.block.simulator.mode.SimulatorModeHandler;
import com.swirlds.config.api.Configuration;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/** BlockStream Simulator App */
public class BlockStreamSimulatorApp {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final BlockStreamManager blockStreamManager;
    private final PublishStreamGrpcClient publishStreamGrpcClient;
    private final BlockStreamConfig blockStreamConfig;
    private final SimulatorModeHandler simulatorModeHandler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Creates a new BlockStreamSimulatorApp instance.
     *
     * @param configuration the configuration to be used by the block stream simulator
     * @param blockStreamManager the block stream manager to be used by the block stream simulator
     * @param publishStreamGrpcClient the gRPC client to be used by the block stream simulator
     */
    @Inject
    public BlockStreamSimulatorApp(
            @NonNull Configuration configuration,
            @NonNull BlockStreamManager blockStreamManager,
            @NonNull PublishStreamGrpcClient publishStreamGrpcClient) {
        this.blockStreamManager = blockStreamManager;
        this.publishStreamGrpcClient = publishStreamGrpcClient;

        blockStreamConfig = configuration.getConfigData(BlockStreamConfig.class);

        SimulatorMode simulatorMode = blockStreamConfig.simulatorMode();
        switch (simulatorMode) {
            case PUBLISHER:
                simulatorModeHandler =
                        new PublisherModeHandler(blockStreamConfig, publishStreamGrpcClient);
                break;
            case CONSUMER:
                simulatorModeHandler = new ConsumerModeHandler(blockStreamConfig);
                break;
            case BOTH:
                simulatorModeHandler = new CombinedModeHandler(blockStreamConfig);
                break;
            default:
                throw new IllegalArgumentException("Unknown SimulatorMode: " + simulatorMode);
        }
    }

    /**
     * Starts the block stream simulator.
     *
     * @throws InterruptedException if the thread is interrupted
     * @throws BlockSimulatorParsingException if a parse error occurs
     * @throws IOException if an I/O error occurs
     */
    public void start() throws InterruptedException, BlockSimulatorParsingException, IOException {
        LOGGER.log(
                System.Logger.Level.INFO,
                "Block Stream Simulator started initializing components...");
        publishStreamGrpcClient.init();
        isRunning.set(true);

        simulatorModeHandler.start(this.blockStreamManager);
    }

    /**
     * Returns whether the block stream simulator is running.
     *
     * @return true if the block stream simulator is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /** Stops the Block Stream Simulator and closes off all grpc channels. */
    public void stop() {
        publishStreamGrpcClient.shutdown();
        isRunning.set(false);
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has stopped");
    }
}
