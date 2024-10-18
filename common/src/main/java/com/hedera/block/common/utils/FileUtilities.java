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
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * A utility class that deals with logic related to dealing with files.
 */
public final class FileUtilities {
    private static final Logger LOGGER = System.getLogger(FileUtilities.class.getName());

    /**
     * The default file permissions for new files.
     * <p>
     * Default permissions are set to: rw-r--r--
     */
    private static final FileAttribute<Set<PosixFilePermission>> DEFAULT_FILE_PERMISSIONS =
            PosixFilePermissions.asFileAttribute(
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.OTHERS_READ));

    /**
     * Default folder permissions for new folders.
     * <p>
     * Default permissions are set to: rwxr-xr-x
     */
    private static final FileAttribute<Set<PosixFilePermission>> DEFAULT_FOLDER_PERMISSIONS =
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
     * Log message template used when a path is not created because a file or folder already exists
     * at the requested path.
     */
    private static final String PRE_EXISTING_FOLDER_MESSAGE =
            "Requested %s [%s] not created because %s already exists at %s";

    /**
     * Create a new path (folder or file) if it does not exist. Any folders or files created will
     * use default permissions.
     *
     * @param toCreate         valid, non-null instance of {@link Path} to be created
     * @param logLevel         valid, non-null instance of {@link System.Logger.Level} to use
     * @param semanticPathName valid, non-blank {@link String} used for logging that represents the
     *                         desired path semantically
     * @param createDir        {@link Boolean} value if we should create a directory or a file
     *
     * @throws IOException if the path cannot be created
     */
    public static void createPathIfNotExists(
            @NonNull final Path toCreate,
            @NonNull final System.Logger.Level logLevel,
            @NonNull final String semanticPathName,
            final boolean createDir)
            throws IOException {
        createPathIfNotExists(
                toCreate,
                logLevel,
                DEFAULT_FILE_PERMISSIONS,
                DEFAULT_FOLDER_PERMISSIONS,
                semanticPathName,
                createDir);
    }

    /**
     * Create a new path (folder or file) if it does not exist.
     *
     * @param toCreate          The path to be created.
     * @param logLevel          The logging level to use when logging this event.
     * @param filePermissions   Permissions to use when creating a new file.
     * @param folderPermissions Permissions to use when creating a new folder.
     * @param semanticPathName  A name to represent the path in a logging statement.
     * @param createDir         A flag indicating we should create a directory (true) or a file
     *                          (false)
     *
     * @throws IOException if the path cannot be created due to a filesystem error.
     */
    public static void createPathIfNotExists(
            @NonNull final Path toCreate,
            @NonNull final System.Logger.Level logLevel,
            @NonNull final FileAttribute<Set<PosixFilePermission>> filePermissions,
            @NonNull final FileAttribute<Set<PosixFilePermission>> folderPermissions,
            @NonNull final String semanticPathName,
            final boolean createDir)
            throws IOException {
        Objects.requireNonNull(toCreate);
        Objects.requireNonNull(logLevel);
        Objects.requireNonNull(filePermissions);
        Objects.requireNonNull(folderPermissions);
        StringUtilities.requireNotBlank(semanticPathName);
        final String requestedType = createDir ? "directory" : "file";
        if (Files.notExists(toCreate)) {
            if (createDir) {
                Files.createDirectories(toCreate, folderPermissions);
            } else {
                Files.createFile(toCreate, filePermissions);
            }
            final String logMessage =
                    "Created %s [%s] at %s".formatted(requestedType, semanticPathName, toCreate);
            LOGGER.log(logLevel, logMessage);
        } else {
            final String actualType = Files.isDirectory(toCreate) ? "directory" : "file";
            final String logMessage =
                    PRE_EXISTING_FOLDER_MESSAGE.formatted(
                            requestedType, semanticPathName, actualType, toCreate);
            LOGGER.log(logLevel, logMessage);
        }
    }

    /**
     * Read a GZIP file and return the content as a byte array.
     * <p>
     * This method is _unsafe_ because it reads the entire file content into a single byte array,
     * which can cause memory issues, and may fail if the file contains a large amount of data.
     *
     * @param filePath Path to the GZIP file.
     *
     * @return byte array containing the _uncompressed_ content of the GZIP file.
     *
     * @throws IOException      if unable to read the file.
     * @throws OutOfMemoryError if a byte array large enough to contain the file contents cannot be
     *                          allocated (either because it exceeds MAX_INT bytes or exceeds
     *                          available heap memory).
     */
    public static byte[] readGzipFileUnsafe(@NonNull final Path filePath) throws IOException {
        Objects.requireNonNull(filePath);
        try (final GZIPInputStream gzipInputStream =
                new GZIPInputStream(Files.newInputStream(filePath))) {
            return gzipInputStream.readAllBytes();
        }
    }

    /**
     * Read a file and return the content as a byte array.
     * <p>
     * This method uses default extensions for gzip and block files.
     * <p>
     * This method is _unsafe_ because it reads the entire file content into a single byte array,
     * which can cause memory issues, and may fail if the file contains a large amount of data.
     *
     * @param file to read bytes from
     *
     * @return byte array of the content of the file or null if the file extension is not supported
     *
     * @throws IOException      if unable to read the file.
     * @throws OutOfMemoryError if a byte array large enough to contain the file contents cannot be
     *                          allocated (either because it exceeds MAX_INT bytes or exceeds
     *                          available heap memory).
     */
    public static byte[] readFileBytesUnsafe(@NonNull final File file) throws IOException {
        return readFileBytesUnsafe(file.toPath());
    }

    /**
     * Read a file and return the content as a byte array.
     * <p>
     * This method uses default extensions for gzip and block files.
     * <p>
     * This method is _unsafe_ because it reads the entire file content into a single byte array,
     * which can cause memory issues, and may fail if the file contains a large amount of data.
     *
     * @param filePath Path to the file
     *
     * @return byte array of the content of the file or null if the file extension is not supported
     *
     * @throws IOException      if unable to read the file.
     * @throws OutOfMemoryError if a byte array large enough to contain the file contents cannot be
     *                          allocated (either because it exceeds MAX_INT bytes or exceeds
     *                          available heap memory).
     */
    public static byte[] readFileBytesUnsafe(@NonNull final Path filePath) throws IOException {
        return readFileBytesUnsafe(filePath, ".blk", ".gz");
    }

    /**
     * Read a file and return the content as a byte array.
     * <p>
     * This method is _unsafe_ because it reads the entire file content into a single byte array,
     * which can cause memory issues, and may fail if the file contains a large amount of data.
     *
     * @param filePath           Path to the file to read.
     * @param blockFileExtension A file extension for block files.
     * @param gzipFileExtension  A file extension for gzip files.
     *
     * @return A byte array with the full contents of the file, or null if the file extension
     * requested does not match at least one of the extensions provided (GZip or Block).
     *
     * @throws IOException      if unable to read the file.
     * @throws OutOfMemoryError if a byte array large enough to contain the file contents cannot be
     *                          allocated (either because it exceeds MAX_INT bytes or exceeds
     *                          available heap memory).
     */
    public static byte[] readFileBytesUnsafe(
            @NonNull final Path filePath,
            @NonNull final String blockFileExtension,
            @NonNull final String gzipFileExtension)
            throws IOException {
        final String filePathAsString = Objects.requireNonNull(filePath).toString();
        Objects.requireNonNull(blockFileExtension);
        Objects.requireNonNull(gzipFileExtension);
        if (filePathAsString.endsWith(gzipFileExtension)) {
            return readGzipFileUnsafe(filePath);
        } else if (filePathAsString.endsWith(blockFileExtension)) {
            return Files.readAllBytes(filePath);
        } else {
            return null;
        }
    }

    private FileUtilities() {}
}
