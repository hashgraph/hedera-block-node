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
import static java.lang.System.Logger.Level.INFO;

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
 * @param type use a predefined type string to replace the persistence component implementation.
 * Non-PRODUCTION values should only be used for troubleshooting and development purposes.
 */
@ConfigData("persistence.storage")
public record PersistenceStorageConfig(
        // @todo(#371) - the default life/archive root path must be absolute starting from /opt
        @ConfigProperty(defaultValue = "") String liveRootPath,
        // @todo(#371) - the default life/archive root path must be absolute starting from /opt
        @ConfigProperty(defaultValue = "") String archiveRootPath,
        @ConfigProperty(defaultValue = "BLOCK_AS_LOCAL_FILE") StorageType type) {
    private static final System.Logger LOGGER = System.getLogger(PersistenceStorageConfig.class.getName());
    // @todo(#371) - the default life/archive root path must be absolute starting from /opt
    private static final String LIVE_ROOT_PATH =
            Path.of("hashgraph/blocknode/data/live/").toAbsolutePath().toString();
    // @todo(#371) - the default life/archive root path must be absolute starting from /opt
    private static final String ARCHIVE_ROOT_PATH =
            Path.of("hashgraph/blocknode/data/archive/").toAbsolutePath().toString();

    /**
     * Constructor to set the default root path if not provided, it will be set to the data
     * directory in the current working directory
     */
    public PersistenceStorageConfig {
        Objects.requireNonNull(type);
        liveRootPath = resolvePath(liveRootPath, LIVE_ROOT_PATH, BLOCK_NODE_LIVE_ROOT_DIRECTORY_SEMANTIC_NAME);
        archiveRootPath =
                resolvePath(archiveRootPath, ARCHIVE_ROOT_PATH, BLOCK_NODE_ARCHIVE_ROOT_DIRECTORY_SEMANTIC_NAME);
        LOGGER.log(INFO, "Persistence Storage Configuration: persistence.storage.type=" + type);
        LOGGER.log(INFO, "Persistence Storage Configuration: persistence.storage.liveRootPath=" + liveRootPath);
        LOGGER.log(INFO, "Persistence Storage Configuration: persistence.storage.archiveRootPath=" + archiveRootPath);
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
        final Path normalized = getNormalizedPath(pathToResolve, defaultIfBlank, semanticPathName);
        createDirectoryPath(normalized, semanticPathName);
        return normalized.toString();
    }

    /**
     * This method normalizes a given path. If the path to normalize is blank,
     * a default path is used. The normalized path must be absolute! If the
     * normalized path is not absolute, an {@link IllegalArgumentException} is
     * thrown.
     *
     * @param pathToNormalize the path to normalize
     * @param defaultIfBlank the default path if the path to normalize is blank
     * @param semanticPathName the semantic name of the path used for logging
     * @return the normalized path
     * @throws IllegalArgumentException if the path to normalize is not absolute
     */
    @NonNull
    private Path getNormalizedPath(
            final String pathToNormalize,
            @NonNull final String defaultIfBlank,
            @NonNull final String semanticPathName) {
        final Path result;
        if (StringUtilities.isBlank(pathToNormalize)) {
            result = Path.of(defaultIfBlank).toAbsolutePath();
        } else {
            result = Path.of(pathToNormalize);
        }

        if (!result.isAbsolute()) {
            throw new IllegalArgumentException("Path provided for [%s] must be absolute!".formatted(semanticPathName));
        } else {
            return result;
        }
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
}
