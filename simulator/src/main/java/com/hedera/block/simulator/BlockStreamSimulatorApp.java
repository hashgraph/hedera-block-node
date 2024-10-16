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
import com.hedera.block.simulator.config.types.StreamingMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.swirlds.config.api.Configuration;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/** BlockStream Simulator App */
public class BlockStreamSimulatorApp {

    private static final System.Logger LOGGER =
            System.getLogger(BlockStreamSimulatorApp.class.getName());

    private final BlockStreamManager blockStreamManager;
    private final PublishStreamGrpcClient publishStreamGrpcClient;
    private final BlockStreamConfig blockStreamConfig;
    private final StreamingMode streamingMode;

    private final int delayBetweenBlockItems;
    private final int millisecondsPerBlock;
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

        streamingMode = blockStreamConfig.streamingMode();
        millisecondsPerBlock = blockStreamConfig.millisecondsPerBlock();
        delayBetweenBlockItems = blockStreamConfig.delayBetweenBlockItems();
    }

    /**
     * Starts the block stream simulator.
     *
     * @throws InterruptedException if the thread is interrupted
     * @throws BlockSimulatorParsingException if a parse error occurs
     * @throws IOException if an I/O error occurs
     */
    public void start() throws InterruptedException, BlockSimulatorParsingException, IOException {

        isRunning.set(true);
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has started");

        if (streamingMode == StreamingMode.MILLIS_PER_BLOCK) {
            millisPerBlockStreaming();
        } else {
            constantRateStreaming();
        }

        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has stopped");
    }

    private void millisPerBlockStreaming()
            throws IOException, InterruptedException, BlockSimulatorParsingException {

        final long secondsPerBlockNanos = millisecondsPerBlock * 1_000_000L;

        Block nextBlock = blockStreamManager.getNextBlock();
        while (nextBlock != null) {
            long startTime = System.nanoTime();
            publishStreamGrpcClient.streamBlock(nextBlock);
            long elapsedTime = System.nanoTime() - startTime;
            long timeToDelay = secondsPerBlockNanos - elapsedTime;
            if (timeToDelay > 0) {
                Thread.sleep(timeToDelay / 1_000_000, (int) (timeToDelay % 1_000_000));
            } else {
                LOGGER.log(
                        System.Logger.Level.WARNING,
                        "Block Server is running behind, Streaming took longer than max expected: "
                                + millisecondsPerBlock
                                + " milliseconds");
            }
            nextBlock = blockStreamManager.getNextBlock();
        }
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has stopped");
    }

    private void constantRateStreaming()
            throws InterruptedException, IOException, BlockSimulatorParsingException {
        int delayMSBetweenBlockItems = delayBetweenBlockItems / 1_000_000;
        int delayNSBetweenBlockItems = delayBetweenBlockItems % 1_000_000;
        boolean streamBlockItem = true;
        int blockItemsStreamed = 0;

        while (streamBlockItem) {
            // get block item
            BlockItem blockItem = blockStreamManager.getNextBlockItem();

            if (blockItem == null) {
                LOGGER.log(
                        System.Logger.Level.INFO,
                        "Block Stream Simulator has reached the end of the block items");
                break;
            }

            publishStreamGrpcClient.streamBlockItem(List.of(blockItem));
            blockItemsStreamed++;

            Thread.sleep(delayMSBetweenBlockItems, delayNSBetweenBlockItems);

            if (blockItemsStreamed >= blockStreamConfig.maxBlockItemsToStream()) {
                LOGGER.log(
                        System.Logger.Level.INFO,
                        "Block Stream Simulator has reached the maximum number of block items to"
                                + " stream");
                streamBlockItem = false;
            }
        }
    }

    /**
     * Returns whether the block stream simulator is running.
     *
     * @return true if the block stream simulator is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /** Stops the block stream simulator. */
    public void stop() {
        isRunning.set(false);
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has stopped");
    }
}
