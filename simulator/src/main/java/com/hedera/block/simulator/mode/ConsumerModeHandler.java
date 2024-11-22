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

import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.grpc.ConsumerStreamGrpcClient;
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
    /** Logger for this class */
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // Configuration
    private final BlockStreamConfig blockStreamConfig;

    // Service dependencies
    private final ConsumerStreamGrpcClient consumerStreamGrpcClient;

    /**
     * Constructs a new {@code ConsumerModeHandler} with the specified dependencies.
     *
     * @param blockStreamConfig The configuration for block streaming parameters
     * @param consumerStreamGrpcClient The client for consuming blocks via gRPC
     * @throws NullPointerException if any parameter is null
     */
    public ConsumerModeHandler(
            @NonNull final BlockStreamConfig blockStreamConfig,
            @NonNull final ConsumerStreamGrpcClient consumerStreamGrpcClient) {
        this.blockStreamConfig = requireNonNull(blockStreamConfig);
        this.consumerStreamGrpcClient = requireNonNull(consumerStreamGrpcClient);
    }

    /**
     * Initializes the gRPC channel for block consumption.
     */
    @Override
    public void init() {
        consumerStreamGrpcClient.init();
        LOGGER.log(INFO, "gRPC Channel initialized for consuming blocks.");
    }

    /**
     * Starts consuming blocks from the stream beginning at genesis (block 0).
     * Currently, requests an infinite stream of blocks starting from genesis.
     *
     * @throws InterruptedException if the consumption process is interrupted
     */
    @Override
    public void start() throws InterruptedException {
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator is starting in consumer mode.");
        consumerStreamGrpcClient.requestBlocks(0, 0);
    }

    /**
     * Gracefully stops block consumption and shuts down the gRPC client.
     *
     * @throws InterruptedException if the shutdown process is interrupted
     */
    @Override
    public void stop() throws InterruptedException {
        consumerStreamGrpcClient.completeStreaming();
        consumerStreamGrpcClient.shutdown();
    }
}
