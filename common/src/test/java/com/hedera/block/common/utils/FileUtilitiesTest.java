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
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FileUtilitiesTest {
    // VALID PATHS & CONTENT
    private static final Path GZ_VALID1_PATH = Path.of("src/test/resources/valid1.txt.gz");
    private static final String GZ_VALID1_CONTENT = "valid1";

    private static final Path GZ_VALID2_PATH = Path.of("src/test/resources/valid2.txt.gz");
    private static final String GZ_VALID2_CONTENT = "valid2";

    // INVALID PATHS
    private static final Path GZ_INVALID1_PATH = Path.of("src/test/resources/invalid1.gz");
    private static final Path GZ_INVALID2_PATH = Path.of("src/test/resources/nonexistent.gz");

    @ParameterizedTest
    @MethodSource("validGzipFiles")
    void readGzipFileUnsafe_ReturnsByteArrayForValidGzipFile(
            final Path filePath, final String expectedContent) throws IOException {
        final byte[] actualContent = FileUtilities.readGzipFileUnsafe(filePath);
        assertThat(actualContent)
                .isNotNull()
                .isNotEmpty()
                .asString()
                .isNotNull()
                .isNotBlank()
                .isEqualTo(expectedContent);
    }

    @ParameterizedTest
    @MethodSource("invalidGzipFiles")
    void readGzipFileUnsafe_ThrowsIOExceptionForInvalidGzipFile(final Path filePath) {
        assertThatIOException().isThrownBy(() -> FileUtilities.readGzipFileUnsafe(filePath));
    }

    private static Stream<Arguments> validGzipFiles() {
        return Stream.of(
                Arguments.of(GZ_VALID1_PATH, GZ_VALID1_CONTENT),
                Arguments.of(GZ_VALID2_PATH, GZ_VALID2_CONTENT));
    }

    private static Stream<Arguments> invalidGzipFiles() {
        return Stream.of(Arguments.of(GZ_INVALID1_PATH), Arguments.of(GZ_INVALID2_PATH));
    }
}
