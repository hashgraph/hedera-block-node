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

package com.hedera.block.server.persistence.storage.read;

import static com.hedera.block.protos.BlockStreamService.Block.Builder;
import static com.hedera.block.server.Constants.BLOCK_FILE_EXTENSION;

import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.Block;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

/**
 * The BlockAsDirReader class reads a block from the file system. The block is stored as a directory
 * containing block items. The block items are stored as files within the block directory.
 */
class BlockAsDirReader implements BlockReader<Block> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final Path blockNodeRootPath;
    private final FileAttribute<Set<PosixFilePermission>> filePerms;

    /**
     * Constructor for the BlockAsDirReader class. It initializes the BlockAsDirReader with the
     * given parameters.
     *
     * @param config the configuration to retrieve the block node root path
     * @param filePerms the file permissions to set on the block node root path
     */
    BlockAsDirReader(
            @NonNull final PersistenceStorageConfig config,
            @NonNull final FileAttribute<Set<PosixFilePermission>> filePerms) {

        LOGGER.log(System.Logger.Level.INFO, "Initializing FileSystemBlockReader");

        @NonNull final Path blockNodeRootPath = Path.of(config.rootPath());

        LOGGER.log(System.Logger.Level.INFO, config.toString());
        LOGGER.log(System.Logger.Level.INFO, "Block Node Root Path: " + blockNodeRootPath);

        this.blockNodeRootPath = blockNodeRootPath;
        this.filePerms = filePerms;
    }

    /**
     * Reads a block from the file system. The block is stored as a directory containing block
     * items. The block items are stored as files within the block directory.
     *
     * @param blockNumber the block number to read
     * @return an optional of the block read from the file system
     * @throws IOException if an I/O error occurs
     */
    @NonNull
    @Override
    public Optional<Block> read(final long blockNumber) throws IOException {

        // Verify path attributes of the block node root path
        if (isPathDisqualified(blockNodeRootPath)) {
            return Optional.empty();
        }

        // Verify path attributes of the block directory within the
        // block node root path
        @NonNull final Path blockPath = blockNodeRootPath.resolve(String.valueOf(blockNumber));
        if (isPathDisqualified(blockPath)) {
            return Optional.empty();
        }

        try {
            // There may be thousands of BlockItem files in a Block directory.
            // The BlockItems must be added to the outbound Block object in order.
            // However, using something like DirectoryStream will iterate without
            // any guaranteed order. To avoid sorting and to keep the retrieval
            // process linear with the number of BlockItems in the Block, run a loop
            // to fetch BlockItems in the expected order. For example, in a Block
            // directory "1" containing 10 BlockItem files (1.blk, 2.blk, 3.blk, ...,
            // 10.blk), the loop will directly fetch the BlockItems in order based on
            // their file names. The loop will exit when it attempts to read a
            // BlockItem file that does not exist (e.g., 11.blk).
            @NonNull final Builder builder = Block.newBuilder();
            for (int i = 1; ; i++) {
                @NonNull final Path blockItemPath = blockPath.resolve(i + BLOCK_FILE_EXTENSION);
                @NonNull
                final Optional<BlockItem> blockItemOpt = readBlockItem(blockItemPath.toString());
                if (blockItemOpt.isPresent()) {
                    builder.addBlockItems(blockItemOpt.get());
                    continue;
                }

                break;
            }

            // Return the Block
            return Optional.of(builder.build());
        } catch (IOException io) {
            LOGGER.log(System.Logger.Level.ERROR, "Error reading block: " + blockPath, io);

            throw io;
        }
    }

    @NonNull
    private Optional<BlockItem> readBlockItem(@NonNull final String blockItemPath)
            throws IOException {

        try (FileInputStream fis = new FileInputStream(blockItemPath)) {
            return Optional.of(BlockItem.parseFrom(fis));
        } catch (FileNotFoundException io) {
            final File f = new File(blockItemPath);
            if (!f.exists()) {
                // The outer loop caller will continue to query
                // for the next BlockItem file based on the index
                // until the FileNotFoundException is thrown.
                // It's expected that this exception will be caught
                // at the end of every query.
                return Optional.empty();
            }

            // FileNotFound is also thrown when a file cannot be read.
            // So re-throw here to make a different decision upstream.
            throw io;
        }
    }

    private boolean isPathDisqualified(@NonNull final Path path) {

        if (!path.toFile().exists()) {
            // This code path gets hit if a consumer
            // requests a block that does not exist.
            // Only log this as a debug message.
            LOGGER.log(System.Logger.Level.DEBUG, "Path not found: {0}", path);
            return true;
        }

        if (!path.toFile().canRead()) {
            LOGGER.log(System.Logger.Level.ERROR, "Path not readable: {0}", path);
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Attempting to repair the path permissions: {0}",
                    path);

            try {
                // If resetting the permissions fails or
                // if the path is still unreadable, return true.
                setPerm(path, filePerms.value());
                if (!path.toFile().canRead()) {
                    return true;
                }
            } catch (IOException e) {
                LOGGER.log(
                        System.Logger.Level.ERROR, "Error setting permissions on: {0}" + path, e);
                return true;
            }
        }

        if (!path.toFile().isDirectory()) {
            LOGGER.log(System.Logger.Level.ERROR, "Path is not a directory: {0}", path);
            return true;
        }

        return false;
    }

    /**
     * Sets the permissions on the given path. This method is protected to allow for testing.
     *
     * @param path the path to set the permissions on
     * @param perms the permissions to set on the path
     * @throws IOException if an I/O error occurs
     */
    protected void setPerm(@NonNull final Path path, @NonNull final Set<PosixFilePermission> perms)
            throws IOException {
        Files.setPosixFilePermissions(path, perms);
    }
}
