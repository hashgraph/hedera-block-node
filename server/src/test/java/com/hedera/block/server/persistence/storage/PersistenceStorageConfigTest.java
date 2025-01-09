// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.from;

import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.StorageType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class that tests the functionality of the
 * {@link PersistenceStorageConfig}
 */
class PersistenceStorageConfigTest {
    private static final Path HASHGRAPH_ROOT_ABSOLUTE_PATH =
            Path.of("hashgraph/").toAbsolutePath();
    private static final Path PERSISTENCE_STORAGE_ROOT_ABSOLUTE_PATH =
            HASHGRAPH_ROOT_ABSOLUTE_PATH.resolve("blocknode/data/");
    // Default compression level (as set in the config annotation)
    private static final int DEFAULT_COMPRESSION_LEVEL = 3;
    // NoOp compression level boundaries
    private static final int LOWER_BOUNDARY_FOR_NO_OP_COMPRESSION = Integer.MIN_VALUE;
    private static final int DEFAULT_VALUE_FOR_NO_OP_COMPRESSION = DEFAULT_COMPRESSION_LEVEL;
    private static final int UPPER_BOUNDARY_FOR_NO_OP_COMPRESSION = Integer.MAX_VALUE;
    // Zstd compression level boundaries
    private static final int LOWER_BOUNDARY_FOR_ZSTD_COMPRESSION = 0;
    private static final int DEFAULT_VALUE_FOR_ZSTD_COMPRESSION = DEFAULT_COMPRESSION_LEVEL;
    private static final int UPPER_BOUNDARY_FOR_ZSTD_COMPRESSION = 20;

    @AfterEach
    void tearDown() {
        if (!Files.exists(HASHGRAPH_ROOT_ABSOLUTE_PATH)) {
            return;
        }
        try (final Stream<Path> walk = Files.walk(HASHGRAPH_ROOT_ABSOLUTE_PATH)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This test aims to verify that the {@link PersistenceStorageConfig} class
     * correctly returns the storage type that was set in the constructor.
     *
     * @param storageType parameterized, the storage type to test
     */
    @ParameterizedTest
    @MethodSource("storageTypes")
    void testPersistenceStorageConfigStorageTypes(final StorageType storageType) {
        final PersistenceStorageConfig actual =
                new PersistenceStorageConfig("", "", storageType, CompressionType.NONE, DEFAULT_COMPRESSION_LEVEL);
        assertThat(actual).returns(storageType, from(PersistenceStorageConfig::type));
    }

    /**
     * This test aims to verify that the {@link PersistenceStorageConfig} class
     * correctly sets the live and archive root paths.
     *
     * @param liveRootPathToTest parameterized, the live root path to test
     * @param expectedLiveRootPathToTest parameterized, the expected live root
     * @param archiveRootPathToTest parameterized, the archive root path to test
     * @param expectedArchiveRootPathToTest parameterized, the expected archive
     * root
     */
    @ParameterizedTest
    @MethodSource({"validAbsoluteDefaultRootPaths", "validAbsoluteNonDefaultRootPaths"})
    void testPersistenceStorageConfigHappyPaths(
            final String liveRootPathToTest,
            final String expectedLiveRootPathToTest,
            final String archiveRootPathToTest,
            final String expectedArchiveRootPathToTest) {
        final PersistenceStorageConfig actual = new PersistenceStorageConfig(
                liveRootPathToTest,
                archiveRootPathToTest,
                StorageType.BLOCK_AS_LOCAL_FILE,
                CompressionType.NONE,
                DEFAULT_COMPRESSION_LEVEL);
        assertThat(actual)
                .returns(expectedLiveRootPathToTest, from(PersistenceStorageConfig::liveRootPath))
                .returns(expectedArchiveRootPathToTest, from(PersistenceStorageConfig::archiveRootPath));
    }

    /**
     * This test aims to verify that the {@link PersistenceStorageConfig} class
     * correctly throws an {@link UncheckedIOException} when either the live or
     * archive root paths are invalid.
     *
     * @param invalidLiveRootPathToTest parameterized, the invalid live root
     * path to test
     * @param invalidArchiveRootPathToTest parameterized, the invalid archive
     * root path to test
     */
    @ParameterizedTest
    @MethodSource({"invalidRootPaths"})
    void testPersistenceStorageConfigInvalidRootPaths(
            final String invalidLiveRootPathToTest, final String invalidArchiveRootPathToTest) {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> new PersistenceStorageConfig(
                        invalidLiveRootPathToTest,
                        invalidArchiveRootPathToTest,
                        StorageType.BLOCK_AS_LOCAL_FILE,
                        CompressionType.NONE,
                        DEFAULT_COMPRESSION_LEVEL));
    }

    /**
     * This test aims to verify that the {@link PersistenceStorageConfig} class
     * correctly returns the compression level that was set in the constructor.
     *
     * @param compressionLevel parameterized, the compression level to test
     */
    @ParameterizedTest
    @MethodSource("validCompressionLevels")
    void testPersistenceStorageConfigValidCompressionLevel(
            final CompressionType compressionType, final int compressionLevel) {
        final PersistenceStorageConfig actual = new PersistenceStorageConfig(
                "", "", StorageType.BLOCK_AS_LOCAL_FILE, compressionType, compressionLevel);
        assertThat(actual).returns(compressionLevel, from(PersistenceStorageConfig::compressionLevel));
    }

    /**
     * This test aims to verify that the {@link PersistenceStorageConfig} class
     * correctly throws an {@link IllegalArgumentException} when the compression
     * level is invalid.
     *
     * @param compressionLevel parameterized, the compression level to test
     */
    @ParameterizedTest
    @MethodSource("invalidCompressionLevels")
    void testPersistenceStorageConfigInvalidCompressionLevel(
            final CompressionType compressionType, final int compressionLevel) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PersistenceStorageConfig(
                        "", "", StorageType.BLOCK_AS_LOCAL_FILE, compressionType, compressionLevel));
    }

    /**
     * This test aims to verify that the {@link PersistenceStorageConfig} class
     * correctly returns the compression type that was set in the constructor.
     *
     * @param compressionType parameterized, the compression type to test
     */
    @ParameterizedTest
    @MethodSource("compressionTypes")
    void testPersistenceStorageConfigCompressionTypes(final CompressionType compressionType) {
        final PersistenceStorageConfig actual =
                new PersistenceStorageConfig("", "", StorageType.NO_OP, compressionType, DEFAULT_COMPRESSION_LEVEL);
        assertThat(actual).returns(compressionType, from(PersistenceStorageConfig::compression));
    }

    /**
     * All storage types dynamically provided.
     */
    private static Stream<Arguments> storageTypes() {
        return Arrays.stream(StorageType.values()).map(Arguments::of);
    }

    /**
     * All compression types dynamically provided.
     */
    private static Stream<Arguments> compressionTypes() {
        return Arrays.stream(CompressionType.values()).map(Arguments::of);
    }

    /**
     * The default absolute paths. We expect these to allow the persistence
     * config to be instantiated. Providing a blank string is accepted, it will
     * create the config instance with it's internal defaults.
     */
    @SuppressWarnings("all")
    private static Stream<Arguments> validAbsoluteDefaultRootPaths() {
        final Path defaultLiveRootAbsolutePath =
                PERSISTENCE_STORAGE_ROOT_ABSOLUTE_PATH.resolve("live/").toAbsolutePath();
        final Path defaultArchiveRootAbsolutePath =
                PERSISTENCE_STORAGE_ROOT_ABSOLUTE_PATH.resolve("archive/").toAbsolutePath();

        // test against the default liveRootPath and archiveRootPath
        final String liveToTest1 = defaultLiveRootAbsolutePath.toString();
        final String liveExpected1 = defaultLiveRootAbsolutePath.toString();
        final String archiveToTest1 = defaultArchiveRootAbsolutePath.toString();
        final String archiveExpected1 = defaultArchiveRootAbsolutePath.toString();

        // blank liveRootPath results in the default liveRootPath to be used
        final String liveToTest2 = "";
        final String liveExpected2 = defaultLiveRootAbsolutePath.toString();
        final String archiveToTest2 = defaultArchiveRootAbsolutePath.toString();
        final String archiveExpected2 = defaultArchiveRootAbsolutePath.toString();

        // blank archiveRootPath results in the default archiveRootPath to be used
        final String liveToTest3 = defaultLiveRootAbsolutePath.toString();
        final String liveExpected3 = defaultLiveRootAbsolutePath.toString();
        final String archiveToTest3 = "";
        final String archiveExpected3 = defaultArchiveRootAbsolutePath.toString();

        // null liveRootPath results in the default liveRootPath to be used
        final String liveToTest4 = null;
        final String liveExpected4 = defaultLiveRootAbsolutePath.toString();
        final String archiveToTest4 = defaultArchiveRootAbsolutePath.toString();
        final String archiveExpected4 = defaultArchiveRootAbsolutePath.toString();

        // null archiveRootPath results in the default archiveRootPath to be used
        final String liveToTest5 = defaultLiveRootAbsolutePath.toString();
        final String liveExpected5 = defaultLiveRootAbsolutePath.toString();
        final String archiveToTest5 = null;
        final String archiveExpected5 = defaultArchiveRootAbsolutePath.toString();

        // blank liveRootPath and archiveRootPath results in the default liveRootPath and archiveRootPath to be used
        final String liveToTest6 = "";
        final String liveExpected6 = defaultLiveRootAbsolutePath.toString();
        final String archiveToTest6 = "";
        final String archiveExpected6 = defaultArchiveRootAbsolutePath.toString();

        // null liveRootPath and archiveRootPath results in the default liveRootPath and archiveRootPath to be used
        final String liveToTest7 = null;
        final String liveExpected7 = defaultLiveRootAbsolutePath.toString();
        final String archiveToTest7 = null;
        final String archiveExpected7 = defaultArchiveRootAbsolutePath.toString();

        return Stream.of(
                Arguments.of(liveToTest1, liveExpected1, archiveToTest1, archiveExpected1),
                Arguments.of(liveToTest2, liveExpected2, archiveToTest2, archiveExpected2),
                Arguments.of(liveToTest3, liveExpected3, archiveToTest3, archiveExpected3),
                Arguments.of(liveToTest4, liveExpected4, archiveToTest4, archiveExpected4),
                Arguments.of(liveToTest5, liveExpected5, archiveToTest5, archiveExpected5),
                Arguments.of(liveToTest6, liveExpected6, archiveToTest6, archiveExpected6),
                Arguments.of(liveToTest7, liveExpected7, archiveToTest7, archiveExpected7));
    }

    /**
     * Somve valid absolute paths that are not the default paths. We expect
     * these to allow the persistence config to be instantiated.
     */
    @SuppressWarnings("all")
    private static Stream<Arguments> validAbsoluteNonDefaultRootPaths() {
        final String liveToTest1 = PERSISTENCE_STORAGE_ROOT_ABSOLUTE_PATH
                .resolve("nondefault/live/")
                .toString();
        final String archiveToTest1 = PERSISTENCE_STORAGE_ROOT_ABSOLUTE_PATH
                .resolve("nondefault/archive/")
                .toString();

        final String liveToTest2 = PERSISTENCE_STORAGE_ROOT_ABSOLUTE_PATH
                .resolve("another/nondefault/live/")
                .toString();
        final String archiveToTest2 = PERSISTENCE_STORAGE_ROOT_ABSOLUTE_PATH
                .resolve("another/nondefault/archive/")
                .toString();

        return Stream.of(
                Arguments.of(liveToTest1, liveToTest1, archiveToTest1, archiveToTest1),
                Arguments.of(liveToTest2, liveToTest2, archiveToTest2, archiveToTest2));
    }

    /**
     * Supplying blank is valid, both must be valid paths in order to be able
     * to create the config instance. If either liveRootPath or archiveRootPath
     * is invalid, we expect to fail. There cannot be invalid paths supplied.
     */
    private static Stream<Arguments> invalidRootPaths() {
        final String invalidPath = "/invalid_path/:invalid_directory";
        return Stream.of(
                Arguments.of("", invalidPath), Arguments.of(invalidPath, ""), Arguments.of(invalidPath, invalidPath));
    }

    private static Stream<Arguments> validCompressionLevels() {
        return Stream.of(
                Arguments.of(
                        CompressionType.NONE,
                        LOWER_BOUNDARY_FOR_NO_OP_COMPRESSION), // lower boundary for NO_OP compression
                Arguments.of(
                        CompressionType.NONE,
                        DEFAULT_VALUE_FOR_NO_OP_COMPRESSION), // default value for NO_OP compression
                Arguments.of(
                        CompressionType.NONE,
                        UPPER_BOUNDARY_FOR_NO_OP_COMPRESSION), // upper boundary for NO_OP compression
                Arguments.of(
                        CompressionType.ZSTD,
                        LOWER_BOUNDARY_FOR_ZSTD_COMPRESSION), // lower boundary for ZSTD compression
                Arguments.of(
                        CompressionType.ZSTD, DEFAULT_VALUE_FOR_ZSTD_COMPRESSION), // default value for ZSTD compression
                Arguments.of(
                        CompressionType.ZSTD,
                        UPPER_BOUNDARY_FOR_ZSTD_COMPRESSION) // upper boundary for ZSTD compression
                );
    }

    private static Stream<Arguments> invalidCompressionLevels() {
        return Stream.of(
                Arguments.of(CompressionType.ZSTD, LOWER_BOUNDARY_FOR_ZSTD_COMPRESSION - 1),
                Arguments.of(CompressionType.ZSTD, UPPER_BOUNDARY_FOR_ZSTD_COMPRESSION + 1));
    }
}
