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

import java.io.File;
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
    private static final String FILE_WITH_UNRECOGNIZED_EXTENSION = "src/test/resources/nonexistent.unrecognized";

    @Test
    void test_createPathIfNotExists_CreatesDirIfDoesNotExist(@TempDir final Path tempDir) throws IOException {
        final String newDir = "newDir";
        final Path toCreate = tempDir.resolve(newDir);

        // ensure the temp directory is empty in the beginning
        assertThat(tempDir).isEmptyDirectory();
        assertThat(toCreate).doesNotExist();

        // run actual
        FileUtilities.createPathIfNotExists(toCreate, Level.ERROR, "test dir 1", true);

        // assert
        assertThat(toCreate).exists().isDirectory();
        assertThat(tempDir.toFile().listFiles()).hasSize(1).contains(toCreate.toFile());
    }

    @Test
    void test_createPathIfNotExists_DoesNotCreateDirIfExists(@TempDir final Path tempDir) throws IOException {
        final String newDir = "newDir";
        final Path toCreate = tempDir.resolve(newDir);

        // ensure the temp directory is empty in the beginning
        assertThat(tempDir).isEmptyDirectory();
        assertThat(toCreate).doesNotExist();

        // create 'newDir'
        Files.createDirectory(toCreate);

        // ensure the temp directory contains only 'newDir' before running actual
        final File tempDirAsFile = tempDir.toFile();
        final File toCreateAsFile = toCreate.toFile();
        assertThat(tempDirAsFile.listFiles()).hasSize(1).contains(toCreateAsFile);
        assertThat(toCreate).exists().isDirectory();

        // run actual
        FileUtilities.createPathIfNotExists(toCreate, Level.ERROR, "test dir 1", true);

        // assert
        assertThat(toCreate).exists().isDirectory();
        assertThat(tempDirAsFile.listFiles()).hasSize(1).contains(toCreateAsFile);
    }

    @Test
    void test_createPathIfNotExists_CreatesFileIfDoesNotExist(@TempDir final Path tempDir) throws IOException {
        final String newFile = "newFile";
        final Path toCreate = tempDir.resolve(newFile);

        // ensure the temp directory is empty in the beginning
        assertThat(tempDir).isEmptyDirectory();
        assertThat(toCreate).doesNotExist();

        // run actual
        FileUtilities.createPathIfNotExists(toCreate, Level.ERROR, "test file 1", false);

        // assert
        assertThat(toCreate).exists().isEmptyFile();
        assertThat(tempDir.toFile().listFiles()).hasSize(1).contains(toCreate.toFile());
    }

    @Test
    void test_createPathIfNotExists_DoesNotCreateFileIfExists(@TempDir final Path tempDir) throws IOException {
        final String newFile = "newFile";
        final Path toCreate = tempDir.resolve(newFile);

        // ensure the temp directory is empty in the beginning
        assertThat(tempDir).isEmptyDirectory();
        assertThat(toCreate).doesNotExist();

        // create 'newFile'
        Files.createFile(toCreate);

        // ensure the temp directory contains only 'newFile' before running actual
        final File tempDirAsFile = tempDir.toFile();
        final File toCreateAsFile = toCreate.toFile();
        assertThat(tempDirAsFile.listFiles()).hasSize(1).contains(toCreateAsFile);
        assertThat(toCreate).exists().isEmptyFile();

        // run actual
        FileUtilities.createPathIfNotExists(toCreate, Level.ERROR, "test file 1", false);

        // assert
        assertThat(toCreate).exists().isEmptyFile();
        assertThat(tempDirAsFile.listFiles()).hasSize(1).contains(toCreateAsFile);
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

    @Test
    void test_readFileBytesUnsafe_ReturnsNullByteArrayWhenExtensionIsNotRecognized() throws IOException {
        final byte[] actualContent = FileUtilities.readFileBytesUnsafe(Path.of(FILE_WITH_UNRECOGNIZED_EXTENSION));
        assertThat(actualContent).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidFiles")
    void test_readFileBytesUnsafe_ThrowsIOExceptionForInvalidGzipFile(final Path filePath) {
        assertThatIOException().isThrownBy(() -> FileUtilities.readFileBytesUnsafe(filePath));
    }

    @ParameterizedTest
    @MethodSource({"validGzipFiles", "validBlkFiles"})
    void test_readFileBytesUnsafe_ReturnsByteArrayWithValidContentForValidFileWithGivenExtension(
            final Path filePath, final String expectedContent) throws IOException {
        final byte[] actualContent = FileUtilities.readFileBytesUnsafe(filePath, ".blk", ".gz");
        assertThat(actualContent)
                .isNotNull()
                .isNotEmpty()
                .asString()
                .isNotNull()
                .isNotBlank()
                .isEqualTo(expectedContent);
    }

    @Test
    void test_readFileBytesUnsafe_ReturnsNullByteArrayWhenExtensionIsNotRecognizedWithGivenExtension()
            throws IOException {
        final byte[] actualContent =
                FileUtilities.readFileBytesUnsafe(Path.of(FILE_WITH_UNRECOGNIZED_EXTENSION), ".blk", ".gz");
        assertThat(actualContent).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidFiles")
    void test_readFileBytesUnsafe_ThrowsIOExceptionForInvalidGzipFileWithGivenExtension(final Path filePath) {
        assertThatIOException().isThrownBy(() -> FileUtilities.readFileBytesUnsafe(filePath, ".blk", ".gz"));
    }

    private static Stream<Arguments> validGzipFiles() {
        return Stream.of(
                Arguments.of("src/test/resources/valid1.txt.gz", "valid1"),
                Arguments.of("src/test/resources/valid2.txt.gz", "valid2"));
    }

    private static Stream<Arguments> validBlkFiles() {
        return Stream.of(
                Arguments.of("src/test/resources/valid1.blk", "valid1blk"),
                Arguments.of("src/test/resources/valid2.blk", "valid2blk"));
    }

    private static Stream<Arguments> invalidFiles() {
        return Stream.of(
                Arguments.of("src/test/resources/invalid1.gz"), Arguments.of("src/test/resources/nonexistent.gz"));
    }
}
