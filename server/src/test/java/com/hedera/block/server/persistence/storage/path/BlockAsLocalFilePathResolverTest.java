// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_ARCHIVE_BATCH_SIZE;
import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.hedera.block.server.Constants;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import com.hedera.block.server.util.TestConfigUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(Map.of(
                PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY,
                testLiveRootPath.toString(),
                PERSISTENCE_STORAGE_ARCHIVE_BATCH_SIZE,
                "10"));
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
     * {@link BlockAsLocalFilePathResolver#resolveLiveRawUnverifiedPathToBlock(long)}
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
    void testSuccessfulLiveRawUnverifiedPathResolution(final long toResolve, final String expectedBlockFile) {
        final String expectedUnverified =
                expectedBlockFile.replace(Constants.BLOCK_FILE_EXTENSION, Constants.UNVERIFIED_BLOCK_FILE_EXTENSION);
        final Path actual = toTest.resolveLiveRawUnverifiedPathToBlock(toResolve);
        assertThat(actual).isNotNull().isAbsolute().isEqualByComparingTo(testLiveRootPath.resolve(expectedUnverified));
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#resolveLiveRawUnverifiedPathToBlock(long)} correctly
     * throws an {@link IllegalArgumentException} when an invalid block number
     * is provided. A block number is invalid if it is a strictly negative number.
     *
     * @param toResolve parameterized, invalid block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumberLiveUnverifiedResolve(final long toResolve) {
        assertThatIllegalArgumentException().isThrownBy(() -> toTest.resolveLiveRawPathToBlock(toResolve));
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findLiveBlock(long)} correctly finds a
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

        final Optional<LiveBlockPath> actual = toTest.findLiveBlock(blockNumber);
        assertThat(actual)
                .isNotNull()
                .isPresent()
                .get(InstanceOfAssertFactories.type(LiveBlockPath.class))
                .returns(blockNumber, LiveBlockPath::blockNumber)
                .returns(expected.getParent(), LiveBlockPath::dirPath)
                .returns(expected.getFileName().toString(), LiveBlockPath::blockFileName)
                .returns(CompressionType.NONE, LiveBlockPath::compressionType);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findLiveBlock(long)} correctly finds a
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

        final Optional<LiveBlockPath> actual = toTest.findLiveBlock(blockNumber);
        assertThat(actual)
                .isNotNull()
                .isPresent()
                .get(InstanceOfAssertFactories.type(LiveBlockPath.class))
                .returns(blockNumber, LiveBlockPath::blockNumber)
                .returns(expected.getParent(), LiveBlockPath::dirPath)
                .returns(expected.getFileName().toString(), LiveBlockPath::blockFileName)
                .returns(CompressionType.ZSTD, LiveBlockPath::compressionType);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findLiveBlock(long)} correctly returns an
     * empty {@link Optional} when a block is not found.
     *
     * @param blockNumber parameterized, valid block number
     * @param expectedBlockFile parameterized, expected block file
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testBlockNotFound(final long blockNumber, final String expectedBlockFile) {
        final Path expected = testLiveRootPath.resolve(expectedBlockFile);

        // assert block does not exist
        assertThat(expected).doesNotExist();

        final Optional<LiveBlockPath> actual = toTest.findLiveBlock(blockNumber);
        assertThat(actual).isNotNull().isEmpty();
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findLiveBlock(long)} correctly throws an
     * {@link IllegalArgumentException} when an invalid block number
     * is provided.
     *
     * @param blockNumber parameterized, invalid block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumberFindBlock(final long blockNumber) {
        assertThatIllegalArgumentException().isThrownBy(() -> toTest.findLiveBlock(blockNumber));
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findArchivedBlock(long)} correctly finds a
     * block by a given number, with no compression.
     *
     * @param blockNumber parameterized, valid block number
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulFindArchiveBlockNoCompression(final long blockNumber) throws IOException {
        final ArchiveBlockPath expected = toTest.resolveRawArchivePath(blockNumber);
        createTestZipWithEntry(expected);

        // assert block was created successfully
        assertThat(expected.dirPath().resolve(expected.zipFileName()))
                .exists()
                .isRegularFile()
                .isReadable();

        final Optional<ArchiveBlockPath> actual = toTest.findArchivedBlock(blockNumber);
        assertThat(actual)
                .isNotNull()
                .isPresent()
                .get(InstanceOfAssertFactories.type(ArchiveBlockPath.class))
                .returns(expected.blockNumber(), ArchiveBlockPath::blockNumber)
                .returns(expected.dirPath(), ArchiveBlockPath::dirPath)
                .returns(expected.zipFileName(), ArchiveBlockPath::zipFileName)
                .returns(expected.zipEntryName(), ArchiveBlockPath::zipEntryName)
                .returns(CompressionType.NONE, ArchiveBlockPath::compressionType);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findArchivedBlock(long)} correctly finds a
     * block by a given number, with zstd compression.
     *
     * @param blockNumber parameterized, valid block number
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulFindArchiveBlockZstdCompressed(final long blockNumber) throws IOException {
        final ArchiveBlockPath rawArchivePath = toTest.resolveRawArchivePath(blockNumber);
        final ArchiveBlockPath expected = new ArchiveBlockPath(
                rawArchivePath.dirPath(),
                rawArchivePath.zipFileName(),
                rawArchivePath.zipEntryName().concat(CompressionType.ZSTD.getFileExtension()),
                CompressionType.ZSTD,
                rawArchivePath.blockNumber());
        createTestZipWithEntry(expected);

        // assert block was created successfully
        assertThat(expected.dirPath().resolve(expected.zipFileName()))
                .exists()
                .isRegularFile()
                .isReadable();

        final Optional<ArchiveBlockPath> actual = toTest.findArchivedBlock(blockNumber);
        assertThat(actual)
                .isNotNull()
                .isPresent()
                .get(InstanceOfAssertFactories.type(ArchiveBlockPath.class))
                .returns(expected.blockNumber(), ArchiveBlockPath::blockNumber)
                .returns(expected.dirPath(), ArchiveBlockPath::dirPath)
                .returns(expected.zipFileName(), ArchiveBlockPath::zipFileName)
                .returns(expected.zipEntryName(), ArchiveBlockPath::zipEntryName)
                .returns(CompressionType.ZSTD, ArchiveBlockPath::compressionType);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findArchivedBlock(long)} correctly returns an
     * empty {@link Optional} when a block is not found.
     *
     * @param blockNumber parameterized, valid block number
     * @param expectedBlockFile parameterized, expected block file
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testArchiveBlockNotFound(final long blockNumber, final String expectedBlockFile) {
        final Path expected = testLiveRootPath.resolve(expectedBlockFile);

        // assert block does not exist
        assertThat(expected).doesNotExist();

        final Optional<ArchiveBlockPath> actual = toTest.findArchivedBlock(blockNumber);
        assertThat(actual).isNotNull().isEmpty();
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#findArchivedBlock(long)} correctly throws an
     * {@link IllegalArgumentException} when an invalid block number
     * is provided.
     *
     * @param blockNumber parameterized, invalid block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumberFindArchiveBlock(final long blockNumber) {
        assertThatIllegalArgumentException().isThrownBy(() -> toTest.findArchivedBlock(blockNumber));
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#existsVerifiedBlock(long)} correctly
     * returns {@code true} when a block is found.
     *
     * @param blockNumber parameterized, valid block number
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testTrueExistsVerified(final long blockNumber, final String blockFile) throws IOException {
        final boolean notExistYet = toTest.existsVerifiedBlock(blockNumber);
        assertThat(notExistYet).isFalse();

        final Path expected = testLiveRootPath.resolve(blockFile);
        Files.createDirectories(expected.getParent());
        Files.createFile(expected);

        assertThat(expected).exists().isRegularFile().isReadable();

        final boolean actual = toTest.existsVerifiedBlock(blockNumber);
        assertThat(actual).isTrue();
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#existsVerifiedBlock(long)} correctly
     * returns {@code false} when a verified block is not found.
     *
     * @param blockNumber parameterized, valid block number
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testFalseExistsVerified(final long blockNumber, final String blockFile) {
        final Path expected = testLiveRootPath.resolve(blockFile);

        assertThat(expected).doesNotExist();

        final boolean actual = toTest.existsVerifiedBlock(blockNumber);
        assertThat(actual).isFalse();
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#existsVerifiedBlock(long)} correctly
     * returns {@code false} when an unverified block is found.
     *
     * @param blockNumber parameterized, valid block number
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testFalseExistsUnverified(final long blockNumber, final String blockFile) throws IOException {
        final boolean notExistYet = toTest.existsVerifiedBlock(blockNumber);
        assertThat(notExistYet).isFalse();

        final String unverifiedFilename =
                blockFile.replace(Constants.BLOCK_FILE_EXTENSION, Constants.UNVERIFIED_BLOCK_FILE_EXTENSION);
        final Path expected = testLiveRootPath.resolve(unverifiedFilename);
        Files.createDirectories(expected.getParent());
        Files.createFile(expected);

        assertThat(expected).exists().isRegularFile().isReadable();

        final boolean actual = toTest.existsVerifiedBlock(blockNumber);
        assertThat(actual).isFalse();
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#resolveRawArchivePath(long)}
     * correctly resolves the path to an archived block by a given number.
     *
     * @param rawBlockPath parameterized, raw block path, needs resolution
     * @param blockNumber parameterized, valid block number
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbersArchivePathResolve")
    void testSuccessfulArchivePathResolve(final ArchiveBlockPath rawBlockPath, final long blockNumber) {
        final ArchiveBlockPath expected = new ArchiveBlockPath(
                testLiveRootPath.resolve(rawBlockPath.dirPath()),
                rawBlockPath.zipFileName(),
                rawBlockPath.zipEntryName(),
                rawBlockPath.compressionType(),
                rawBlockPath.blockNumber());
        final ArchiveBlockPath actual = toTest.resolveRawArchivePath(blockNumber);
        assertThat(actual)
                .isNotNull()
                .returns(expected.blockNumber(), ArchiveBlockPath::blockNumber)
                .returns(expected.dirPath(), ArchiveBlockPath::dirPath)
                .returns(expected.zipFileName(), ArchiveBlockPath::zipFileName)
                .returns(expected.zipEntryName(), ArchiveBlockPath::zipEntryName)
                .returns(expected.compressionType(), ArchiveBlockPath::compressionType);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#markVerified (long)} correctly
     * marks a block as verified.
     *
     * @param blockNumber parameterized, invalid block number
     * @param expectedBlockFile parameterized, expected block file
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testMarkVerified(final long blockNumber, final String expectedBlockFile) throws IOException {
        final Path verifiedExpectedPath = testLiveRootPath.resolve(Path.of(expectedBlockFile));
        assertThat(verifiedExpectedPath).doesNotExist();

        final String expectedUnverified =
                expectedBlockFile.replace(Constants.BLOCK_FILE_EXTENSION, Constants.UNVERIFIED_BLOCK_FILE_EXTENSION);
        final Path unverifiedExpectedPath = testLiveRootPath.resolve(expectedUnverified);
        Files.createDirectories(unverifiedExpectedPath.getParent());
        Files.createFile(unverifiedExpectedPath);

        assertThat(unverifiedExpectedPath).exists().isRegularFile().isReadable();

        toTest.markVerified(blockNumber);

        assertThat(unverifiedExpectedPath).doesNotExist();
        assertThat(verifiedExpectedPath).exists().isRegularFile().isReadable();
    }

    /**
     * Some valid block numbers.
     *
     * @return a stream of valid block numbers
     */
    static Stream<Arguments> validBlockNumbers() {
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
    static Stream<Arguments> invalidBlockNumbers() {
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

    /**
     * Some valid block numbers and their corresponding
     * {@link ArchiveBlockPath} instances. This will work if archive group
     * size is 10!
     *
     * @return a stream of valid block numbers and their corresponding
     * {@link ArchiveBlockPath} instances
     */
    static Stream<Arguments> validBlockNumbersArchivePathResolve() {
        // spotless:off
        return Stream.of(
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0000000000000000000.blk", CompressionType.NONE, 0L), 0L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0000000000000000001.blk", CompressionType.NONE, 1L), 1L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0000000000000000002.blk", CompressionType.NONE, 2L), 2L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/"), "1.zip", "0000000000000000010.blk", CompressionType.NONE, 10L), 10L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/"), "0.zip", "0000000000000000100.blk", CompressionType.NONE, 100L), 100L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/"), "0.zip", "0000000000000001000.blk", CompressionType.NONE, 1_000L), 1_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/"), "0.zip", "0000000000000010000.blk", CompressionType.NONE, 10_000L), 10_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/"), "0.zip", "0000000000000100000.blk", CompressionType.NONE, 100_000L), 100_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/"), "0.zip", "0000000000001000000.blk", CompressionType.NONE, 1_000_000L), 1_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/"), "0.zip", "0000000000010000000.blk", CompressionType.NONE, 10_000_000L), 10_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/"), "0.zip", "0000000000100000000.blk", CompressionType.NONE, 100_000_000L), 100_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/"), "0.zip", "0000000001000000000.blk", CompressionType.NONE, 1_000_000_000L), 1_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/"), "0.zip", "0000000010000000000.blk", CompressionType.NONE, 10_000_000_000L), 10_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/"), "0.zip", "0000000100000000000.blk", CompressionType.NONE, 100_000_000_000L), 100_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0000001000000000000.blk", CompressionType.NONE, 1_000_000_000_000L), 1_000_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0000010000000000000.blk", CompressionType.NONE, 10_000_000_000_000L), 10_000_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0000100000000000000.blk", CompressionType.NONE, 100_000_000_000_000L), 100_000_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0001000000000000000.blk", CompressionType.NONE, 1_000_000_000_000_000L), 1_000_000_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0010000000000000000.blk", CompressionType.NONE, 10_000_000_000_000_000L), 10_000_000_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "0100000000000000000.blk", CompressionType.NONE, 100_000_000_000_000_000L), 100_000_000_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/"), "0.zip", "1000000000000000000.blk", CompressionType.NONE, 1_000_000_000_000_000_000L), 1_000_000_000_000_000_000L),
                Arguments.of(new ArchiveBlockPath(Path.of("9/2/2/3/3/7/2/0/3/6/8/5/4/7/7/5/8/"), "0.zip", "9223372036854775807.blk", CompressionType.NONE, Long.MAX_VALUE), Long.MAX_VALUE));
         // spotless:on
    }

    private void createTestZipWithEntry(final ArchiveBlockPath archiveBlockPath) throws IOException {
        Files.createDirectories(archiveBlockPath.dirPath());
        try (final OutputStream zipOutputStream =
                        Files.newOutputStream(archiveBlockPath.dirPath().resolve(archiveBlockPath.zipFileName()));
                final ZipOutputStream zipOut = new ZipOutputStream(zipOutputStream)) {
            zipOut.putNextEntry(new ZipEntry(archiveBlockPath.zipEntryName()));
            zipOut.closeEntry();
        }
    }
}
