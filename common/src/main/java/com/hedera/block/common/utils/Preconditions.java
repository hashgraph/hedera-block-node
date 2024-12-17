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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;

/** A utility class used to assert various preconditions. */
public final class Preconditions {
    private static final String DEFAULT_NOT_BLANK_MESSAGE = "The input String is required to be non-blank.";
    private static final String DEFAULT_REQUIRED_POSITIVE_MESSAGE = "The input number [%d] is required to be positive.";

    /**
     * This method asserts a given {@link String} is not blank, meaning it is
     * not {@code null} or does not contain only whitespaces as defined by
     * {@link String#isBlank()}. If the given {@link String} is not blank, then
     * we return it, else we throw {@link IllegalArgumentException}.
     *
     * @param toCheck a {@link String} to be checked if is blank as defined
     * above
     * @return the {@link String} to be checked if it is not blank as defined
     * above
     * @throws IllegalArgumentException if the input {@link String} to be
     * checked is blank
     */
    public static String requireNotBlank(final String toCheck) {
        return requireNotBlank(toCheck, DEFAULT_NOT_BLANK_MESSAGE);
    }

    /**
     * This method asserts a given {@link String} is not blank, meaning it is
     * not {@code null} or does not contain only whitespaces as defined by
     * {@link String#isBlank()}. If the given {@link String} is not blank, then
     * we return it, else we throw {@link IllegalArgumentException}.
     *
     * @param toCheck a {@link String} to be checked if is blank as defined
     * above
     * @param errorMessage the error message to be used if the precondition
     * check fails. Must not be {@code null}!
     * @return the {@link String} to be checked if it is not blank as defined
     * above
     * @throws IllegalArgumentException if the input {@link String} to be
     * checked is blank
     */
    public static String requireNotBlank(final String toCheck, @NonNull final String errorMessage) {
        if (StringUtilities.isBlank(toCheck)) {
            throw new IllegalArgumentException(errorMessage);
        } else {
            return toCheck;
        }
    }

    /**
     * This method asserts a given integer is a positive. An integer is positive
     * if it is NOT equal to zero and is greater than zero.
     *
     * @param toCheck the number to check if it is a positive power of two
     * @return the number to check if it is positive
     * @throws IllegalArgumentException if the input number to check is not
     * positive
     */
    public static int requirePositive(final int toCheck) {
        return requirePositive(toCheck, DEFAULT_REQUIRED_POSITIVE_MESSAGE);
    }

    /**
     * This method asserts a given integer is a positive. An integer is positive
     * if it is NOT equal to zero and is greater than zero.
     *
     * @param toCheck the integer to check if it is a positive power of two
     * @param errorMessage a formatted string with one decimal parameters for
     * {@code toCheck}.<br/>
     * Example error message: {@code "The input number (%d) must be positive."}
     * @return the number to check if it is positive
     * @throws IllegalArgumentException if the input integer to check is not
     * positive
     * @see java.util.Formatter for more information on error message formatting
     */
    public static int requirePositive(final int toCheck, @NonNull final String errorMessage) {
        if (0 >= toCheck) {
            throw new IllegalArgumentException(errorMessage);
        } else {
            return toCheck;
        }
    }

    /**
     * This method asserts a given long is a positive. A long is positive
     * if it is NOT equal to zero and is greater than zero.
     *
     * @param toCheck the long to check if it is a positive power of two
     * @return the long to check if it is positive
     * @throws IllegalArgumentException if the input long to check is not
     * positive
     */
    public static long requirePositive(final long toCheck) {
        return requirePositive(toCheck, DEFAULT_REQUIRED_POSITIVE_MESSAGE);
    }

    /**
     * This method asserts a given long is a positive. A long is positive
     * if it is NOT equal to zero and is greater than zero.
     *
     * @param toCheck the long to check if it is a positive power of two
     * @param errorMessage a formatted string with one decimal parameters for
     * {@code toCheck}.<br/>
     * Example error message: {@code "The input number (%d) must be positive."}
     * @return the number to check if it is positive
     * @throws IllegalArgumentException if the input long to check is not
     * positive
     * @see java.util.Formatter for more information on error message formatting
     */
    public static long requirePositive(final long toCheck, @NonNull final String errorMessage) {
        if (0L >= toCheck) {
            throw new IllegalArgumentException(errorMessage);
        } else {
            return toCheck;
        }
    }

    /**
     * Ensures that a given long value is greater than or equal to a specified base
     * value.
     * If the value does not meet the requirement, an
     * {@link IllegalArgumentException} is thrown.
     *
     * <p>
     * This method delegates the validation to
     * {@link #requireGreaterOrEqual(long, long, String)},
     * using a default error message if the check fails.
     * </p>
     *
     * @param toTest the long value to test
     * @param base   the base value to compare against
     * @return the input {@code toTest} if it is greater than or equal to
     *         {@code base}
     * @throws IllegalArgumentException if {@code toTest} is less than {@code base}
     */
    public static long requireGreaterOrEqual(final long toTest, final long base) {
        return requireGreaterOrEqual(toTest, base, null);
    }

    /**
     * Ensures that a given long value is greater than or equal to a specified base
     * value.
     * If the value does not meet the requirement, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param toTest       the long value to test
     * @param base         the base value to compare against
     * @param errorMessage the error message to include in the exception if the
     *                     check fails;
     *                     if {@code null}, a default message is used
     * @return the input {@code toTest} if it is greater than or equal to
     *         {@code base}
     * @throws IllegalArgumentException if {@code toTest} is less than {@code base}
     */
    public static long requireGreaterOrEqual(final long toTest, final long base, final String errorMessage) {
        if (toTest >= base) {
            return toTest;
        }

        final String message = Objects.isNull(errorMessage)
                ? "The input integer [%d] is required be greater or equal than [%d].".formatted(toTest, base)
                : errorMessage;
        throw new IllegalArgumentException(message);
    }

    /**
     * This method asserts a given int is within a range (boundaries
     * included). If the given int is within the range, then we return it,
     * else, an {@link IllegalArgumentException} is thrown.
     *
     * @param toCheck the int value to test
     * @param lowerBoundary the lower boundary
     * @param upperBoundary the upper boundary
     * @return the input {@code toCheck} if it is within the range (boundaries
     * included)
     * @throws IllegalArgumentException if the input int does not pass the test
     */
    public static int requireInRange(final int toCheck, final int lowerBoundary, final int upperBoundary) {
        return requireInRange(toCheck, lowerBoundary, upperBoundary, null);
    }

    /**
     * This method asserts a given int is within a range (boundaries
     * included). If the given int is within the range, then we return it,
     * else, an {@link IllegalArgumentException} is thrown.
     *
     * @param toCheck the int value to check
     * @param lowerBoundary the lower boundary
     * @param upperBoundary the upper boundary
     * @param errorMessage the error message to be used in the exception if the
     * input int to test is not within the range, if null, a default message
     * will be used
     * @return the input {@code toCheck} if it is within the range (boundaries
     * included)
     * @throws IllegalArgumentException if the input int does not pass the test
     */
    public static int requireInRange(
            final int toCheck, final int lowerBoundary, final int upperBoundary, final String errorMessage) {
        if (toCheck >= lowerBoundary && toCheck <= upperBoundary) {
            return toCheck;
        } else {
            final String message = Objects.isNull(errorMessage)
                    ? "The input int [%d] is required to be in the range [%d, %d] boundaries included."
                            .formatted(toCheck, lowerBoundary, upperBoundary)
                    : errorMessage;
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * This method asserts a given long is a whole number. A long is whole
     * if it is greater or equal to zero.
     *
     * @param toCheck the long to check if it is a whole number
     * @return the number to check if it is whole number
     * @throws IllegalArgumentException if the input number to check is not
     *                                  positive
     */
    public static long requireWhole(final long toCheck) {
        return requireWhole(toCheck, null);
    }

    /**
     * This method asserts a given long is a whole number. A long is whole
     * if it is greater or equal to zero.
     *
     * @param toCheck      the long to check if it is a whole number
     * @param errorMessage the error message to be used in the exception if the
     *                     input long to check is not a whole number, if null, a
     *                     default message will
     *                     be used
     * @return the number to check if it is whole number
     * @throws IllegalArgumentException if the input number to check is not
     *                                  positive
     */
    public static long requireWhole(final long toCheck, final String errorMessage) {
        if (toCheck >= 0) {
            return toCheck;
        } else {
            final String message = Objects.isNull(errorMessage)
                    ? "The input integer [%d] is required be whole.".formatted(toCheck)
                    : errorMessage;
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * This method asserts a given integer is a power of two.
     *
     * @param toCheck the number to check if it is a power of two
     * @return the number to check if it is a power of two
     * @throws IllegalArgumentException if the input number to check is not a
     *                                  power of two
     */
    public static int requirePowerOfTwo(final int toCheck) {
        return requirePowerOfTwo(toCheck, null);
    }

    /**
     * This method asserts a given integer is a power of two.
     *
     * @param toCheck      the number to check if it is a power of two
     * @param errorMessage the error message to be used in the exception if the
     *                     input integer to check is not a power of two, if null, a
     *                     default message
     *                     will be used
     * @return the number to check if it is a power of two
     * @throws IllegalArgumentException if the input number to check is not a
     *                                  power of two
     */
    public static int requirePowerOfTwo(final int toCheck, final String errorMessage) {
        if (!MathUtilities.isPowerOfTwo(toCheck)) {
            final String message = Objects.isNull(errorMessage)
                    ? "The input integer [%d] is required to be a power of two.".formatted(toCheck)
                    : errorMessage;
            throw new IllegalArgumentException(message);
        } else {
            return toCheck;
        }
    }

    private Preconditions() {}
}
