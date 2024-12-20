// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.common.utils;

import edu.umd.cs.findbugs.annotations.NonNull;

/** A utility class used to assert various preconditions. */
public final class Preconditions {
    private static final String DEFAULT_NOT_BLANK_MESSAGE = "The input String is required to be non-blank.";
    private static final String DEFAULT_REQUIRE_POSITIVE_MESSAGE = "The input number [%d] is required to be positive.";
    private static final String DEFAULT_GT_OR_EQ_MESSAGE =
            "The input number [%d] is required to be greater or equal than [%d].";
    private static final String DEFAULT_REQUIRE_IN_RANGE_MESSAGE =
            "The input number [%d] is required to be in the range [%d, %d] boundaries included.";
    private static final String DEFAULT_REQUIRE_WHOLE_MESSAGE =
            "The input number [%d] is required to be a whole number.";
    private static final String DEFAULT_REQUIRE_POWER_OF_TWO_MESSAGE =
            "The input number [%d] is required to be a power of two.";

    /**
     * This method asserts a given {@link String} is not blank.
     * A blank {@link String} is one that is either {@code null} or contains
     * only whitespaces as defined by {@link String#isBlank()}. If the given
     * {@link String} is not blank, then we return it, else we throw an
     * {@link IllegalArgumentException}.
     *
     * @param toCheck a {@link String} to be checked if is blank as defined above
     * @return the {@link String} to be checked if it is not blank as defined above
     * @throws IllegalArgumentException if the input {@link String} to be
     * checked is blank
     */
    public static String requireNotBlank(final String toCheck) {
        return requireNotBlank(toCheck, DEFAULT_NOT_BLANK_MESSAGE);
    }

    /**
     * This method asserts a given {@link String} is not blank.
     * A blank {@link String} is one that is either {@code null} or contains
     * only whitespaces as defined by {@link String#isBlank()}. If the given
     * {@link String} is not blank, then we return it, else we throw an
     * {@link IllegalArgumentException}.
     *
     * @param toCheck a {@link String} to be checked if is blank as defined above
     * @param errorMessage the error message to be used if the precondition
     * check fails, must not be {@code null}
     * @return the {@link String} to be checked if it is not blank as defined above
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
     * This method asserts a given integer is a positive.
     * An integer is positive if it is NOT equal to zero and is greater than zero.
     *
     * @param toCheck the number to check if it is a positive power of two
     * @return the number to check if it is positive
     * @throws IllegalArgumentException if the input number to check is not positive
     */
    public static int requirePositive(final int toCheck) {
        return requirePositive(toCheck, DEFAULT_REQUIRE_POSITIVE_MESSAGE);
    }

    /**
     * This method asserts a given integer is a positive.
     * An integer is positive if it is NOT equal to zero and is greater than zero.
     *
     * @param toCheck the integer to check if it is a positive power of two
     * @param errorMessage a formatted string with one decimal parameters for
     * {@code toCheck}, must not be {@code null}.<br/>
     * Example error message: {@value #DEFAULT_REQUIRE_POSITIVE_MESSAGE}
     * @return the number to check if it is positive
     * @throws IllegalArgumentException if the input integer to check is not positive
     * @see java.util.Formatter for more information on error message formatting
     */
    public static int requirePositive(final int toCheck, @NonNull final String errorMessage) {
        if (0 >= toCheck) {
            throw new IllegalArgumentException(errorMessage.formatted(toCheck));
        } else {
            return toCheck;
        }
    }

    /**
     * This method asserts a given long is a positive.
     * A long is positive if it is NOT equal to zero and is greater than zero.
     *
     * @param toCheck the long to check if it is a positive power of two
     * @return the long to check if it is positive
     * @throws IllegalArgumentException if the input long to check is not positive
     */
    public static long requirePositive(final long toCheck) {
        return requirePositive(toCheck, DEFAULT_REQUIRE_POSITIVE_MESSAGE);
    }

    /**
     * This method asserts a given long is a positive.
     * A long is positive if it is NOT equal to zero and is greater than zero.
     *
     * @param toCheck the long to check if it is a positive power of two
     * @param errorMessage a formatted string with one decimal parameters for
     * {@code toCheck}, must not be {@code null}.<br/>
     * Example error message: {@value #DEFAULT_REQUIRE_POSITIVE_MESSAGE}
     * @return the number to check if it is positive
     * @throws IllegalArgumentException if the input long to check is not positive
     * @see java.util.Formatter for more information on error message formatting
     */
    public static long requirePositive(final long toCheck, @NonNull final String errorMessage) {
        if (0L >= toCheck) {
            throw new IllegalArgumentException(errorMessage.formatted(toCheck));
        } else {
            return toCheck;
        }
    }

    /**
     * Ensures that a given long value is greater than or equal to a specified
     * base value.
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
     * @return the input {@code toTest} if it is greater than or equal to {@code base}
     * @throws IllegalArgumentException if {@code toTest} is less than {@code base}
     */
    public static long requireGreaterOrEqual(final long toTest, final long base) {
        return requireGreaterOrEqual(toTest, base, DEFAULT_GT_OR_EQ_MESSAGE);
    }

    /**
     * Ensures that a given long value is greater than or equal to a specified
     * base value.
     * If the value does not meet the requirement, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param toTest the long value to test
     * @param base the base value to compare against
     * @param errorMessage a formatted string with two decimal parameters for
     * {@code toTest} and {@code base}, must not be {@code null}.<br/>
     * Example error message: {@value #DEFAULT_GT_OR_EQ_MESSAGE}
     * @return the number to check if it is greater than or equal to the base
     * @throws IllegalArgumentException if the input long toTest is not greater
     * than or equal to the base
     * @see java.util.Formatter for more information on error message formatting
     */
    public static long requireGreaterOrEqual(final long toTest, final long base, @NonNull final String errorMessage) {
        if (toTest >= base) {
            return toTest;
        } else {
            throw new IllegalArgumentException(errorMessage.formatted(toTest, base));
        }
    }

    /**
     * This method asserts a given int is within a range (boundaries included).
     * If the given int is within the range, then we return it, else, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param toCheck the int value to test
     * @param lowerBoundary the lower boundary
     * @param upperBoundary the upper boundary
     * @return the input {@code toCheck} if it is within the range (boundaries included)
     * @throws IllegalArgumentException if the input int does not pass the test
     */
    public static int requireInRange(final int toCheck, final int lowerBoundary, final int upperBoundary) {
        return requireInRange(toCheck, lowerBoundary, upperBoundary, DEFAULT_REQUIRE_IN_RANGE_MESSAGE);
    }

    /**
     * This method asserts a given int is within a range (boundaries included).
     * If the given int is within the range, then we return it, else, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param toCheck the int value to check
     * @param lowerBoundary the lower boundary
     * @param upperBoundary the upper boundary
     * @param errorMessage a formatted string with three decimal parameters for
     * {@code toTest}, {@code upperBoundary} and {@code lowerBoundary}, must not
     * be {@code null}.<br/>
     * Example error message: {@value #DEFAULT_REQUIRE_IN_RANGE_MESSAGE}
     * @return the input {@code toCheck} if it is within the range (boundaries included)
     * @throws IllegalArgumentException if the input int does not pass the test
     * @see java.util.Formatter for more information on error message formatting
     */
    public static int requireInRange(
            final int toCheck, final int lowerBoundary, final int upperBoundary, @NonNull final String errorMessage) {
        if (toCheck >= lowerBoundary && toCheck <= upperBoundary) {
            return toCheck;
        } else {
            throw new IllegalArgumentException(errorMessage.formatted(toCheck, lowerBoundary, upperBoundary));
        }
    }

    /**
     * This method asserts a given long is a whole number.
     * A long is whole if it is greater or equal to zero.
     *
     * @param toCheck the long to check if it is a whole number
     * @return the number to check if it is whole number
     * @throws IllegalArgumentException if the input number to check is not positive
     */
    public static long requireWhole(final long toCheck) {
        return requireWhole(toCheck, DEFAULT_REQUIRE_WHOLE_MESSAGE);
    }

    /**
     * This method asserts a given long is a whole number.
     * A long is whole if it is greater or equal to zero.
     *
     * @param toCheck the long to check if it is a whole number
     * @param errorMessage a formatted string with one decimal parameters for
     * {@code toCheck}, must not be {@code null}.<br/>
     * Example error message: {@value #DEFAULT_REQUIRE_WHOLE_MESSAGE}
     * @return the number to check if it is whole number
     * @throws IllegalArgumentException if the input number to check is not positive
     * @see java.util.Formatter for more information on error message formatting
     */
    public static long requireWhole(final long toCheck, @NonNull final String errorMessage) {
        if (toCheck >= 0) {
            return toCheck;
        } else {
            throw new IllegalArgumentException(errorMessage.formatted(toCheck));
        }
    }

    /**
     * This method asserts a given integer is a power of two.
     *
     * @param toCheck the number to check if it is a power of two
     * @return the number to check if it is a power of two
     * @throws IllegalArgumentException if the input number to check is not a
     * power of two
     */
    public static int requirePowerOfTwo(final int toCheck) {
        return requirePowerOfTwo(toCheck, DEFAULT_REQUIRE_POWER_OF_TWO_MESSAGE);
    }

    /**
     * This method asserts a given integer is a power of two.
     *
     * @param toCheck the number to check if it is a power of two
     * @param errorMessage a formatted string with one decimal parameter for
     * {@code toCheck}, must not be {@code null}.<br/>
     * Example error message: {@value #DEFAULT_REQUIRE_POWER_OF_TWO_MESSAGE}
     * @return the number to check if it is a power of two
     * @throws IllegalArgumentException if the input number to check is not a
     * power of two
     * @see java.util.Formatter for more information on error message formatting
     */
    public static int requirePowerOfTwo(final int toCheck, @NonNull final String errorMessage) {
        if (!MathUtilities.isPowerOfTwo(toCheck)) {
            throw new IllegalArgumentException(errorMessage.formatted(toCheck));
        } else {
            return toCheck;
        }
    }

    private Preconditions() {}
}
