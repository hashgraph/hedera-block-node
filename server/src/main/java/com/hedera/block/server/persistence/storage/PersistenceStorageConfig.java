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

import static com.hedera.block.server.Constants.BLOCK_NODE_ROOT_DIRECTORY_SEMANTIC_NAME;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.common.utils.FileUtilities;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Use this configuration across the persistent storage package
 *
 * @param rootPath provides the root path for saving block data, if you want to override it need to
 *     set it as persistence.storage.rootPath
 * @param type use a predefined type string to replace the persistence component implementation.
 *     Non-PRODUCTION values should only be used for troubleshooting and development purposes.
 */
@ConfigData("persistence.storage")
public record PersistenceStorageConfig(
        @ConfigProperty(defaultValue = "") String rootPath,
        @ConfigProperty(defaultValue = "BLOCK_AS_FILE") StorageType type) {
    private static final System.Logger LOGGER = System.getLogger(PersistenceStorageConfig.class.getName());

    /**
     * Constructor to set the default root path if not provided, it will be set to the data
     * directory in the current working directory
     */
    public PersistenceStorageConfig {
        Objects.requireNonNull(type);
        Objects.requireNonNull(rootPath);

        // verify rootPath prop
        Path path = Path.of(rootPath);
        if (rootPath.isBlank()) {
            path = Paths.get("").toAbsolutePath().resolve("data");
        }

        // Check if absolute
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException(rootPath + " Root path must be absolute");
        }

        // Create Directory if it does not exist
        try {
            FileUtilities.createFolderPathIfNotExists(path, ERROR, BLOCK_NODE_ROOT_DIRECTORY_SEMANTIC_NAME);
        } catch (final IOException e) {
            final String message =
                    "Unable to instantiate [%s]! Unable to create the root directory for the block storage [%s]"
                            .formatted(this.getClass().getName(), path);
            throw new UncheckedIOException(message, e);
        }

        rootPath = path.toString();
        LOGGER.log(INFO, "Persistence Storage Configuration: persistence.storage.rootPath=" + path);
        LOGGER.log(INFO, "Persistence Storage Configuration: persistence.storage.type=" + type);
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
        BLOCK_AS_FILE,
        /**
         * This type of storage stores Blocks as directories with the Block
         * number being the directory number. Block Items are stored as files
         * within a given Block directory. Used primarily for testing purposes.
         */
        BLOCK_AS_DIR,
        /**
         * This type of storage does nothing.
         */
        NOOP
    }
}
