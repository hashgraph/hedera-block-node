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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

/**
 * Tests for {@link NoOpCompression}.
 */
class NoOpCompressionTest {
    @TempDir
    private Path testTempDir;

    private NoOpCompression toTest;

    @BeforeEach
    void setUp() {
        toTest = new NoOpCompression();
    }

    /**
     * This test aims to verify that the
     * {@link NoOpCompression#getCompressionFileExtension()} method returns a
     * blank string.
     */
    @Test
    void testGetCompressionFileExtension() {
        assertThat(toTest.getCompressionFileExtension()).isNotNull().isBlank();
    }

    /**
     * This test aims to verify that the
     * {@link NoOpCompression#newCompressingOutputStream} enforce the API
     * contract for the precondition of the input path not being a directory.
     */
    @Test
    void testPreconditionDirectoryNotAllowed() throws IOException {
        final Path directory = testTempDir.resolve("path_as_dir");
        Files.createDirectories(directory);

        // assert that the target directory exists
        assertThat(directory).exists().isDirectory();

        final String expectedErrorMessage = "The input path [%s] must not be a directory!".formatted(directory);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> toTest.newCompressingOutputStream(directory))
                .withMessage(expectedErrorMessage);
    }

    /**
     * This test aims to verify that the
     * {@link NoOpCompression#newCompressingOutputStream} enforce the API
     * contract for the precondition of the path to the parent directory of the
     * input path existing.
     */
    @Test
    void testPreconditionParentDirectoryMustExist() {
        final Path pathWithNonExistentParent =
                testTempDir.resolve("path_as_dir").resolve("tmp.txt");

        // assert that the parent directory does not exist
        assertThat(pathWithNonExistentParent.getParent()).doesNotExist();

        final String expectedErrorMessage = "The path to the parent directory of the input path [%s] must exist!"
                .formatted(pathWithNonExistentParent);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> toTest.newCompressingOutputStream(pathWithNonExistentParent))
                .withMessage(expectedErrorMessage);
    }

    /**
     * This test aims to verify that the
     * {@link NoOpCompression#newCompressingOutputStream(Path)} creates a new
     * {@link OutputStream} instance that writes the input data to it`s
     * destination as it is received, without any compression.
     *
     * @param testData parameterized, test data
     * @throws IOException if an I/O exception occurs
     */
    @ParameterizedTest
    @MethodSource("testData")
    void testSuccessfulCompression(final String testData) throws IOException {
        final Path actual = testTempDir.resolve("successfulCompression.txt");

        // assert that the target file does not exist yet
        assertThat(actual).doesNotExist();

        final byte[] byteArrayTestData = testData.getBytes(StandardCharsets.UTF_8);
        try (final OutputStream out = toTest.newCompressingOutputStream(actual)) {
            out.write(byteArrayTestData);
        }
        assertThat(actual)
                .exists()
                .isReadable()
                .isRegularFile()
                .hasSameBinaryContentAs(actualNoOpCompression(byteArrayTestData));
    }

    private Path actualNoOpCompression(final byte[] byteArrayTestData) throws IOException {
        final Path tempFile = testTempDir.resolve("tempComparisonFile.txt");
        try (final OutputStream out = Files.newOutputStream(tempFile)) {
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
