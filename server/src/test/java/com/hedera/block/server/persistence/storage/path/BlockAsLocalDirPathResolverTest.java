// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link BlockAsLocalDirPathResolver} class.
 */
class BlockAsLocalDirPathResolverTest {
    @TempDir
    private static Path TEST_LIVE_ROOT_PATH;

    private BlockAsLocalDirPathResolver toTest;

    @BeforeEach
    void setUp() {
        toTest = BlockAsLocalDirPathResolver.of(TEST_LIVE_ROOT_PATH);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalDirPathResolver#resolvePathToBlock(long)} correctly
     * resolves the path to a block by a given number. For the
     * block-as-local-directory storage strategy, the path to a block is simply
     * the live root path appended with the given block number.
     *
     * @param toResolve parameterized, valid block number
     * @param expected parameterized, expected path
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulPathResolution(final long toResolve, final Path expected) {
        final Path actual = toTest.resolvePathToBlock(toResolve);
        assertThat(actual).isNotNull().isAbsolute().isEqualByComparingTo(expected);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalDirPathResolver#resolvePathToBlock(long)} correctly
     * throws an {@link IllegalArgumentException} when an invalid block number
     * is provided. A block number is invalid if it is a strictly negative number.
     *
     * @param toResolve parameterized, invalid block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumber(final long toResolve) {
        assertThatIllegalArgumentException().isThrownBy(() -> toTest.resolvePathToBlock(toResolve));
    }

    /**
     * Some valid block numbers.
     *
     * @return a stream of valid block numbers
     */
    public static Stream<Arguments> validBlockNumbers() {
        return Stream.of(
                Arguments.of(0L, TEST_LIVE_ROOT_PATH.resolve("0")),
                Arguments.of(1L, TEST_LIVE_ROOT_PATH.resolve("1")),
                Arguments.of(2L, TEST_LIVE_ROOT_PATH.resolve("2")),
                Arguments.of(10L, TEST_LIVE_ROOT_PATH.resolve("10")),
                Arguments.of(100L, TEST_LIVE_ROOT_PATH.resolve("100")),
                Arguments.of(1_000L, TEST_LIVE_ROOT_PATH.resolve("1000")),
                Arguments.of(10_000L, TEST_LIVE_ROOT_PATH.resolve("10000")),
                Arguments.of(100_000L, TEST_LIVE_ROOT_PATH.resolve("100000")),
                Arguments.of(1_000_000L, TEST_LIVE_ROOT_PATH.resolve("1000000")),
                Arguments.of(10_000_000L, TEST_LIVE_ROOT_PATH.resolve("10000000")),
                Arguments.of(100_000_000L, TEST_LIVE_ROOT_PATH.resolve("100000000")),
                Arguments.of(1_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("1000000000")),
                Arguments.of(10_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("10000000000")),
                Arguments.of(100_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("100000000000")),
                Arguments.of(1_000_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("1000000000000")),
                Arguments.of(10_000_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("10000000000000")),
                Arguments.of(100_000_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("100000000000000")),
                Arguments.of(1_000_000_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("1000000000000000")),
                Arguments.of(10_000_000_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("10000000000000000")),
                Arguments.of(100_000_000_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("100000000000000000")),
                Arguments.of(1_000_000_000_000_000_000L, TEST_LIVE_ROOT_PATH.resolve("1000000000000000000")),
                Arguments.of(Long.MAX_VALUE, TEST_LIVE_ROOT_PATH.resolve(String.valueOf(Long.MAX_VALUE))));
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
