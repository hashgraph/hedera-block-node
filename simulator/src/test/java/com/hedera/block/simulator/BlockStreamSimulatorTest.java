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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.block.simulator.mode.PublisherModeHandler;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.swirlds.config.api.Configuration;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockStreamSimulatorTest {

    @Mock private BlockStreamManager blockStreamManager;

    @Mock private PublishStreamGrpcClient publishStreamGrpcClient;

    private BlockStreamSimulatorApp blockStreamSimulator;

    @BeforeEach
    void setUp() throws IOException {

        Configuration configuration =
                TestUtils.getTestConfiguration(
                        Map.of(
                                "blockStream.maxBlockItemsToStream",
                                "100",
                                "blockStream.streamingMode",
                                "CONSTANT_RATE"));

        blockStreamSimulator =
                new BlockStreamSimulatorApp(
                        configuration, blockStreamManager, publishStreamGrpcClient);
    }

    @AfterEach
    void tearDown() {
        blockStreamSimulator.stop();
    }

    @Test
    void start_logsStartedMessage()
            throws InterruptedException, BlockSimulatorParsingException, IOException {
        blockStreamSimulator.start();
        assertTrue(blockStreamSimulator.isRunning());
    }

    @Test
    void start_constantRateStreaming()
            throws InterruptedException, BlockSimulatorParsingException, IOException {

        BlockItem blockItem =
                BlockItem.newBuilder()
                        .blockHeader(BlockHeader.newBuilder().number(1L).build())
                        .build();

        Block block1 = Block.newBuilder().items(blockItem).build();
        Block block2 = Block.newBuilder().items(blockItem, blockItem, blockItem).build();

        BlockStreamManager blockStreamManager = Mockito.mock(BlockStreamManager.class);
        when(blockStreamManager.getNextBlock()).thenReturn(block1, block2, null);

        Configuration configuration =
                TestUtils.getTestConfiguration(
                        Map.of(
                                "blockStream.maxBlockItemsToStream",
                                "2",
                                "generator.managerImplementation",
                                "BlockAsFileLargeDataSets",
                                "generator.rootPath",
                                getAbsoluteFolder("src/test/resources/block-0.0.3-blk/"),
                                "blockStream.streamingMode",
                                "CONSTANT_RATE",
                                "blockStream.blockItemsBatchSize",
                                "2"));

        BlockStreamSimulatorApp blockStreamSimulator =
                new BlockStreamSimulatorApp(
                        configuration, blockStreamManager, publishStreamGrpcClient);

        blockStreamSimulator.start();
        assertTrue(blockStreamSimulator.isRunning());
    }

    private String getAbsoluteFolder(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath().toString();
    }

    @Test
    void stop_doesNotThrowException() {
        assertDoesNotThrow(() -> blockStreamSimulator.stop());
        assertFalse(blockStreamSimulator.isRunning());
    }

    @Test
    void start_millisPerBlockStreaming()
            throws InterruptedException, IOException, BlockSimulatorParsingException {
        BlockStreamManager blockStreamManager = Mockito.mock(BlockStreamManager.class);
        BlockItem blockItem =
                BlockItem.newBuilder()
                        .blockHeader(BlockHeader.newBuilder().number(1L).build())
                        .build();
        Block block = Block.newBuilder().items(blockItem).build();
        when(blockStreamManager.getNextBlock()).thenReturn(block, block, null);

        Configuration configuration =
                TestUtils.getTestConfiguration(
                        Map.of(
                                "blockStream.maxBlockItemsToStream",
                                "2",
                                "generator.managerImplementation",
                                "BlockAsFileLargeDataSets",
                                "generator.rootPath",
                                getAbsoluteFolder("src/test/resources/block-0.0.3-blk/"),
                                "blockStream.streamingMode",
                                "MILLIS_PER_BLOCK"));

        BlockStreamSimulatorApp blockStreamSimulator =
                new BlockStreamSimulatorApp(
                        configuration, blockStreamManager, publishStreamGrpcClient);

        blockStreamSimulator.start();
        assertTrue(blockStreamSimulator.isRunning());
    }

    @Test
    void start_millisPerSecond_streamingLagVerifyWarnLog()
            throws InterruptedException, IOException, BlockSimulatorParsingException {
        List<LogRecord> logRecords = captureLogs();

        BlockStreamManager blockStreamManager = Mockito.mock(BlockStreamManager.class);
        BlockItem blockItem =
                BlockItem.newBuilder()
                        .blockHeader(BlockHeader.newBuilder().number(1L).build())
                        .build();
        Block block = Block.newBuilder().items(blockItem).build();
        when(blockStreamManager.getNextBlock()).thenReturn(block, block, null);
        PublishStreamGrpcClient publishStreamGrpcClient =
                Mockito.mock(PublishStreamGrpcClient.class);

        // simulate that the first block takes 15ms to stream, when the limit is 10, to force to go
        // over WARN Path.
        when(publishStreamGrpcClient.streamBlock(any()))
                .thenAnswer(
                        invocation -> {
                            Thread.sleep(15);
                            return true;
                        })
                .thenReturn(true);

        Configuration configuration =
                TestUtils.getTestConfiguration(
                        Map.of(
                                "generator.managerImplementation",
                                "BlockAsFileBlockStreamManager",
                                "generator.rootPath",
                                getAbsoluteFolder("src/test/resources/block-0.0.3-blk/"),
                                "blockStream.maxBlockItemsToStream",
                                "2",
                                "blockStream.streamingMode",
                                "MILLIS_PER_BLOCK",
                                "blockStream.millisecondsPerBlock",
                                "10",
                                "blockStream.blockItemsBatchSize",
                                "1"));

        BlockStreamSimulatorApp blockStreamSimulator =
                new BlockStreamSimulatorApp(
                        configuration, blockStreamManager, publishStreamGrpcClient);

        blockStreamSimulator.start();
        assertTrue(blockStreamSimulator.isRunning());

        // Assert log exists
        boolean found_log =
                logRecords.stream()
                        .anyMatch(
                                logRecord ->
                                        logRecord
                                                .getMessage()
                                                .contains("Block Server is running behind"));
        assertTrue(found_log);
    }

    @Test
    void start_withBothMode_throwsUnsupportedOperationException() throws Exception {
        Configuration configuration =
                TestUtils.getTestConfiguration(Map.of("blockStream.simulatorMode", "BOTH"));
        blockStreamSimulator =
                new BlockStreamSimulatorApp(
                        configuration, blockStreamManager, publishStreamGrpcClient);
        assertThrows(UnsupportedOperationException.class, () -> blockStreamSimulator.start());
    }

    @Test
    void start_withConsumerMode_throwsUnsupportedOperationException() throws Exception {
        Configuration configuration =
                TestUtils.getTestConfiguration(Map.of("blockStream.simulatorMode", "CONSUMER"));
        blockStreamSimulator =
                new BlockStreamSimulatorApp(
                        configuration, blockStreamManager, publishStreamGrpcClient);
        assertThrows(UnsupportedOperationException.class, () -> blockStreamSimulator.start());
    }

    private List<LogRecord> captureLogs() {
        // Capture logs
        Logger logger = Logger.getLogger(PublisherModeHandler.class.getName());
        final List<LogRecord> logRecords = new ArrayList<>();

        // Custom handler to capture logs
        Handler handler =
                new Handler() {
                    @Override
                    public void publish(LogRecord record) {
                        logRecords.add(record);
                    }

                    @Override
                    public void flush() {}

                    @Override
                    public void close() throws SecurityException {}
                };

        // Add handler to logger
        logger.addHandler(handler);

        return logRecords;
    }
}
