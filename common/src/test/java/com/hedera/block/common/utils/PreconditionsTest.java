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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link Preconditions} functionality.
 */
class PreconditionsTest {
    /**
     * This test aims to verify that the
     * {@link Preconditions#requireNotBlank(String)} will return the input
     * 'toTest' parameter if the non-blank check passes. Test includes
     * overloads.
     *
     * @param toTest parameterized, the string to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#nonBlankStrings")
    void testRequireNotBlankPass(final String toTest) {
        final Consumer<String> asserts =
                actual -> assertThat(actual).isNotNull().isNotBlank().isEqualTo(toTest);

        final String actual = Preconditions.requireNotBlank(toTest);
        assertThat(actual).satisfies(asserts);

        final String actualOverload = Preconditions.requireNotBlank(toTest, "test error message");
        assertThat(actualOverload).satisfies(asserts);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requireNotBlank(String)} will throw an
     * {@link IllegalArgumentException} if the non-blank check fails. Test
     * includes overloads.
     *
     * @param toTest parameterized, the string to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#blankStrings")
    void testRequireNotBlankFail(final String toTest) {
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireNotBlank(toTest));

        final String testErrorMessage = "test error message";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireNotBlank(toTest, testErrorMessage))
                .withMessage(testErrorMessage);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requireWhole(long)} will return the input 'toTest'
     * parameter if the positive check passes. Test includes overloads.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#wholeNumbers")
    void testRequireWholePass(final int toTest) {
        final Consumer<Integer> asserts =
                actual -> assertThat(actual).isGreaterThanOrEqualTo(0).isEqualTo(toTest);

        final int actual = (int) Preconditions.requireWhole(toTest);
        assertThat(actual).satisfies(asserts);

        final int actualOverload = (int) Preconditions.requireWhole(toTest, "test error message");
        assertThat(actualOverload).satisfies(asserts);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requireWhole(long)} will throw an
     * {@link IllegalArgumentException} if the positive check fails. Test
     * includes overloads.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#negativeIntegers")
    void testRequireWholeFail(final int toTest) {
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireWhole(toTest));

        final String testErrorMessage = "test error message";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireWhole(toTest, testErrorMessage))
                .withMessage(testErrorMessage);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requireGreaterOrEqual(long, long)} will return the input
     * 'toTest' parameter if the check passes.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#validGreaterOrEqualValues")
    void testRequireGreaterOrEqualPass(final long toTest, final long base) {
        final Consumer<Long> asserts =
                actual -> assertThat(actual).isGreaterThanOrEqualTo(base).isEqualTo(toTest);

        final long actual = Preconditions.requireGreaterOrEqual(toTest, base);
        assertThat(actual).satisfies(asserts);

        final long actualOverload = Preconditions.requireGreaterOrEqual(toTest, base, "test error message");
        assertThat(actualOverload).satisfies(asserts);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requireGreaterOrEqual(long, long)} will throw an
     * {@link IllegalArgumentException} if the check fails.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#invalidGreaterOrEqualValues")
    void testRequireGreaterOrEqualFail(final long toTest, final long base) {
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireGreaterOrEqual(toTest, base));

        final String testErrorMessage = "test error message";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireGreaterOrEqual(toTest, base, testErrorMessage))
                .withMessage(testErrorMessage);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requirePositive(int)} will return the input 'toTest'
     * parameter if the positive check passes. Test includes overloads.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#positiveIntegers")
    void testRequirePositivePass(final int toTest) {
        final Consumer<Integer> asserts =
                actual -> assertThat(actual).isPositive().isEqualTo(toTest);

        final int actual = Preconditions.requirePositive(toTest);
        assertThat(actual).satisfies(asserts);

        final int actualOverload = Preconditions.requirePositive(toTest, "test error message");
        assertThat(actualOverload).satisfies(asserts);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requirePositive(int)} will throw an
     * {@link IllegalArgumentException} if the positive check fails. Test
     * includes overloads.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#zeroAndNegativeIntegers")
    void testRequirePositiveFail(final int toTest) {
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requirePositive(toTest));

        final String testErrorMessage = "test error message";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requirePositive(toTest, testErrorMessage))
                .withMessage(testErrorMessage);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requirePositive(long)} will return the input 'toTest'
     * parameter if the positive check passes. Test includes overloads.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#positiveIntegers")
    void testRequirePositiveLongPass(final long toTest) {
        final Consumer<Long> asserts = actual -> assertThat(actual).isPositive().isEqualTo(toTest);

        final long actual = Preconditions.requirePositive(toTest);
        assertThat(actual).satisfies(asserts);

        final long actualOverload = Preconditions.requirePositive(toTest, "test error message");
        assertThat(actualOverload).satisfies(asserts);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requirePositive(long)} will throw an
     * {@link IllegalArgumentException} if the positive check fails. Test
     * includes overloads.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#zeroAndNegativeIntegers")
    void testRequirePositiveLongFail(final long toTest) {
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requirePositive(toTest));

        final String testErrorMessage = "test error message";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requirePositive(toTest, testErrorMessage))
                .withMessage(testErrorMessage);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requirePowerOfTwo(int)} will return the input
     * 'toTest' parameter if the power of two check passes. Test includes
     * overloads.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource("com.hedera.block.common.CommonsTestUtility#powerOfTwoIntegers")
    void testRequirePowerOfTwoPass(final int toTest) {
        final Consumer<Integer> asserts =
                actual -> assertThat(actual).isPositive().isEqualTo(toTest);

        final int actual = Preconditions.requirePowerOfTwo(toTest);
        assertThat(actual).satisfies(asserts);

        final int actualOverload = Preconditions.requirePowerOfTwo(toTest, "test error message");
        assertThat(actualOverload).satisfies(asserts);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requirePowerOfTwo(int)} will throw an
     * {@link IllegalArgumentException} if the power of two check fails. Test
     * includes overloads.
     *
     * @param toTest parameterized, the number to test
     */
    @ParameterizedTest
    @MethodSource({
        "com.hedera.block.common.CommonsTestUtility#nonPowerOfTwoIntegers",
        "com.hedera.block.common.CommonsTestUtility#negativePowerOfTwoIntegers"
    })
    void testRequirePowerOfTwoFail(final int toTest) {
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requirePowerOfTwo(toTest));

        final String testErrorMessage = "test error message";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requirePowerOfTwo(toTest, testErrorMessage))
                .withMessage(testErrorMessage);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requireInRange(int, int, int)} will return the
     * input 'toTest' parameter if the range check passes. Test includes
     * overloads.
     *
     * @param toTest parameterized, the number to test
     * @param lowerBoundary parameterized, the lower boundary
     * @param upperBoundary parameterized, the upper boundary
     */
    @ParameterizedTest
    @MethodSource("validRequireInRangeValues")
    void testRequireInRangePass(final int toTest, final int lowerBoundary, final int upperBoundary) {
        final Consumer<Integer> asserts = actual ->
                assertThat(actual).isBetween(lowerBoundary, upperBoundary).isEqualTo(toTest);

        final int actual = Preconditions.requireInRange(toTest, lowerBoundary, upperBoundary);
        assertThat(actual).satisfies(asserts);

        final int actualOverload =
                Preconditions.requireInRange(toTest, lowerBoundary, upperBoundary, "test error message");
        assertThat(actualOverload).satisfies(asserts);
    }

    /**
     * This test aims to verify that the
     * {@link Preconditions#requireInRange(int, int, int)} will throw an
     * {@link IllegalArgumentException} if the range check fails. Test includes
     * overloads.
     *
     * @param toTest parameterized, the number to test
     * @param lowerBoundary parameterized, the lower boundary
     * @param upperBoundary parameterized, the upper boundary
     */
    @ParameterizedTest
    @MethodSource("invalidRequireInRangeValues")
    void testRequireInRangeFail(final int toTest, final int lowerBoundary, final int upperBoundary) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireInRange(toTest, lowerBoundary, upperBoundary));

        final String testErrorMessage = "test error message";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireInRange(toTest, lowerBoundary, upperBoundary, testErrorMessage))
                .withMessage(testErrorMessage);
    }

    private static Stream<Arguments> validRequireInRangeValues() {
        return Stream.of(
                Arguments.of(0, 0, 0),
                Arguments.of(0, 0, 1),
                Arguments.of(1, 0, 1),
                Arguments.of(1, 0, 2),
                Arguments.of(-1, -1, -1),
                Arguments.of(-2, -2, -1),
                Arguments.of(-1, -2, -1),
                Arguments.of(-1, -2, 0));
    }

    private static Stream<Arguments> invalidRequireInRangeValues() {
        return Stream.of(
                Arguments.of(0, 1, 1),
                Arguments.of(0, 1, 2),
                Arguments.of(1, 2, 3),
                Arguments.of(-1, 0, 1),
                Arguments.of(-1, 0, 0),
                Arguments.of(1, 0, 0));
    }
}
