// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.mode;

import static com.hedera.block.simulator.Constants.NANOS_PER_MILLI;
import static com.hedera.block.simulator.metrics.SimulatorMetricTypes.Counter.LiveBlockItemsSent;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.StreamingMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.hapi.block.stream.protoc.Block;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/**
 * The {@code PublisherModeHandler} class implements the
 * {@link SimulatorModeHandler} interface
 * and provides the behavior for a mode where simulator is working using PublishBlockStream and acts as a client.
 *
 * <p>
 * This mode handles single operation in the block streaming process, utilizing
 * the
 * {@link BlockStreamConfig} for configuration settings. It is designed for
 * scenarios where
 * the simulator needs to handle publication of blocks.
 */
public class PublisherClientModeHandler implements SimulatorModeHandler {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // Configuration fields
    private final BlockStreamConfig blockStreamConfig;
    private final StreamingMode streamingMode;
    private final int delayBetweenBlockItems;
    private final int millisecondsPerBlock;

    // Service dependencies
    private final BlockStreamManager blockStreamManager;
    private final PublishStreamGrpcClient publishStreamGrpcClient;
    private final MetricsService metricsService;

    // State fields
    private final AtomicBoolean shouldPublish;

    /**
     * Constructs a new {@code PublisherModeHandler} with the specified dependencies.
     *
     * @param blockStreamConfig The configuration for block streaming parameters
     * @param publishStreamGrpcClient The client for publishing blocks via gRPC
     * @param blockStreamManager The manager responsible for block generation
     * @param metricsService The service for recording metrics
     * @throws NullPointerException if any parameter is null
     */
    @Inject
    public PublisherClientModeHandler(
            @NonNull final BlockStreamConfig blockStreamConfig,
            @NonNull final PublishStreamGrpcClient publishStreamGrpcClient,
            @NonNull final BlockStreamManager blockStreamManager,
            @NonNull final MetricsService metricsService) {
        this.blockStreamConfig = requireNonNull(blockStreamConfig);
        this.publishStreamGrpcClient = requireNonNull(publishStreamGrpcClient);
        this.blockStreamManager = requireNonNull(blockStreamManager);
        this.metricsService = requireNonNull(metricsService);

        streamingMode = blockStreamConfig.streamingMode();
        delayBetweenBlockItems = blockStreamConfig.delayBetweenBlockItems();
        millisecondsPerBlock = blockStreamConfig.millisecondsPerBlock();
        shouldPublish = new AtomicBoolean(true);
    }

    public void init() {
        blockStreamManager.init();
        publishStreamGrpcClient.init();
        LOGGER.log(INFO, "gRPC Channel initialized for publishing blocks.");
    }

    /**
     * Starts the simulator and initiate streaming, depending on the working mode.
     *
     * @throws BlockSimulatorParsingException if an error occurs while parsing
     *                                        blocks
     * @throws IOException                    if an I/O error occurs during block
     *                                        streaming
     * @throws InterruptedException           if the thread running the simulator is
     *                                        interrupted
     */
    @Override
    public void start() throws BlockSimulatorParsingException, IOException, InterruptedException {
        LOGGER.log(INFO, "Block Stream Simulator is starting in publisher mode.");
        if (streamingMode == StreamingMode.MILLIS_PER_BLOCK) {
            millisPerBlockStreaming();
        } else {
            constantRateStreaming();
        }
        LOGGER.log(INFO, "Block Stream Simulator has stopped streaming.");
    }

    private void millisPerBlockStreaming() throws IOException, InterruptedException, BlockSimulatorParsingException {
        final long secondsPerBlockNanos = (long) millisecondsPerBlock * NANOS_PER_MILLI;

        Block nextBlock = blockStreamManager.getNextBlock();
        while (nextBlock != null && shouldPublish.get()) {
            long startTime = System.nanoTime();
            if (!publishStreamGrpcClient.streamBlock(nextBlock)) {
                LOGGER.log(System.Logger.Level.INFO, "Block Stream Simulator stopped streaming due to errors.");
                break;
            }

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
        LOGGER.log(INFO, "Block Stream Simulator has stopped");
        LOGGER.log(
                INFO,
                "Number of BlockItems sent by the Block Stream Simulator: "
                        + metricsService.get(LiveBlockItemsSent).get());
    }

    private void constantRateStreaming() throws InterruptedException, IOException, BlockSimulatorParsingException {
        int delayMSBetweenBlockItems = delayBetweenBlockItems / NANOS_PER_MILLI;
        int delayNSBetweenBlockItems = delayBetweenBlockItems % NANOS_PER_MILLI;
        int blockItemsStreamed = 0;

        while (shouldPublish.get()) {
            // get block
            Block block = blockStreamManager.getNextBlock();

            if (block == null) {
                LOGGER.log(INFO, "Block Stream Simulator has reached the end of the block items");
                break;
            }
            if (!publishStreamGrpcClient.streamBlock(block)) {
                LOGGER.log(INFO, "Block Stream Simulator stopped streaming due to errors.");
                break;
            }

            blockItemsStreamed += block.getItemsList().size();

            Thread.sleep(delayMSBetweenBlockItems, delayNSBetweenBlockItems);

            if (blockItemsStreamed >= blockStreamConfig.maxBlockItemsToStream()) {
                LOGGER.log(INFO, "Block Stream Simulator has reached the maximum number of block items to" + " stream");
                shouldPublish.set(false);
            }
        }
    }

    /**
     * Stops the handler and manager from streaming.
     */
    @Override
    public void stop() throws InterruptedException {
        shouldPublish.set(false);
        publishStreamGrpcClient.shutdown();
    }
}
