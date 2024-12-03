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

import static com.hedera.block.common.constants.StringsConstants.LOGGING_PROPERTIES;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.StreamStatus;
import com.hedera.block.simulator.config.types.SimulatorMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.ConsumerStreamGrpcClient;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.mode.CombinedModeHandler;
import com.hedera.block.simulator.mode.ConsumerModeHandler;
import com.hedera.block.simulator.mode.PublisherModeHandler;
import com.hedera.block.simulator.mode.SimulatorModeHandler;
import com.swirlds.config.api.Configuration;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.inject.Inject;

/**
 * The BlockStream Simulator Application manages the lifecycle and coordination
 * of block streaming
 * operations. It supports different modes of operation including publishing,
 * consuming, or both
 * simultaneously.
 *
 * <p>
 * This class serves as the main entry point for the simulator, handling
 * initialization,
 * execution, and shutdown of streaming operations based on the configured mode.
 */
public class BlockStreamSimulatorApp {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // Service dependencies
    private final PublishStreamGrpcClient publishStreamGrpcClient;
    private final ConsumerStreamGrpcClient consumerStreamGrpcClient;
    private final SimulatorModeHandler simulatorModeHandler;

    // State
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Creates a new BlockStreamSimulatorApp instance with the specified
     * dependencies.
     *
     * @param configuration            The configuration to be used by the block
     *                                 stream simulator
     * @param blockStreamManager       The manager responsible for block stream
     *                                 generation
     * @param publishStreamGrpcClient  The gRPC client for publishing blocks
     * @param consumerStreamGrpcClient The gRPC client for consuming blocks
     * @param metricsService           The service for recording metrics
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if an unknown simulator mode is configured
     */
    @Inject
    public BlockStreamSimulatorApp(
            @NonNull Configuration configuration,
            @NonNull BlockStreamManager blockStreamManager,
            @NonNull PublishStreamGrpcClient publishStreamGrpcClient,
            @NonNull ConsumerStreamGrpcClient consumerStreamGrpcClient,
            @NonNull MetricsService metricsService) {

        requireNonNull(configuration);
        requireNonNull(blockStreamManager);
        loadLoggingProperties();

        this.publishStreamGrpcClient = requireNonNull(publishStreamGrpcClient);
        this.consumerStreamGrpcClient = requireNonNull(consumerStreamGrpcClient);

        // Initialize the appropriate mode handler based on configuration
        final BlockStreamConfig blockStreamConfig =
                requireNonNull(configuration.getConfigData(BlockStreamConfig.class));
        // @todo(386) Load simulator mode using dagger
        final SimulatorMode simulatorMode = blockStreamConfig.simulatorMode();
        this.simulatorModeHandler = switch (simulatorMode) {
            case PUBLISHER -> new PublisherModeHandler(
                    blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);
            case CONSUMER -> new ConsumerModeHandler(consumerStreamGrpcClient);
            case BOTH -> new CombinedModeHandler();
        };
    }

    /**
     * Initializes and starts the block stream simulator in the configured mode.
     * This method initializes all components and begins the streaming process.
     *
     * @throws InterruptedException           if the streaming process is
     *                                        interrupted
     * @throws BlockSimulatorParsingException if a block parsing error occurs
     * @throws IOException                    if an I/O error occurs during
     *                                        streaming
     */
    public void start() throws InterruptedException, BlockSimulatorParsingException, IOException {
        LOGGER.log(INFO, "Block Stream Simulator started initializing components...");
        simulatorModeHandler.init();

        isRunning.set(true);

        simulatorModeHandler.start();
    }

    /**
     * Checks if the simulator is currently running.
     *
     * @return true if the simulator is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Gracefully stops the simulator and closes all gRPC channels.
     * This method ensures proper cleanup of resources and termination of streaming
     * operations.
     *
     * @throws InterruptedException if the shutdown process is interrupted
     */
    public void stop() throws InterruptedException {
        // @todo(322) Add real lifecycle to the simulator
        simulatorModeHandler.stop();
        isRunning.set(false);

        LOGGER.log(INFO, "Block Stream Simulator has stopped");
    }

    /**
     * Retrieves the current status of both publishing and consuming streams.
     * This method provides information about the number of blocks processed and the
     * last known status of both publisher and consumer operations.
     *
     * @return A StreamStatus object containing current metrics and status
     *         information
     */
    public StreamStatus getStreamStatus() {
        return StreamStatus.builder()
                .publishedBlocks(publishStreamGrpcClient.getPublishedBlocks())
                .consumedBlocks(consumerStreamGrpcClient.getConsumedBlocks())
                .lastKnownPublisherStatuses(publishStreamGrpcClient.getLastKnownStatuses())
                .lastKnownConsumersStatuses(consumerStreamGrpcClient.getLastKnownStatuses())
                .build();
    }

    private void loadLoggingProperties() {
        final LogManager logManager = LogManager.getLogManager();
        try (InputStream is = BlockStreamSimulator.class.getClassLoader().getResourceAsStream(LOGGING_PROPERTIES)) {
            logManager.readConfiguration(is);
        } catch (IOException | NullPointerException e) {
            logManager.reset();
            Logger rootLogger = logManager.getLogger("");
            ConsoleHandler consoleHandler = new ConsoleHandler();

            consoleHandler.setLevel(Level.INFO);
            rootLogger.setLevel(Level.INFO);
            rootLogger.addHandler(consoleHandler);

            LOGGER.log(
                    ERROR,
                    "Loading Logging Configuration failed, continuing with default. Error is: %s"
                            .formatted(e.getMessage()));
        }
    }
}
