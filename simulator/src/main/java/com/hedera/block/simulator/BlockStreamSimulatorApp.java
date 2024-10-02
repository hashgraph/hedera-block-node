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
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.hapi.block.stream.BlockItem;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;

/** BlockStream Simulator App */
public class BlockStreamSimulatorApp {

    private static final System.Logger LOGGER =
            System.getLogger(BlockStreamSimulatorApp.class.getName());

    private final Configuration configuration;
    private BlockStreamManager blockStreamManager;
    private PublishStreamGrpcClient publishStreamGrpcClient;
    private BlockStreamConfig blockStreamConfig;

    private final int delayBetweenBlockItems;

    private boolean isRunning = false;

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
        this.configuration = configuration;
        this.blockStreamManager = blockStreamManager;
        this.publishStreamGrpcClient = publishStreamGrpcClient;

        blockStreamConfig = configuration.getConfigData(BlockStreamConfig.class);

        delayBetweenBlockItems = blockStreamConfig.delayBetweenBlockItems();
    }

    /**
     * Constructs a new {@code BlockStreamSimulatorApp} with the specified configuration.
     *
     * @param configuration the configuration to use for the simulator app
     */
    public BlockStreamSimulatorApp(Configuration configuration) {
        this.configuration = configuration;
        initilizeDependencies();
        initializeConfig();

        this.delayBetweenBlockItems = blockStreamConfig.delayBetweenBlockItems();
    }

    /**
     * Constructs a new {@code BlockStreamSimulatorApp} with the default configuration.
     *
     * @throws IOException if an I/O error occurs while loading the default configuration
     */
    public BlockStreamSimulatorApp() throws IOException {
        this.configuration = loadDefaultConfiguration();
        initilizeDependencies();
        initializeConfig();

        this.delayBetweenBlockItems = blockStreamConfig.delayBetweenBlockItems();
    }

    /**
     * Initializes the configuration by retrieving the {@code BlockStreamConfig} from the current
     * configuration.
     */
    private void initializeConfig() {
        this.blockStreamConfig = configuration.getConfigData(BlockStreamConfig.class);
    }

    /**
     * Initializes the dependencies required by the simulator app using Dagger dependency injection.
     */
    private void initilizeDependencies() {
        BlockStreamSimulatorInjectionComponent DIComponent =
                DaggerBlockStreamSimulatorInjectionComponent.factory().create(configuration);

        this.blockStreamManager = DIComponent.getBlockStreamManager();
        this.publishStreamGrpcClient = DIComponent.getPublishStreamGrpcClient();
    }

    /**
     * Loads the default configuration for the simulator app.
     *
     * @return the default configuration
     * @throws IOException if an I/O error occurs while loading the configuration
     */
    private Configuration loadDefaultConfiguration() throws IOException {
        ConfigurationBuilder configurationBuilder =
                ConfigurationBuilder.create()
                        .withSource(SystemEnvironmentConfigSource.getInstance())
                        .withSource(SystemPropertiesConfigSource.getInstance())
                        .withSource(new ClasspathFileConfigSource(Path.of("app.properties")))
                        .autoDiscoverExtensions();
        return configurationBuilder.build();
    }

    /**
     * Starts the block stream simulator.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public void start() throws InterruptedException {
        int delayMSBetweenBlockItems = delayBetweenBlockItems / 1_000_000;
        int delayNSBetweenBlockItems = delayBetweenBlockItems % 1_000_000;

        isRunning = true;
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has started");

        boolean streamBlockItem = true;
        int blockItemsStreamed = 0;

        while (streamBlockItem) {
            // get block item
            BlockItem blockItem = blockStreamManager.getNextBlockItem();
            publishStreamGrpcClient.streamBlockItem(blockItem);
            blockItemsStreamed++;

            Thread.sleep(delayMSBetweenBlockItems, delayNSBetweenBlockItems);

            if (blockItemsStreamed >= blockStreamConfig.maxBlockItemsToStream()) {
                streamBlockItem = false;
            }
        }

        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has stopped");
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
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has stopped");
    }
}
