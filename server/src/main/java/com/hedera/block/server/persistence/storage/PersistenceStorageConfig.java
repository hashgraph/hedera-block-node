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
import java.nio.file.Path;
import java.nio.file.Paths;

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
        @ConfigProperty(defaultValue = "") String rootPath, @ConfigProperty(defaultValue = "PRODUCTION") String type) {
    private static final System.Logger LOGGER = System.getLogger(PersistenceStorageConfig.class.getName());

    /**
     * Constructor to set the default root path if not provided, it will be set to the data
     * directory in the current working directory
     */
    public PersistenceStorageConfig {
        // verify rootPath prop
        Path path = Path.of(rootPath);
        if (rootPath.isEmpty()) {
            path = Paths.get(rootPath).toAbsolutePath().resolve("data");
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
            throw new RuntimeException(message, e);
        }

        LOGGER.log(INFO, "Persistence Storage configuration persistence.storage.rootPath: " + path);
        rootPath = path.toString();
        LOGGER.log(INFO, "Persistence configuration persistence.storage.type: " + type);
    }
}
