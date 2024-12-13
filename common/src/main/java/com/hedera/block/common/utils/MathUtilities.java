// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.common.utils;

/**
 * A utility class that deals with logic related to Mathematics.
 */
public final class MathUtilities {
    /**
     * This method checks if the given number is a power of two.
     *
     * @param toCheck the number to check if it is a power of two
     * @return {@code true} if the given number is a power of two
     */
    public static boolean isPowerOfTwo(final int toCheck) {
        // mathematically powers of two are always positive numbers, so if the
        // input is negative or zero, it is not a power of two, and we do not
        // need to trigger the second check, hence we return false immediately
        // by short-circuiting the logical AND operation
        return (0 < toCheck) && ((toCheck & (toCheck - 1)) == 0);
    }

    /**
     * This method checks if the given number is even.
     *
     * @param toCheck the number to check if it is even
     * @return {@code true} if the given number is even
     */
    public static boolean isEven(final int toCheck) {
        return (toCheck % 2) == 0;
    }

    private MathUtilities() {}
}
