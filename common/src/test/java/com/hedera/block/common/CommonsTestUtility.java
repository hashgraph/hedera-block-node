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

package com.hedera.block.common;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Utilities for testing the common module.
 */
public final class CommonsTestUtility {
    /**
     * Some simple non-blank strings only with spaces and new lines and tabs (there are more whitespace chars).
     */
    public static Stream<Arguments> nonBlankStrings() {
        return Stream.of(
                Arguments.of("a"),
                Arguments.of(" a"),
                Arguments.of("a "),
                Arguments.of(" a "),
                Arguments.of("a b"),
                Arguments.of("\na"), // new line
                Arguments.of("\ta"), // tab
                Arguments.of("\fa"), // form feed
                Arguments.of("\ra"), // carriage return
                Arguments.of("\u000Ba"), // vertical tab
                Arguments.of("\u001Ca"), // file separator
                Arguments.of("\u001Da"), // group separator
                Arguments.of("\u001Ea"), // record separator
                Arguments.of("\u001Fa") // unit separator
                );
    }

    /**
     * Some simple blank strings.
     */
    public static Stream<Arguments> blankStrings() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of(" "),
                Arguments.of("\n"), // new line
                Arguments.of("\t"), // tab
                Arguments.of("\f"), // form feed
                Arguments.of("\r"), // carriage return
                Arguments.of("\u000B"), // vertical tab
                Arguments.of("\u001C"), // file separator
                Arguments.of("\u001D"), // group separator
                Arguments.of("\u001E"), // record separator
                Arguments.of("\u001F") // unit separator
                );
    }

    private CommonsTestUtility() {}
}
