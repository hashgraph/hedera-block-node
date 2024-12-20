// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.compression;

import static org.assertj.core.api.Assertions.assertThat;

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
        toTest = NoOpCompression.newInstance();
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
     * {@link NoOpCompression#wrap(OutputStream)} correctly wraps a valid
     * provided {@link OutputStream} and writes the test data to it`s
     * destination as it is provided, no compression is done.
     *
     * @param testData parameterized, test data
     * @throws IOException if an I/O exception occurs
     */
    @ParameterizedTest
    @MethodSource("testData")
    void testSuccessfulCompression(final String testData) throws IOException {
        final Path actual = testTempDir.resolve("successfulCompression.txt");
        Files.createFile(actual);

        // assert that the target file exists
        assertThat(actual).exists();

        final byte[] byteArrayTestData = testData.getBytes(StandardCharsets.UTF_8);
        try (final OutputStream out = toTest.wrap(Files.newOutputStream(actual))) {
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
