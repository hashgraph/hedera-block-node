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

import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_COMPRESSION_LEVEL;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.luben.zstd.ZstdOutputStream;
import com.hedera.block.common.utils.FileUtilities;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.util.TestConfigUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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

    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;
    private ZstdCompression toTest;

    @BeforeEach
    void setUp() throws IOException {
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(PERSISTENCE_STORAGE_COMPRESSION_LEVEL, String.valueOf(6)));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        toTest = ZstdCompression.of(testConfig);
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
     * {@link NoOpCompression#wrap(OutputStream)} correctly wraps a valid
     * provided {@link OutputStream} and utilizes the Zstandard compression
     * algorithm when writing the data to it`s destination.
     *
     * @param testData parameterized, test data
     * @throws IOException if an I/O exception occurs
     */
    @ParameterizedTest
    @MethodSource("testData")
    void testSuccessfulCompression(final String testData) throws IOException {
        final Path actual = testTempDir.resolve(FileUtilities.appendExtension(
                Path.of("successfulCompression.txt"), toTest.getCompressionFileExtension()));
        Files.createFile(actual);

        // assert that the raw target file exists
        assertThat(actual).exists();

        final byte[] byteArrayTestData = testData.getBytes(StandardCharsets.UTF_8);
        try (final OutputStream out = toTest.wrap(Files.newOutputStream(actual))) {
            out.write(byteArrayTestData);
        }
        assertThat(actual)
                .exists()
                .isReadable()
                .isRegularFile()
                .isNotEmptyFile()
                .hasSameBinaryContentAs(actualZstdCompression(byteArrayTestData));
    }

    private Path actualZstdCompression(final byte[] byteArrayTestData) throws IOException {
        final Path tempFile = testTempDir.resolve(
                FileUtilities.appendExtension(Path.of("tempComparisonFile.txt"), toTest.getCompressionFileExtension()));
        try (final ZstdOutputStream out =
                new ZstdOutputStream(Files.newOutputStream(tempFile), testConfig.compressionLevel())) {
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
