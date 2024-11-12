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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link MathUtilities} functionality.
 */
class MathUtilitiesTest {
    /**
     * This test aims to verify that the {@link MathUtilities#isPowerOfTwo(int)}
     * returns {@code true} if the input number is a power of two.
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#powerOfTwoIntegers")
    void testIsPowerOfTwoPass(final int toTest) {
        final boolean actual = MathUtilities.isPowerOfTwo(toTest);
        assertThat(actual).isTrue();
    }

    /**
     * This test aims to verify that the {@link MathUtilities#isPowerOfTwo(int)}
     * returns {@code false} if the input number is not a power of two.
     */
    @ParameterizedTest
    @MethodSource({
        "com.hedera.block.common.CommonsTestUtility#nonPowerOfTwoIntegers",
        "com.hedera.block.common.CommonsTestUtility#negativePowerOfTwoIntegers"
    })
    void testIsPowerOfTwoFail(final int toTest) {
        final boolean actual = MathUtilities.isPowerOfTwo(toTest);
        assertThat(actual).isFalse();
    }
}
