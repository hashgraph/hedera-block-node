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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/** FileUtils methods provide common functionality for the storage package. */
public final class FileUtils {

    private static final System.Logger LOGGER = System.getLogger(FileUtils.class.getName());

    private FileUtils() {}

    /**
     * Default file permissions defines the file and directory for the storage package.
     *
     * <p>Default permissions are set to: rwxr-xr-x
     */
    @NonNull
    public static final FileAttribute<Set<PosixFilePermission>> defaultPerms =
            PosixFilePermissions.asFileAttribute(
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.GROUP_EXECUTE,
                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OTHERS_EXECUTE));

    public static void createPathIfNotExists(
            @NonNull final Path blockNodePath,
            @NonNull final System.Logger.Level logLevel,
            @NonNull FileAttribute<Set<PosixFilePermission>> perms)
            throws IOException {
        // Initialize the Block directory if it does not exist
        if (Files.notExists(blockNodePath)) {
            Files.createDirectory(blockNodePath, perms);
            LOGGER.log(logLevel, "Created block node root directory: " + blockNodePath);
        } else {
            LOGGER.log(logLevel, "Using existing block node root directory: " + blockNodePath);
        }
    }
}
