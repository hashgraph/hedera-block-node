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
import com.hedera.block.simulator.config.types.StreamingMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

public class PublisherModeHandler implements SimulatorModeHandler {
    private static final System.Logger LOGGER =
            System.getLogger(PublisherModeHandler.class.getName());

    private final BlockStreamConfig blockStreamConfig;
    private final PublishStreamGrpcClient publishStreamGrpcClient;
    private final StreamingMode streamingMode;
    private final int delayBetweenBlockItems;
    private final int millisecondsPerBlock;
    private final int DELAY_DENOMINATOR = 1_000_000;

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
        LOGGER.log(
                System.Logger.Level.INFO,
                String.format(
                        "Start streaming in %s streaming mode.", StreamingMode.MILLIS_PER_BLOCK));

        final long secondsPerBlockNanos = (long) millisecondsPerBlock * DELAY_DENOMINATOR;

        Block nextBlock = blockStreamManager.getNextBlock();
        while (nextBlock != null) {
            long startTime = System.nanoTime();
            publishStreamGrpcClient.streamBlock(nextBlock);
            long elapsedTime = System.nanoTime() - startTime;
            long timeToDelay = secondsPerBlockNanos - elapsedTime;
            if (timeToDelay > 0) {
                Thread.sleep(
                        timeToDelay / DELAY_DENOMINATOR, (int) (timeToDelay % DELAY_DENOMINATOR));
            } else {
                LOGGER.log(
                        System.Logger.Level.WARNING,
                        "Block Server is running behind, Streaming took longer than max expected: "
                                + millisecondsPerBlock
                                + " milliseconds");
            }
            nextBlock = blockStreamManager.getNextBlock();
        }
    }

    private void constantRateStreaming(@NonNull BlockStreamManager blockStreamManager)
            throws InterruptedException, IOException, BlockSimulatorParsingException {
        LOGGER.log(
                System.Logger.Level.INFO,
                String.format(
                        "Start streaming in %s streaming mode.", StreamingMode.CONSTANT_RATE));

        final long delayMSBetweenBlockItems = delayBetweenBlockItems / DELAY_DENOMINATOR;
        final int delayNSBetweenBlockItems = delayBetweenBlockItems % DELAY_DENOMINATOR;
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

            publishStreamGrpcClient.streamBlockItem(blockItem);
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
}
