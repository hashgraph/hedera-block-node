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

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link NoOpBlockPathResolver} class.
 */
class NoOpBlockPathResolverTest {
    private NoOpBlockPathResolver toTest;

    @BeforeEach
    void setUp() {
        toTest = NoOpBlockPathResolver.newInstance();
    }

    /**
     * This test aims to verify that the
     * {@link NoOpBlockPathResolver#resolvePathToBlock(long)} correctly resolves
     * the path to a block by a given number. The no-op resolver does nothing,
     * always returns a path resolved under '/tmp' based on the blockNumber and
     * has no preconditions check. E.g. for blockNumber 0, the resolved path is
     * '/tmp/hashgraph/blocknode/data/0.tmp.blk'.
     *
     * @param toResolve parameterized, block number
     */
    @ParameterizedTest
    @MethodSource({"validBlockNumbers", "invalidBlockNumbers"})
    void testSuccessfulPathResolution(final long toResolve, final Path expected) {
        final Path actual = toTest.resolvePathToBlock(toResolve);
        assertThat(actual).isNotNull().isAbsolute().isEqualByComparingTo(expected);
    }

    /**
     * Some valid block numbers.
     *
     * @return a stream of valid block numbers
     */
    public static Stream<Arguments> validBlockNumbers() {
        return Stream.of(
                Arguments.of(0L, "/tmp/hashgraph/blocknode/data/0.tmp.blk"),
                Arguments.of(1L, "/tmp/hashgraph/blocknode/data/1.tmp.blk"),
                Arguments.of(2L, "/tmp/hashgraph/blocknode/data/2.tmp.blk"),
                Arguments.of(10L, "/tmp/hashgraph/blocknode/data/10.tmp.blk"),
                Arguments.of(100L, "/tmp/hashgraph/blocknode/data/100.tmp.blk"),
                Arguments.of(1_000L, "/tmp/hashgraph/blocknode/data/1000.tmp.blk"),
                Arguments.of(10_000L, "/tmp/hashgraph/blocknode/data/10000.tmp.blk"),
                Arguments.of(100_000L, "/tmp/hashgraph/blocknode/data/100000.tmp.blk"),
                Arguments.of(1_000_000L, "/tmp/hashgraph/blocknode/data/1000000.tmp.blk"),
                Arguments.of(10_000_000L, "/tmp/hashgraph/blocknode/data/10000000.tmp.blk"),
                Arguments.of(100_000_000L, "/tmp/hashgraph/blocknode/data/100000000.tmp.blk"),
                Arguments.of(1_000_000_000L, "/tmp/hashgraph/blocknode/data/1000000000.tmp.blk"),
                Arguments.of(10_000_000_000L, "/tmp/hashgraph/blocknode/data/10000000000.tmp.blk"),
                Arguments.of(100_000_000_000L, "/tmp/hashgraph/blocknode/data/100000000000.tmp.blk"),
                Arguments.of(1_000_000_000_000L, "/tmp/hashgraph/blocknode/data/1000000000000.tmp.blk"),
                Arguments.of(10_000_000_000_000L, "/tmp/hashgraph/blocknode/data/10000000000000.tmp.blk"),
                Arguments.of(100_000_000_000_000L, "/tmp/hashgraph/blocknode/data/100000000000000.tmp.blk"),
                Arguments.of(1_000_000_000_000_000L, "/tmp/hashgraph/blocknode/data/1000000000000000.tmp.blk"),
                Arguments.of(10_000_000_000_000_000L, "/tmp/hashgraph/blocknode/data/10000000000000000.tmp.blk"),
                Arguments.of(100_000_000_000_000_000L, "/tmp/hashgraph/blocknode/data/100000000000000000.tmp.blk"),
                Arguments.of(1_000_000_000_000_000_000L, "/tmp/hashgraph/blocknode/data/1000000000000000000.tmp.blk"),
                Arguments.of(Long.MAX_VALUE, "/tmp/hashgraph/blocknode/data/9223372036854775807.tmp.blk"));
    }

    /**
     * Some invalid block numbers.
     *
     * @return a stream of invalid block numbers
     */
    public static Stream<Arguments> invalidBlockNumbers() {
        return Stream.of(
                Arguments.of(-1L, "/tmp/hashgraph/blocknode/data/-1.tmp.blk"),
                Arguments.of(-2L, "/tmp/hashgraph/blocknode/data/-2.tmp.blk"),
                Arguments.of(-10L, "/tmp/hashgraph/blocknode/data/-10.tmp.blk"),
                Arguments.of(-100L, "/tmp/hashgraph/blocknode/data/-100.tmp.blk"),
                Arguments.of(-1_000L, "/tmp/hashgraph/blocknode/data/-1000.tmp.blk"),
                Arguments.of(-10_000L, "/tmp/hashgraph/blocknode/data/-10000.tmp.blk"),
                Arguments.of(-100_000L, "/tmp/hashgraph/blocknode/data/-100000.tmp.blk"),
                Arguments.of(-1_000_000L, "/tmp/hashgraph/blocknode/data/-1000000.tmp.blk"),
                Arguments.of(-10_000_000L, "/tmp/hashgraph/blocknode/data/-10000000.tmp.blk"),
                Arguments.of(-100_000_000L, "/tmp/hashgraph/blocknode/data/-100000000.tmp.blk"),
                Arguments.of(-1_000_000_000L, "/tmp/hashgraph/blocknode/data/-1000000000.tmp.blk"),
                Arguments.of(-10_000_000_000L, "/tmp/hashgraph/blocknode/data/-10000000000.tmp.blk"),
                Arguments.of(-100_000_000_000L, "/tmp/hashgraph/blocknode/data/-100000000000.tmp.blk"),
                Arguments.of(-1_000_000_000_000L, "/tmp/hashgraph/blocknode/data/-1000000000000.tmp.blk"),
                Arguments.of(-10_000_000_000_000L, "/tmp/hashgraph/blocknode/data/-10000000000000.tmp.blk"),
                Arguments.of(-100_000_000_000_000L, "/tmp/hashgraph/blocknode/data/-100000000000000.tmp.blk"),
                Arguments.of(-1_000_000_000_000_000L, "/tmp/hashgraph/blocknode/data/-1000000000000000.tmp.blk"),
                Arguments.of(-10_000_000_000_000_000L, "/tmp/hashgraph/blocknode/data/-10000000000000000.tmp.blk"),
                Arguments.of(-100_000_000_000_000_000L, "/tmp/hashgraph/blocknode/data/-100000000000000000.tmp.blk"),
                Arguments.of(-1_000_000_000_000_000_000L, "/tmp/hashgraph/blocknode/data/-1000000000000000000.tmp.blk"),
                Arguments.of(Long.MIN_VALUE, "/tmp/hashgraph/blocknode/data/-9223372036854775808.tmp.blk"));
    }
}
