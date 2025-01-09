// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.read;

import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.from;
import static org.mockito.Mockito.spy;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.compression.Compression;
import com.hedera.block.server.persistence.storage.compression.NoOpCompression;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalFilePathResolver;
import com.hedera.block.server.persistence.storage.write.BlockAsLocalFileWriter;
import com.hedera.block.server.util.PersistTestUtils;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link BlockAsLocalFileReader} class.
 */
@SuppressWarnings("FieldCanBeLocal")
class BlockAsLocalFileReaderTest {
    private BlockAsLocalFileWriter blockAsLocalFileWriterMock;
    private Compression compressionMock;
    private BlockAsLocalFileReader toTest;

    @TempDir
    private Path testLiveRootPath;

    @BeforeEach
    void setUp() throws IOException {
        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY, testLiveRootPath.toString()));
        final PersistenceStorageConfig testConfig =
                blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);

        final String testConfigLiveRootPath = testConfig.liveRootPath();
        assertThat(testConfigLiveRootPath).isEqualTo(testLiveRootPath.toString());

        compressionMock = spy(NoOpCompression.newInstance());
        final BlockAsLocalFilePathResolver blockAsLocalFileResolverMock =
                spy(BlockAsLocalFilePathResolver.of(testLiveRootPath));
        blockAsLocalFileWriterMock =
                spy(BlockAsLocalFileWriter.of(blockNodeContext, blockAsLocalFileResolverMock, compressionMock));
        toTest = BlockAsLocalFileReader.of(blockAsLocalFileResolverMock);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFileReader#read(long)} correctly reads a block with a
     * given block number and returns a {@code non-empty} {@link Optional}.
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulBlockRead(final long blockNumber) throws IOException, ParseException {
        final List<BlockItemUnparsed> blockItemUnparsed =
                PersistTestUtils.generateBlockItemsUnparsedForWithBlockNumber(blockNumber);
        final Optional<List<BlockItemUnparsed>> written = blockAsLocalFileWriterMock.write(blockItemUnparsed);

        // writing the test data is successful
        assertThat(written).isNotNull().isPresent();

        final Optional<BlockUnparsed> actual = toTest.read(blockNumber);
        assertThat(actual)
                .isNotNull()
                .isPresent()
                .get(InstanceOfAssertFactories.type(BlockUnparsed.class))
                .isNotNull()
                .isExactlyInstanceOf(BlockUnparsed.class)
                .returns(blockNumber, from(blockUnparsed -> {
                    try {
                        return BlockHeader.PROTOBUF
                                .parse(Objects.requireNonNull(
                                        blockUnparsed.blockItems().getFirst().blockHeader()))
                                .number();
                    } catch (final ParseException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .extracting(BlockUnparsed::blockItems)
                .asList()
                .isNotNull()
                .isNotEmpty();
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFileReader#read(long)} correctly reads a block with a
     * given block number and has the same contents as the block that has been
     * persisted.
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulBlockReadContents(final long blockNumber) throws IOException, ParseException {
        final List<BlockItemUnparsed> blockItemUnparsed =
                PersistTestUtils.generateBlockItemsUnparsedForWithBlockNumber(blockNumber);
        final Optional<List<BlockItemUnparsed>> written = blockAsLocalFileWriterMock.write(blockItemUnparsed);

        // writing the test data is successful
        assertThat(written).isNotNull().isPresent();

        final Optional<BlockUnparsed> actual = toTest.read(blockNumber);
        assertThat(actual)
                .isNotNull()
                .isPresent()
                .get(InstanceOfAssertFactories.type(BlockUnparsed.class))
                .isNotNull()
                .extracting(BlockUnparsed::blockItems)
                .asList()
                .isNotNull()
                .isNotEmpty()
                .hasSize(blockItemUnparsed.size())
                .containsExactlyElementsOf(blockItemUnparsed);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFileReader#read(long) correctly returns an empty
     * {@link Optional} when no block file is found for the given valid block
     * number.
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testEmptyOptWhenNoBLockFileFound(final long blockNumber) throws IOException, ParseException {
        final Optional<BlockUnparsed> actual = toTest.read(blockNumber);
        assertThat(actual).isNotNull().isEmpty();
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFileReader#read(long)} correctly throws an
     * {@link IllegalArgumentException} when an invalid block number is
     * provided. A block number is invalid if it is a strictly negative number.
     *
     * @param toRead parameterized, block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumber(final long toRead) {
        assertThatIllegalArgumentException().isThrownBy(() -> toTest.read(toRead));
    }

    /**
     * Some valid block numbers.
     *
     * @return a stream of valid block numbers
     */
    public static Stream<Arguments> validBlockNumbers() {
        return Stream.of(
                Arguments.of(0L),
                Arguments.of(1L),
                Arguments.of(2L),
                Arguments.of(10L),
                Arguments.of(100L),
                Arguments.of(1_000L),
                Arguments.of(10_000L),
                Arguments.of(100_000L),
                Arguments.of(1_000_000L),
                Arguments.of(10_000_000L),
                Arguments.of(100_000_000L),
                Arguments.of(1_000_000_000L),
                Arguments.of(10_000_000_000L),
                Arguments.of(100_000_000_000L),
                Arguments.of(1_000_000_000_000L),
                Arguments.of(10_000_000_000_000L),
                Arguments.of(100_000_000_000_000L),
                Arguments.of(1_000_000_000_000_000L),
                Arguments.of(10_000_000_000_000_000L),
                Arguments.of(100_000_000_000_000_000L),
                Arguments.of(1_000_000_000_000_000_000L),
                Arguments.of(Long.MAX_VALUE));
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
