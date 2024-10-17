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
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FileUtilitiesTest {
    // VALID PATHS & CONTENT
    private static final Path GZ_VALID1_PATH = Path.of("src/test/resources/valid1.txt.gz");
    private static final String GZ_VALID1_CONTENT = "valid1";

    private static final Path GZ_VALID2_PATH = Path.of("src/test/resources/valid2.txt.gz");
    private static final String GZ_VALID2_CONTENT = "valid2";

    private static final Path BLK_VALID1_PATH = Path.of("src/test/resources/valid1.blk");
    private static final String BLK_VALID1_CONTENT = "valid1blk";

    private static final Path BLK_VALID2_PATH = Path.of("src/test/resources/valid2.blk");
    private static final String BLK_VALID2_CONTENT = "valid2blk";

    // INVALID PATHS
    private static final Path GZ_INVALID1_PATH = Path.of("src/test/resources/invalid1.gz");
    private static final Path FILE_INVALID2_PATH = Path.of("src/test/resources/nonexistent.gz");

    @Test
    void test_createPathIfNotExists_CreatesDirIfDoesNotExist(@TempDir final Path tempDir)
            throws IOException {
        final String newDir = "newDir";
        final Path toCreate = tempDir.resolve(newDir);

        assertThat(tempDir).isEmptyDirectory();
        assertThat(toCreate).doesNotExist();

        FileUtilities.createPathIfNotExists(toCreate, Level.ERROR, "test dir 1", true);

        assertThat(tempDir.toFile().listFiles()).hasSize(1);
        assertThat(toCreate).exists().isDirectory();
    }

    @Test
    void test_createPathIfNotExists_CreatesFileIfDoesNotExist(@TempDir final Path tempDir)
            throws IOException {
        final String newFile = "newFile";
        final Path toCreate = tempDir.resolve(newFile);

        assertThat(tempDir).isEmptyDirectory();
        assertThat(toCreate).doesNotExist();

        FileUtilities.createPathIfNotExists(toCreate, Level.ERROR, "test file 1", false);

        assertThat(tempDir.toFile().listFiles()).hasSize(1);
        assertThat(toCreate).exists().isEmptyFile();
    }

    @Test
    void test_createPathIfNotExists_DoesNotCreateDirIfExists(@TempDir final Path tempDir)
            throws IOException {
        final String newDir = "newDir";
        final Path toCreate = tempDir.resolve(newDir);

        assertThat(tempDir).isEmptyDirectory();
        assertThat(toCreate).doesNotExist();

        Files.createDirectory(toCreate);

        assertThat(tempDir.toFile().listFiles()).hasSize(1);
        assertThat(toCreate).exists().isDirectory();

        FileUtilities.createPathIfNotExists(toCreate, Level.ERROR, "test dir 1", true);

        assertThat(tempDir.toFile().listFiles()).hasSize(1);
        assertThat(toCreate).exists().isDirectory();
    }

    @Test
    void test_createPathIfNotExists_DoesNotCreateFileIfExists(@TempDir final Path tempDir)
            throws IOException {
        final String newFile = "newFile";
        final Path toCreate = tempDir.resolve(newFile);

        assertThat(tempDir).isEmptyDirectory();
        assertThat(toCreate).doesNotExist();

        Files.createFile(toCreate);

        assertThat(tempDir.toFile().listFiles()).hasSize(1);
        assertThat(toCreate).exists().isEmptyFile();

        FileUtilities.createPathIfNotExists(toCreate, Level.ERROR, "test file 1", false);

        assertThat(tempDir.toFile().listFiles()).hasSize(1);
        assertThat(toCreate).exists().isEmptyFile();
    }

    @ParameterizedTest
    @MethodSource("validGzipFiles")
    void test_readGzipFileUnsafe_ReturnsByteArrayWithValidContentForValidGzipFile(
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
    @MethodSource("invalidFiles")
    void test_readGzipFileUnsafe_ThrowsIOExceptionForInvalidGzipFile(final Path filePath) {
        assertThatIOException().isThrownBy(() -> FileUtilities.readGzipFileUnsafe(filePath));
    }

    @ParameterizedTest
    @MethodSource({"validGzipFiles", "validBlkFiles"})
    void test_readFileBytesUnsafe_ReturnsByteArrayWithValidContentForValidFile(
            final Path filePath, final String expectedContent) throws IOException {
        final byte[] actualContent = FileUtilities.readFileBytesUnsafe(filePath);
        assertThat(actualContent)
                .isNotNull()
                .isNotEmpty()
                .asString()
                .isNotNull()
                .isNotBlank()
                .isEqualTo(expectedContent);
    }

    @ParameterizedTest
    @MethodSource("invalidFiles")
    void test_readFileBytesUnsafe_ThrowsIOExceptionForInvalidGzipFile(final Path filePath) {
        assertThatIOException().isThrownBy(() -> FileUtilities.readFileBytesUnsafe(filePath));
    }

    private static Stream<Arguments> validGzipFiles() {
        return Stream.of(
                Arguments.of(GZ_VALID1_PATH, GZ_VALID1_CONTENT),
                Arguments.of(GZ_VALID2_PATH, GZ_VALID2_CONTENT));
    }

    private static Stream<Arguments> validBlkFiles() {
        return Stream.of(
                Arguments.of(BLK_VALID1_PATH, BLK_VALID1_CONTENT),
                Arguments.of(BLK_VALID2_PATH, BLK_VALID2_CONTENT));
    }

    private static Stream<Arguments> invalidFiles() {
        return Stream.of(Arguments.of(GZ_INVALID1_PATH), Arguments.of(FILE_INVALID2_PATH));
    }
}
