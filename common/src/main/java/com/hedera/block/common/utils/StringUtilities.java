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

import java.util.Objects;

/** A utility class that deals with logic related to Strings. */
public final class StringUtilities {
    /**
     * This method takes an input {@link String} and checks if it is blank.
     * A {@link String} is considered blank if it is either {@code null} or
     * contains only whitespace characters as defined by
     * {@link String#isBlank()}.
     *
     * @param toCheck a {@link String} to check if it is blank
     * @return {@code true} if the given {@link String} to check is either
     * {@code null} or contains only whitespace characters, {@code false}
     * otherwise
     */
    public static boolean isBlank(final String toCheck) {
        return Objects.isNull(toCheck) || toCheck.isBlank();
    }

    private StringUtilities() {}
}
