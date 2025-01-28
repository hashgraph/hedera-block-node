// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.write;

import static com.hedera.block.server.persistence.storage.read.BlockAsLocalDirReaderTest.removeBlockReadPerms;
import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.hedera.block.server.ack.AckHandler;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalDirPathResolver;
import com.hedera.block.server.persistence.storage.read.BlockAsLocalDirReader;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockAsLocalDirRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.block.server.util.TestUtils;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

public class BlockAsLocalDirWriterTest {
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;
    private BlockAsLocalDirPathResolver pathResolverMock;
    private List<BlockItemUnparsed> blockItems;

    @Mock
    private AckHandler ackHandlerMock;

    @TempDir
    private Path testLiveRootPath;

    @BeforeEach
    public void setUp() throws IOException {
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY, testLiveRootPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        blockItems = generateBlockItemsUnparsed(1);

        final String testConfigLiveRootPath = testConfig.liveRootPath();
        assertThat(testConfigLiveRootPath).isEqualTo(testLiveRootPath.toString());
        pathResolverMock = spy(BlockAsLocalDirPathResolver.of(testConfig));

        ackHandlerMock = mock(AckHandler.class);
    }

    @Test
    public void testWriterAndReaderHappyPath() throws IOException, ParseException {

        final BlockWriter<List<BlockItemUnparsed>, Long> blockWriter =
                BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock);
        for (int i = 0; i < 10; i++) {
            final Optional<Long> result = blockWriter.write(List.of(blockItems.get(i)));
            if (i == 9) {
                if (result.isPresent()) {
                    assertEquals(1L, result.get());
                } else {
                    fail("The optional should contain the last block proof block item");
                }
            } else {
                assertTrue(result.isEmpty());
            }
        }

        // Confirm the block
        BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());

        boolean hasHeader = false;
        boolean hasBlockProof = false;
        boolean hasStartEvent = false;

        BlockUnparsed block = blockOpt.get();
        for (BlockItemUnparsed blockItem : block.blockItems()) {
            if (blockItem.hasBlockHeader()) {
                hasHeader = true;
            } else if (blockItem.hasBlockProof()) {
                hasBlockProof = true;
            } else if (blockItem.hasEventHeader()) {
                hasStartEvent = true;
            } else {
                fail("Unknown block item type");
            }
        }

        assertTrue(hasHeader, "Block should have a header");
        assertTrue(hasBlockProof, "Block should have a block proof");
        assertTrue(hasStartEvent, "Block should have a start event");
    }

    @Test
    public void testRemoveBlockWritePerms() throws IOException, ParseException {

        final BlockWriter<List<BlockItemUnparsed>, Long> blockWriter =
                BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock);

        // Change the permissions on the block node root directory
        removeRootWritePerms(testConfig);

        // The first BlockItem contains a header which will create a new block directory.
        // The BlockWriter will attempt to repair the permissions and should succeed.
        Optional<Long> result = blockWriter.write(List.of(blockItems.getFirst()));
        assertFalse(result.isPresent());

        // Confirm we're able to read 1 block item
        BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(1, blockOpt.get().blockItems().size());
        assertTrue(blockOpt.get().blockItems().getFirst().hasBlockHeader());

        // Remove all permissions on the block directory and
        // attempt to write the next block item
        removeBlockAllPerms(1, testConfig);
        result = blockWriter.write(List.of(blockItems.get(1)));
        assertFalse(result.isPresent());

        // There should now be 2 blockItems in the block
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(2, blockOpt.get().blockItems().size());
        assertFalse(blockOpt.get().blockItems().get(1).hasBlockHeader());

        // Remove read permission on the block directory
        removeBlockReadPerms(1, testConfig);
        result = blockWriter.write(List.of(blockItems.get(2)));
        assertFalse(result.isPresent());

        // There should now be 3 blockItems in the block
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(3, blockOpt.get().blockItems().size());
        assertFalse(blockOpt.get().blockItems().get(1).hasBlockHeader());
    }

    @Test
    public void testUnrecoverableIOExceptionOnWrite() throws IOException, ParseException {
        // Use a spy to simulate an IOException when the first block item is written
        final BlockWriter<List<BlockItemUnparsed>, Long> blockWriter =
                spy(BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock));
        doThrow(IOException.class).when(blockWriter).write(blockItems);
        assertThrows(IOException.class, () -> blockWriter.write(blockItems));
    }

    @Test
    public void testRemoveRootDirReadPerm() throws IOException, ParseException {

        final BlockWriter<List<BlockItemUnparsed>, Long> blockWriter =
                BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock);

        // Write the first block item to create the block
        // directory
        Optional<Long> result = blockWriter.write(List.of(blockItems.getFirst()));
        assertFalse(result.isPresent());

        // Remove root dir read permissions and
        // block dir read permissions
        removeRootReadPerms(testConfig);
        removeBlockReadPerms(1, testConfig);

        // Attempt to write the remaining block
        // items
        for (int i = 1; i < 10; i++) {
            if (i == 9) {
                result = blockWriter.write(List.of(blockItems.get(i)));
                if (result.isPresent()) {
                    assertEquals(1L, result.get());
                } else {
                    fail("The optional should contain the last block proof block item");
                }
            } else {
                result = blockWriter.write(List.of(blockItems.get(i)));
                assertTrue(result.isEmpty());
            }
        }

        BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        Optional<BlockUnparsed> blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(10, blockOpt.get().blockItems().size());
    }

    @Test
    public void testPartialBlockRemoval() throws IOException, ParseException {
        final int expectedItemsPerBlock = 10;
        final List<BlockItemUnparsed> blockItems = generateBlockItemsUnparsed(3);
        final BlockRemover blockRemover = BlockAsLocalDirRemover.of(pathResolverMock);
        final BlockAsLocalDirWriter toTest = BlockAsLocalDirWriter.of(blockNodeContext, blockRemover, pathResolverMock);

        // Now make the calls
        for (int i = 0; i < 23; i++) {
            final Optional<Long> result = toTest.write(List.of(blockItems.get(i)));
            if (i == 9 || i == 19) {
                // The last block item in each block is the block proof
                // and should be returned by the write method
                assertTrue(result.isPresent());
            } else {
                // The write method should return an empty optional
                assertTrue(result.isEmpty());
            }
        }

        // simulate an IOException on the 23rd block item
        final AtomicInteger callCount = new AtomicInteger(0);
        doAnswer(invocation -> {
                    if (callCount.incrementAndGet() == 1) {
                        return Path.of("/invalid_path/:invalid_directory");
                    }
                    return invocation.callRealMethod();
                })
                .when(pathResolverMock)
                .resolveLiveRawPathToBlock(3);

        assertThatIOException().isThrownBy(() -> toTest.write(List.of(blockItems.get(23))));

        // Verify the partially written block was removed
        final BlockReader<BlockUnparsed> blockReader = BlockAsLocalDirReader.of(testConfig);
        Optional<BlockUnparsed> blockOpt = blockReader.read(3);
        assertTrue(blockOpt.isEmpty());

        final Path targetPathForBlock1 = pathResolverMock.resolveLiveRawPathToBlock(1);
        assertThat(targetPathForBlock1).isNotNull().exists().isDirectory();

        final Path targetPathForBlock2 = pathResolverMock.resolveLiveRawPathToBlock(2);
        assertThat(targetPathForBlock2).isNotNull().exists().isDirectory();

        final Path targetPathForBlock3 = pathResolverMock.resolveLiveRawPathToBlock(3);
        assertThat(targetPathForBlock3).isNotNull().doesNotExist();

        // Confirm blocks 1 and 2 still exist
        blockOpt = blockReader.read(1);
        assertFalse(blockOpt.isEmpty());
        assertEquals(expectedItemsPerBlock, blockOpt.get().blockItems().size());
        assertEquals(
                1,
                BlockHeader.PROTOBUF
                        .parse(blockOpt.get().blockItems().getFirst().blockHeader())
                        .number());

        blockOpt = blockReader.read(2);
        assertFalse(blockOpt.isEmpty());
        assertEquals(expectedItemsPerBlock, blockOpt.get().blockItems().size());
        assertEquals(
                2,
                BlockHeader.PROTOBUF
                        .parse(blockOpt.get().blockItems().getFirst().blockHeader())
                        .number());
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalDirWriter#write(List)} correctly throws an
     * {@link IllegalArgumentException} when an invalid block number is
     * provided. A block number is invalid if it is a strictly negative number.
     *
     * @param blockNumber parameterized, block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumber(final long blockNumber) throws IOException {
        final BlockAsLocalDirWriter toTest =
                BlockAsLocalDirWriter.of(blockNodeContext, mock(BlockRemover.class), pathResolverMock);

        final BlockHeader blockHeader =
                BlockHeader.newBuilder().number(blockNumber).build();
        final BlockItemUnparsed blockItem = BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(blockHeader))
                .build();

        assertThatIllegalArgumentException().isThrownBy(() -> toTest.write(List.of(blockItem)));
    }

    private void removeRootWritePerms(final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.liveRootPath());
        Files.setPosixFilePermissions(blockNodeRootPath, TestUtils.getNoWrite().value());
    }

    private void removeRootReadPerms(final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.liveRootPath());
        Files.setPosixFilePermissions(blockNodeRootPath, TestUtils.getNoRead().value());
    }

    private void removeBlockAllPerms(final int blockNumber, final PersistenceStorageConfig config) throws IOException {
        final Path blockNodeRootPath = Path.of(config.liveRootPath());
        final Path blockPath = blockNodeRootPath.resolve(String.valueOf(blockNumber));
        Files.setPosixFilePermissions(blockPath, TestUtils.getNoPerms().value());
    }

    /**
     * Some invalid block numbers.
     *
     * @return a stream of invalid block numbers
     */
    public static Stream<Arguments> invalidBlockNumbers() {
        return Stream.of(
                Arguments.of(-1L),
                Arguments.of(-2L),
                Arguments.of(-10L),
                Arguments.of(-100L),
                Arguments.of(-1_000L),
                Arguments.of(-10_000L),
                Arguments.of(-100_000L),
                Arguments.of(-1_000_000L),
                Arguments.of(-10_000_000L),
                Arguments.of(-100_000_000L),
                Arguments.of(-1_000_000_000L),
                Arguments.of(-10_000_000_000L),
                Arguments.of(-100_000_000_000L),
                Arguments.of(-1_000_000_000_000L),
                Arguments.of(-10_000_000_000_000L),
                Arguments.of(-100_000_000_000_000L),
                Arguments.of(-1_000_000_000_000_000L),
                Arguments.of(-10_000_000_000_000_000L),
                Arguments.of(-100_000_000_000_000_000L),
                Arguments.of(-1_000_000_000_000_000_000L),
                Arguments.of(Long.MIN_VALUE));
    }
}
