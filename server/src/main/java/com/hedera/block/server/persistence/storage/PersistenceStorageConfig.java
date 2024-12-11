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

package com.hedera.block.server.persistence.storage;

import static com.hedera.block.server.Constants.BLOCK_NODE_ARCHIVE_ROOT_DIRECTORY_SEMANTIC_NAME;
import static com.hedera.block.server.Constants.BLOCK_NODE_LIVE_ROOT_DIRECTORY_SEMANTIC_NAME;

import com.github.luben.zstd.Zstd;
import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.common.utils.StringUtilities;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Use this configuration across the persistence storage package.
 *
 * @param liveRootPath provides the root path for saving blocks live
 * @param archiveRootPath provides the root path for archived blocks
 * @param type storage type
 * @param compression compression type to use for the storage
 * Non-PRODUCTION values should only be used for troubleshooting and development purposes.
 */
@ConfigData("persistence.storage")
public record PersistenceStorageConfig(
        // @todo(#371) - the default life/archive root path must be absolute starting from /opt
        @ConfigProperty(defaultValue = "") String liveRootPath,
        // @todo(#371) - the default life/archive root path must be absolute starting from /opt
        @ConfigProperty(defaultValue = "") String archiveRootPath,
        @ConfigProperty(defaultValue = "BLOCK_AS_LOCAL_FILE") StorageType type,
        @ConfigProperty(defaultValue = "ZSTD") CompressionType compression,
        // todo for the default compression level, should we use the Zstd#defaultCompressionLevel()
        //  (should be 3 based on some docs) or should we go with 6 as proposed in PR review?
        @ConfigProperty(defaultValue = "6") int compressionLevel) {
    // @todo(#371) - the default life/archive root path must be absolute starting from /opt
    private static final String LIVE_ROOT_PATH =
            Path.of("hashgraph/blocknode/data/live/").toAbsolutePath().toString();
    // @todo(#371) - the default life/archive root path must be absolute starting from /opt
    private static final String ARCHIVE_ROOT_PATH =
            Path.of("hashgraph/blocknode/data/archive/").toAbsolutePath().toString();

    /**
     * Constructor.
     */
    public PersistenceStorageConfig {
        Objects.requireNonNull(type);
        Objects.requireNonNull(compression);
        // todo should we use the Zstd#minCompressionLevel() and Zstd#maxCompressionLevel() or do we?
        //  have a range that is defined by us internally that should be allowed, as proposed in one
        //  PR comment, 0 - 9 with 6 default?
        Preconditions.requireInRange(compressionLevel, Zstd.minCompressionLevel(), Zstd.maxCompressionLevel());
        liveRootPath = resolvePath(liveRootPath, LIVE_ROOT_PATH, BLOCK_NODE_LIVE_ROOT_DIRECTORY_SEMANTIC_NAME);
        archiveRootPath =
                resolvePath(archiveRootPath, ARCHIVE_ROOT_PATH, BLOCK_NODE_ARCHIVE_ROOT_DIRECTORY_SEMANTIC_NAME);
    }

    /**
     * This method attempts to resolve a given configured path. If the input
     * path is blank, a default path is used. The resolved path must be
     * absolute! If the path resolution is successful, at attempt is made to
     * create the directory path. If the directory path cannot be created, an
     * {@link UncheckedIOException} is thrown.
     *
     * @param pathToResolve the path to resolve
     * @param defaultIfBlank the default path if the path to resolve is blank
     * @param semanticPathName the semantic name of the path used for logging
     * @return the resolved path
     * @throws IllegalArgumentException if the resolved path is not absolute
     * @throws UncheckedIOException if the resolved path cannot be created
     */
    @NonNull
    private String resolvePath(
            final String pathToResolve, @NonNull final String defaultIfBlank, @NonNull final String semanticPathName) {
        final Path normalized = getNormalizedPath(pathToResolve, defaultIfBlank);
        createDirectoryPath(normalized, semanticPathName);
        return normalized.toString();
    }

    /**
     * This method normalizes a given path. If the path to normalize is blank,
     * a default path is used. The normalized path must be absolute!
     *
     * @param pathToNormalize the path to normalize
     * @param defaultIfBlank the default path if the path to normalize is blank
     * @throws IllegalArgumentException if the path to normalize is not absolute
     */
    @NonNull
    private Path getNormalizedPath(final String pathToNormalize, @NonNull final String defaultIfBlank) {
        final String actualToNormalize = StringUtilities.isBlank(pathToNormalize) ? defaultIfBlank : pathToNormalize;
        return Path.of(actualToNormalize).normalize().toAbsolutePath();
    }

    /**
     * This method creates a directory path at the given target path. If the
     * directory path cannot be created, an {@link UncheckedIOException} is
     * thrown.
     *
     * @param targetPath the target path to create the directory path
     * @param semanticPathName the semantic name of the path used for logging
     * @throws UncheckedIOException if the directory path cannot be created
     */
    private void createDirectoryPath(@NonNull final Path targetPath, @NonNull final String semanticPathName) {
        try {
            Files.createDirectories(targetPath);
        } catch (final IOException e) {
            final String classname = this.getClass().getName();
            final String message = "Unable to instantiate [%s]! Unable to create the [%s] path that was provided!"
                    .formatted(classname, semanticPathName);
            throw new UncheckedIOException(message, e);
        }
    }

    /**
     * An enum that reflects the type of Block Storage Persistence that is
     * currently used within the given server instance. During runtime one
     * should only query for the storage type that was configured by calling
     * {@link PersistenceStorageConfig#type()} on an instance of the persistence
     * storage config that was only constructed via
     * {@link com.swirlds.config.api.Configuration#getConfigData(Class)}!
     */
    public enum StorageType {
        /**
         * This type of storage stores Blocks as individual files with the Block
         * number as a unique file name and persisted in a trie structure with
         * digit-per-folder
         * (see <a href="https://github.com/hashgraph/hedera-block-node/issues/125">#125</a>).
         * This is also the default setting for the server if it is not
         * explicitly specified via an environment variable or app.properties.
         */
        BLOCK_AS_LOCAL_FILE,
        /**
         * This type of storage stores Blocks as directories with the Block
         * number being the directory number. Block Items are stored as files
         * within a given Block directory. Used primarily for testing purposes.
         */
        BLOCK_AS_LOCAL_DIRECTORY,
        /**
         * This type of storage does nothing.
         */
        NO_OP
    }

    /**
     * An enum that reflects the type of compression that is used to compress
     * the blocks that are stored within the persistence storage.
     */
    public enum CompressionType {
        /**
         * This type of compression is used to compress the blocks using the
         * `Zstandard` algorithm.
         */
        ZSTD,
        /**
         * This type means no compression will be done.
         */
        NONE
    }
}
