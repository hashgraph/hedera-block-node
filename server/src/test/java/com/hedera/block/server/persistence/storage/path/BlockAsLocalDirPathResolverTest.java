// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
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
 * Tests for the {@link BlockAsLocalDirPathResolver} class.
 */
@SuppressWarnings("FieldCanBeLocal")
class BlockAsLocalDirPathResolverTest {
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;

    @TempDir
    private Path testLiveRootPath;

    private BlockAsLocalDirPathResolver toTest;

    @BeforeEach
    void setUp() throws IOException {
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY, testLiveRootPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);

        final String testConfigLiveRootPath = testConfig.liveRootPath();
        assertThat(testConfigLiveRootPath).isEqualTo(testLiveRootPath.toString());
        toTest = BlockAsLocalDirPathResolver.of(testConfig);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalDirPathResolver#resolveLiveRawPathToBlock(long)}
     * correctly resolves the path to a block by a given number. For the
     * block-as-local-directory storage strategy, the path to a block is simply
     * the live root path appended with the given block number.
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
     * {@link BlockAsLocalDirPathResolver#resolveLiveRawPathToBlock(long)} correctly
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
     * {@link BlockAsLocalDirPathResolver#resolveArchiveRawPathToBlock(long)}
     * correctly resolves the path to a block by a given number. For the
     * block-as-local-directory storage strategy, the path to a block is simply
     * the live root path appended with the given block number.
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
     * {@link BlockAsLocalDirPathResolver#resolveArchiveRawPathToBlock(long)} correctly
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
     * {@link BlockAsLocalDirPathResolver#findBlock(long)} correctly finds a
     * block with the given block number.
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
     * {@link BlockAsLocalDirPathResolver#findBlock(long)} correctly returns an
     * empty {@link Optional} when a block with the given block number does not
     * exist.
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
     * {@link BlockAsLocalDirPathResolver#findBlock(long)} correctly throws an
     * {@link IllegalArgumentException} when an invalid block number is invalid.
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
                Arguments.of(0L, "0"),
                Arguments.of(1L, "1"),
                Arguments.of(2L, "2"),
                Arguments.of(10L, "10"),
                Arguments.of(100L, "100"),
                Arguments.of(1_000L, "1000"),
                Arguments.of(10_000L, "10000"),
                Arguments.of(100_000L, "100000"),
                Arguments.of(1_000_000L, "1000000"),
                Arguments.of(10_000_000L, "10000000"),
                Arguments.of(100_000_000L, "100000000"),
                Arguments.of(1_000_000_000L, "1000000000"),
                Arguments.of(10_000_000_000L, "10000000000"),
                Arguments.of(100_000_000_000L, "100000000000"),
                Arguments.of(1_000_000_000_000L, "1000000000000"),
                Arguments.of(10_000_000_000_000L, "10000000000000"),
                Arguments.of(100_000_000_000_000L, "100000000000000"),
                Arguments.of(1_000_000_000_000_000L, "1000000000000000"),
                Arguments.of(10_000_000_000_000_000L, "10000000000000000"),
                Arguments.of(100_000_000_000_000_000L, "100000000000000000"),
                Arguments.of(1_000_000_000_000_000_000L, "1000000000000000000"),
                Arguments.of(Long.MAX_VALUE, String.valueOf(Long.MAX_VALUE)));
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
