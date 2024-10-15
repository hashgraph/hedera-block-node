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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** A utility class that deals with logic related to dealing with files. */
public final class FileUtils {
    private static final Logger LOGGER = System.getLogger(FileUtils.class.getName());

    /**
     * Default file permissions defines the file and directory for the storage package.
     *
     * <p>Default permissions are set to: rwxr-xr-x
     */
    public static final FileAttribute<Set<PosixFilePermission>> DEFAULT_PERMS =
            PosixFilePermissions.asFileAttribute(
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.GROUP_EXECUTE,
                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OTHERS_EXECUTE));

    /**
     * Use this to create a Dir or File if it does not exist with the given permissions and log the
     * result.
     *
     * @param toCreate valid, non-null instance of {@link Path} to be created
     * @param logLevel valid, non-null instance of {@link System.Logger.Level} to use
     * @param perms valid, non-null instance of {@link FileAttribute} permissions to use when
     *     creating the path
     * @param semanticPathName valid, non-blank {@link String} used for logging that represents the
     *     desired path semantically
     * @param createDir {@link Boolean} value if we should create a directory or a file
     * @throws IOException if the path cannot be created
     */
    public static void createPathIfNotExists(
            @NonNull final Path toCreate,
            @NonNull final System.Logger.Level logLevel,
            @NonNull final FileAttribute<Set<PosixFilePermission>> perms,
            @NonNull final String semanticPathName,
            final boolean createDir)
            throws IOException {
        Objects.requireNonNull(toCreate);
        Objects.requireNonNull(logLevel);
        Objects.requireNonNull(perms);
        StringUtils.requireNotBlank(semanticPathName);
        final String type = createDir ? "directory" : "file";
        if (Files.notExists(toCreate)) {
            if (createDir) {
                Files.createDirectories(toCreate, perms);
            } else {
                Files.createFile(toCreate, perms);
            }
            LOGGER.log(logLevel, "Created " + type + " [" + semanticPathName + "] at:" + toCreate);
        } else {
            LOGGER.log(
                    logLevel,
                    "Using existing " + type + " [" + semanticPathName + "] at:" + toCreate);
        }
    }

    /**
     * Read a GZIP file and return the content as a byte array.
     *
     * @param filePath Path to the GZIP file
     * @return byte array of the content of the GZIP file
     * @throws IOException if an I/O error occurs
     */
    public static byte[] readGzFile(@NonNull final Path filePath) throws IOException {
        try (final InputStream fileInputStream =
                        Files.newInputStream(Objects.requireNonNull(filePath));
                final GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream)) {
            return gzipInputStream.readAllBytes();
        }
    }

    // TODO the below code required the .gz and .blk file extension strings that we agreed to remove
    // please remove the method if you think it should not exist here, if it should, then should the
    // string literals .gz and .blk be back in the Strings constants class, or introduced here
    // directly
    //    /**
    //     * Read a file and return the content as a byte array.
    //     *
    //     * @param filePath Path to the file
    //     * @return byte array of the content of the file or null if the file extension is not
    // supported
    //     * @throws IOException if an I/O error occurs
    //     */
    //    public static byte[] readFileBytes(@NonNull final Path filePath) throws IOException {
    //        final String filePathAsString = Objects.requireNonNull(filePath).toString();
    //        if (filePathAsString.endsWith(Strings.GZ_FILE_EXTENSION)) {
    //            return readGzFile(filePath);
    //        } else if (filePathAsString.endsWith(Strings.BLOCK_FILE_EXTENSION)) {
    //            return Files.readAllBytes(filePath);
    //        } else {
    //            return null;
    //        }
    //    }

    private FileUtils() {}
}
