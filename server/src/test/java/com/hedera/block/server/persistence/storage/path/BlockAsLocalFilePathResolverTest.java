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

package com.hedera.block.server.persistence.storage.path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.hedera.block.server.Constants;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link BlockAsLocalFilePathResolver} class.
 */
class BlockAsLocalFilePathResolverTest {
    private static final long MAX_LONG_DIGITS = 19L;

    @TempDir
    private static Path TEST_LIVE_ROOT_PATH;

    private BlockAsLocalFilePathResolver toTest;

    @BeforeEach
    void setUp() {
        toTest = BlockAsLocalFilePathResolver.of(TEST_LIVE_ROOT_PATH);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#resolvePathToBlock(long)} correctly
     * resolves the path to a block by a given number. For the
     * block-as-file storage strategy, the path to a block is a trie structure
     * where each digit of the block number is a directory and the block number
     * itself is the file name.
     *
     * @param toResolve parameterized, valid block number
     * @param expected parameterized, expected path
     */
    @ParameterizedTest
    @MethodSource("validBlockNumbers")
    void testSuccessfulPathResolution(final long toResolve, final Path expected) {
        final Path actual = toTest.resolvePathToBlock(toResolve);
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFilePathResolver#resolvePathToBlock(long)} correctly
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

    private static Path expectedResolvedBlockPath(final long blockNumber) {
        final String rawBlockNumber = String.format("%0" + MAX_LONG_DIGITS + "d", blockNumber);
        final String[] blockPath = rawBlockNumber.split("");
        final String blockFileName = rawBlockNumber.concat(Constants.BLOCK_FILE_EXTENSION);
        blockPath[blockPath.length - 1] = blockFileName;
        return Path.of(TEST_LIVE_ROOT_PATH.toString(), blockPath);
    }

    /**
     * Some valid block numbers.
     *
     * @return a stream of valid block numbers
     */
    public static Stream<Arguments> validBlockNumbers() {
        return Stream.of(
                Arguments.of(0L, expectedResolvedBlockPath(0L)),
                Arguments.of(1L, expectedResolvedBlockPath(1L)),
                Arguments.of(2L, expectedResolvedBlockPath(2L)),
                Arguments.of(10L, expectedResolvedBlockPath(10L)),
                Arguments.of(100L, expectedResolvedBlockPath(100L)),
                Arguments.of(1_000L, expectedResolvedBlockPath(1_000L)),
                Arguments.of(10_000L, expectedResolvedBlockPath(10_000L)),
                Arguments.of(100_000L, expectedResolvedBlockPath(100_000L)),
                Arguments.of(1_000_000L, expectedResolvedBlockPath(1_000_000L)),
                Arguments.of(10_000_000L, expectedResolvedBlockPath(10_000_000L)),
                Arguments.of(100_000_000L, expectedResolvedBlockPath(100_000_000L)),
                Arguments.of(1_000_000_000L, expectedResolvedBlockPath(1_000_000_000L)),
                Arguments.of(10_000_000_000L, expectedResolvedBlockPath(10_000_000_000L)),
                Arguments.of(100_000_000_000L, expectedResolvedBlockPath(100_000_000_000L)),
                Arguments.of(1_000_000_000_000L, expectedResolvedBlockPath(1_000_000_000_000L)),
                Arguments.of(10_000_000_000_000L, expectedResolvedBlockPath(10_000_000_000_000L)),
                Arguments.of(100_000_000_000_000L, expectedResolvedBlockPath(100_000_000_000_000L)),
                Arguments.of(1_000_000_000_000_000L, expectedResolvedBlockPath(1_000_000_000_000_000L)),
                Arguments.of(10_000_000_000_000_000L, expectedResolvedBlockPath(10_000_000_000_000_000L)),
                Arguments.of(100_000_000_000_000_000L, expectedResolvedBlockPath(100_000_000_000_000_000L)),
                Arguments.of(1_000_000_000_000_000_000L, expectedResolvedBlockPath(1_000_000_000_000_000_000L)),
                Arguments.of(Long.MAX_VALUE, expectedResolvedBlockPath(Long.MAX_VALUE)));
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
