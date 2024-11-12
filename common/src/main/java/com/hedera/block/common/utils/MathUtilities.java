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

    private MathUtilities() {}
}
