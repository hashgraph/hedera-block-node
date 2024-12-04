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

    /**
     * Some valid block numbers.
     *
     * @return a stream of valid block numbers
     */
    public static Stream<Arguments> validBlockNumbers() {
        return Stream.of(
                Arguments.of(
                        0L, TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0000000000000000000.blk")),
                Arguments.of(
                        1L, TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0000000000000000001.blk")),
                Arguments.of(
                        2L, TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0000000000000000002.blk")),
                Arguments.of(
                        10L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0000000000000000010.blk")),
                Arguments.of(
                        100L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0000000000000000100.blk")),
                Arguments.of(
                        1_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0000000000000001000.blk")),
                Arguments.of(
                        10_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0000000000000010000.blk")),
                Arguments.of(
                        100_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0000000000000100000.blk")),
                Arguments.of(
                        1_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0000000000001000000.blk")),
                Arguments.of(
                        10_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0000000000010000000.blk")),
                Arguments.of(
                        100_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0000000000100000000.blk")),
                Arguments.of(
                        1_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0000000001000000000.blk")),
                Arguments.of(
                        10_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0000000010000000000.blk")),
                Arguments.of(
                        100_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0000000100000000000.blk")),
                Arguments.of(
                        1_000_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0000001000000000000.blk")),
                Arguments.of(
                        10_000_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0000010000000000000.blk")),
                Arguments.of(
                        100_000_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0000100000000000000.blk")),
                Arguments.of(
                        1_000_000_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0001000000000000000.blk")),
                Arguments.of(
                        10_000_000_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0010000000000000000.blk")),
                Arguments.of(
                        100_000_000_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("0/1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0100000000000000000.blk")),
                Arguments.of(
                        1_000_000_000_000_000_000L,
                        TEST_LIVE_ROOT_PATH.resolve("1/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/0/1000000000000000000.blk")),
                Arguments.of(
                        Long.MAX_VALUE,
                        TEST_LIVE_ROOT_PATH.resolve("9/2/2/3/3/7/2/0/3/6/8/5/4/7/7/5/8/0/9223372036854775807.blk")));
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
