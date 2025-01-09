// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import com.hedera.block.server.util.TestConfigUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link BlockAsLocalFilePathResolver} class.
 */
@SuppressWarnings("FieldCanBeLocal")
class BlockAsLocalFilePathResolverTest {
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;

    @TempDir
    private Path testLiveRootPath;

    private BlockAsLocalFilePathResolver toTest;

    @BeforeEach
    void setUp() throws IOException {
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY, testLiveRootPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);

        final String testConfigLiveRootPath = testConfig.liveRootPath();
        assertThat(testConfigLiveRootPath).isEqualTo(testLiveRootPath.toString());
        toTest = BlockAsLocalFilePathResolver.of(testConfig);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#resolveLiveRawPathToBlock(long)}
     * correctly resolves the path to a block by a given number. For the
     * block-as-file storage strategy, the path to a block is a trie structure
     * where each digit of the block number is a directory and the block number
     * itself is the file name.
     *
     * @param toResolve parameterized, valid block number
     * @param expectedBlockFile parameterized, expected block file
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulLiveRawPathResolution(final long toResolve, final String expectedBlockFile) {
        final Path actual = toTest.resolveLiveRawPathToBlock(toResolve);
        assertThat(actual).isNotNull().isAbsolute().isEqualByComparingTo(testLiveRootPath.resolve(expectedBlockFile));
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#resolveLiveRawPathToBlock(long)} correctly
     * throws an {@link IllegalArgumentException} when an invalid block number
     * is provided. A block number is invalid if it is a strictly negative number.
     *
     * @param toResolve parameterized, invalid block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumberLiveResolve(final long toResolve) {
        assertThatIllegalArgumentException().isThrownBy(() -> toTest.resolveLiveRawPathToBlock(toResolve));
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#resolveArchiveRawPathToBlock(long)}
     * correctly resolves the path to a block by a given number. For the
     * block-as-file storage strategy, the path to a block is a trie structure
     * where each digit of the block number is a directory and the block number
     * itself is the file name.
     *
     * @param toResolve parameterized, valid block number
     * @param expectedBlockFile parameterized, expected block file
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulArchiveRawPathResolution(final long toResolve, final String expectedBlockFile) {
        // todo this test is not yet implemented
        Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> toTest.resolveArchiveRawPathToBlock(toResolve));
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#resolveArchiveRawPathToBlock(long)} correctly
     * throws an {@link IllegalArgumentException} when an invalid block number
     * is provided. A block number is invalid if it is a strictly negative number.
     *
     * @param toResolve parameterized, invalid block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumberArchiveResolve(final long toResolve) {
        assertThatIllegalArgumentException().isThrownBy(() -> toTest.resolveArchiveRawPathToBlock(toResolve));
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findBlock(long)} correctly finds a
     * block by a given number, with no compression.
     *
     * @param blockNumber parameterized, valid block number
     * @param expectedBlockFile parameterized, expected block file
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulFindBlockNoCompression(final long blockNumber, final String expectedBlockFile)
            throws IOException {
        final Path expected = testLiveRootPath.resolve(expectedBlockFile);
        Files.createDirectories(expected.getParent());
        Files.createFile(expected);

        // assert block was created successfully
        assertThat(expected).exists().isRegularFile().isReadable();

        final Optional<Path> actual = toTest.findBlock(blockNumber);
        assertThat(actual)
                .isNotNull()
                .isPresent()
                .get(InstanceOfAssertFactories.PATH)
                .isAbsolute()
                .exists()
                .isReadable()
                .isRegularFile()
                .isEqualByComparingTo(expected);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findBlock(long)} correctly finds a
     * block by a given number, with zstd compression.
     *
     * @param blockNumber parameterized, valid block number
     * @param expectedBlockFile parameterized, expected block file
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulFindBlockZstdCompressed(final long blockNumber, final String expectedBlockFile)
            throws IOException {
        final String expectedBlockFileWithExtension = expectedBlockFile.concat(CompressionType.ZSTD.getFileExtension());
        final Path expected = testLiveRootPath.resolve(expectedBlockFileWithExtension);
        Files.createDirectories(expected.getParent());
        Files.createFile(expected);

        // assert block was created successfully
        assertThat(expected).exists().isRegularFile().isReadable();

        final Optional<Path> actual = toTest.findBlock(blockNumber);
        assertThat(actual)
                .isNotNull()
                .isPresent()
                .get(InstanceOfAssertFactories.PATH)
                .isAbsolute()
                .exists()
                .isReadable()
                .isRegularFile()
                .isEqualByComparingTo(expected);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findBlock(long)} correctly returns an
     * empty {@link Optional} when a block is not found.
     *
     * @param blockNumber parameterized, valid block number
     * @param expectedBlockFile parameterized, expected block file
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testBlockNotFound(final long blockNumber, final String expectedBlockFile) throws IOException {
        final Path expected = testLiveRootPath.resolve(expectedBlockFile);

        // assert block does not exist
        assertThat(expected).doesNotExist();

        final Optional<Path> actual = toTest.findBlock(blockNumber);
        assertThat(actual).isNotNull().isEmpty();
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findBlock(long)} correctly throws an
     * {@link IllegalArgumentException} when an invalid block number
     * is provided.
     *
     * @param blockNumber parameterized, invalid block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumberFindBlock(final long blockNumber) {
        assertThatIllegalArgumentException().isThrownBy(() -> toTest.findBlock(blockNumber));
    }

    /**
     * Some valid block numbers.
     *
     * @return a stream of valid block numbers
     */
    public static Stream<Arguments> validBlockNumbers() {
        return Stream.of(
                Arguments.of(0L, "0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0000000000000000000.blk"),
                Arguments.of(1L, "0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0000000000000000001.blk"),
                Arguments.of(2L, "0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0000000000000000002.blk"),
                Arguments.of(10L, "0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0000000000000000010.blk"),
                Arguments.of(100L, "0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0000000000000000100.blk"),
                Arguments.of(1_000L, "0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0000000000000001000.blk"),
                Arguments.of(10_000L, "0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0000000000000010000.blk"),
                Arguments.of(100_000L, "0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0000000000000100000.blk"),
                Arguments.of(1_000_000L, "0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0000000000001000000.blk"),
                Arguments.of(10_000_000L, "0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0000000000010000000.blk"),
                Arguments.of(100_000_000L, "0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0000000000100000000.blk"),
                Arguments.of(1_000_000_000L, "0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0000000001000000000.blk"),
                Arguments.of(10_000_000_000L, "0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0000000010000000000.blk"),
                Arguments.of(100_000_000_000L, "0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0000000100000000000.blk"),
                Arguments.of(1_000_000_000_000L, "0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0000001000000000000.blk"),
                Arguments.of(10_000_000_000_000L, "0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0000010000000000000.blk"),
                Arguments.of(100_000_000_000_000L, "0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0000100000000000000.blk"),
                Arguments.of(1_000_000_000_000_000L, "0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0001000000000000000.blk"),
                Arguments.of(10_000_000_000_000_000L, "0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0010000000000000000.blk"),
                Arguments.of(100_000_000_000_000_000L, "0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0100000000000000000.blk"),
                Arguments.of(1_000_000_000_000_000_000L, "1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1000000000000000000.blk"),
                Arguments.of(Long.MAX_VALUE, "9/2/2/3/3/7/2/0/3/6/8/5/4/7/7/5/8/0/9223372036854775807.blk"));
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
