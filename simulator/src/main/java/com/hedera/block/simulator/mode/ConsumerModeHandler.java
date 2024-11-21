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
    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final ConsumerStreamGrpcClient consumerStreamGrpcClient;
    private final BlockStreamConfig blockStreamConfig;

    public ConsumerModeHandler(
            @NonNull final BlockStreamConfig blockStreamConfig,
            @NonNull final ConsumerStreamGrpcClient consumerStreamGrpcClient) {
        this.blockStreamConfig = requireNonNull(blockStreamConfig);
        this.consumerStreamGrpcClient = requireNonNull(consumerStreamGrpcClient);
    }

    /**
     * Starts the simulator and initiate streaming, depending on the working mode.
     */
    @Override
    public void start() throws InterruptedException {
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator is starting in consumer mode.");
        // WIP
        consumerStreamGrpcClient.requestBlocks(0, 0);
    }

    /**
     * Stops the handler and manager from streaming.
     */
    @Override
    public void stop() {
        // WIP
        throw new UnsupportedOperationException();
    }
}
