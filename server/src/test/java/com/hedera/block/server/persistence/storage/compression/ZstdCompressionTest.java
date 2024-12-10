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

package com.hedera.block.server.persistence.storage.compression;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.luben.zstd.ZstdOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ZstdCompressionTest {
    @TempDir
    private Path testTempDir = Path.of("src/test/resources/tempDir");

    private ZstdCompression toTest;

    @BeforeEach
    void setUp() {
        toTest = new ZstdCompression();
    }

    /**
     * This test aims to verify that the
     * {@link ZstdCompression#getCompressionFileExtension()} method returns the
     * Zstandard (Zstd) compression file extension ".zstd".
     */
    @Test
    void testGetCompressionFileExtension() {
        assertThat(toTest.getCompressionFileExtension())
                .isNotNull()
                .isNotBlank()
                .isEqualTo(".zstd");
    }

    /**
     * This test aims to verify that the
     * {@link ZstdCompression#newCompressingOutputStream(Path)} creates a new
     * {@link OutputStream} instance that will run the input data through the
     * Zstandard (Zstd) compression algorithm before writing it to it`s
     * destination.
     *
     * @param testData parameterized, test data
     * @throws IOException if an I/O exception occurs
     */
    @ParameterizedTest
    @MethodSource("testData")
    void testSuccessfulCompression(final String testData) throws IOException {
        final Path rawTargetPath = testTempDir.resolve("successfulCompression.txt");
        // assert that the raw target file does not exist
        assertThat(rawTargetPath).doesNotExist();

        final Path actual =
                rawTargetPath.resolveSibling(rawTargetPath.getFileName() + toTest.getCompressionFileExtension());
        // assert that the target file does not exist yet
        assertThat(actual).doesNotExist();

        final byte[] byteArrayTestData = testData.getBytes(StandardCharsets.UTF_8);
        try (final OutputStream out = toTest.newCompressingOutputStream(rawTargetPath)) {
            out.write(byteArrayTestData);
        }
        assertThat(actual)
                .exists()
                .isReadable()
                .isRegularFile()
                .isNotEmptyFile()
                .hasSameBinaryContentAs(actualZstdCompression(byteArrayTestData));
        assertThat(rawTargetPath).doesNotExist();
    }

    private Path actualZstdCompression(final byte[] byteArrayTestData) throws IOException {
        final Path tempFile = testTempDir.resolve("tempComparisonFile.txt.zstd");
        try (final ZstdOutputStream out = new ZstdOutputStream(Files.newOutputStream(tempFile))) {
            out.write(byteArrayTestData);
            return tempFile;
        }
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of(" "),
                Arguments.of("\t "),
                Arguments.of("Some Random test data"),
                Arguments.of("Other Random test data"),
                Arguments.of("11110000"),
                Arguments.of(" a "),
                Arguments.of("\t a "),
                Arguments.of("\n a "));
    }
}
