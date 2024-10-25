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

import static com.hedera.block.simulator.Constants.NANOS_PER_MILLI;
import static java.util.Objects.requireNonNull;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.StreamingMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.hapi.block.stream.Block;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * The {@code PublisherModeHandler} class implements the {@link SimulatorModeHandler} interface
 * and provides the behavior for a mode where only publishing of block data
 * occurs.
 *
 * <p>This mode handles single operation in the block streaming process, utilizing the
 * {@link BlockStreamConfig} for configuration settings. It is designed for scenarios where
 * the simulator needs to handle publication of blocks.
 */
public class PublisherModeHandler implements SimulatorModeHandler {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final BlockStreamConfig blockStreamConfig;
    private final PublishStreamGrpcClient publishStreamGrpcClient;
    private final StreamingMode streamingMode;
    private final int delayBetweenBlockItems;
    private final int millisecondsPerBlock;

    /**
     * Constructs a new {@code PublisherModeHandler} with the specified block stream configuration and publisher client.
     *
     * @param blockStreamConfig the configuration data for managing block streams
     * @param publishStreamGrpcClient the grpc client used for streaming blocks
     */
    public PublisherModeHandler(
            @NonNull final BlockStreamConfig blockStreamConfig,
            @NonNull PublishStreamGrpcClient publishStreamGrpcClient) {
        requireNonNull(blockStreamConfig);
        requireNonNull(publishStreamGrpcClient);
        this.blockStreamConfig = blockStreamConfig;
        this.publishStreamGrpcClient = publishStreamGrpcClient;

        streamingMode = blockStreamConfig.streamingMode();
        delayBetweenBlockItems = blockStreamConfig.delayBetweenBlockItems();
        millisecondsPerBlock = blockStreamConfig.millisecondsPerBlock();
    }

    /**
     * Starts the simulator and initiate streaming, depending on the working mode.
     */
    @Override
    public void start(@NonNull BlockStreamManager blockStreamManager)
            throws BlockSimulatorParsingException, IOException, InterruptedException {
        requireNonNull(blockStreamManager);

        if (streamingMode == StreamingMode.MILLIS_PER_BLOCK) {
            millisPerBlockStreaming(blockStreamManager);
        } else {
            constantRateStreaming(blockStreamManager);
        }
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has stopped streaming.");
    }

    private void millisPerBlockStreaming(@NonNull BlockStreamManager blockStreamManager)
            throws IOException, InterruptedException, BlockSimulatorParsingException {

        final long secondsPerBlockNanos = (long) millisecondsPerBlock * NANOS_PER_MILLI;

        Block nextBlock = blockStreamManager.getNextBlock();
        while (nextBlock != null) {
            long startTime = System.nanoTime();
            publishStreamGrpcClient.streamBlock(nextBlock);
            long elapsedTime = System.nanoTime() - startTime;
            long timeToDelay = secondsPerBlockNanos - elapsedTime;
            if (timeToDelay > 0) {
                Thread.sleep(timeToDelay / NANOS_PER_MILLI, (int) (timeToDelay % NANOS_PER_MILLI));
            } else {
                LOGGER.log(
                        System.Logger.Level.WARNING,
                        "Block Server is running behind. Streaming took: "
                                + (elapsedTime / 1_000_000)
                                + "ms - Longer than max expected of: "
                                + millisecondsPerBlock
                                + " milliseconds");
            }
            nextBlock = blockStreamManager.getNextBlock();
        }
        LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator has stopped");
    }

    private void constantRateStreaming(@NonNull BlockStreamManager blockStreamManager)
            throws InterruptedException, IOException, BlockSimulatorParsingException {
        int delayMSBetweenBlockItems = delayBetweenBlockItems / NANOS_PER_MILLI;
        int delayNSBetweenBlockItems = delayBetweenBlockItems % NANOS_PER_MILLI;
        boolean streamBlockItem = true;
        int blockItemsStreamed = 0;

        while (streamBlockItem) {
            // get block
            Block block = blockStreamManager.getNextBlock();

            if (block == null) {
                LOGGER.log(
                        System.Logger.Level.INFO,
                        "Block Stream Simulator has reached the end of the block items");
                break;
            }

            publishStreamGrpcClient.streamBlock(block);
            blockItemsStreamed += block.items().size();

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
}
