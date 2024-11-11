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

package com.hedera.block.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link Preconditions} functionality.
 */
class PreconditionsTest {
    /**
     * This test aims to verify that the
     * {@link Preconditions#requireNotBlank(String)} will return the input
     * 'toTest' parameter if the non-blank check passes.
     *
     * @param toTest parameterized, the string to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#nonBlankStrings")
    void testRequireNotBlankPass(final String toTest) {
        final String actual = Preconditions.requireNotBlank(toTest);
        assertThat(actual).isNotNull().isNotBlank().isEqualTo(toTest);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requireNotBlank(String)} will throw an
     * {@link IllegalArgumentException} if the non-blank check fails.
     *
     * @param toTest parameterized, the string to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#blankStrings")
    void testRequireNotBlankFail(final String toTest) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Preconditions.requireNotBlank(toTest));
    }
}
