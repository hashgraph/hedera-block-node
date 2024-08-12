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

package com.hedera.block.server.persistence.storage.write;

import static com.hedera.block.protos.BlockStreamService.BlockItem;
import static com.hedera.block.server.Constants.BLOCKNODE_STORAGE_ROOT_PATH_KEY;
import static com.hedera.block.server.Constants.BLOCK_FILE_EXTENSION;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.config.Config;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * The BlockAsDirWriter class writes a block to the filesystem block item by block item. In this
 * implementation, a block is represented as a directory of BlockItems. BlockAsDirWriter is stateful
 * and uses the known, deterministic block item attributes to create new "blocks" (directories) and
 * write block items to them. If an unexpected exception occurs during the write operation, the
 * BlockAsDirWriter will first try to correct file permissions if appropriate. It will then attempt
 * to remove the current, incomplete block (directory) before re-throwing the exception to the
 * caller.
 */
class BlockAsDirWriter implements BlockWriter<BlockItem> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final Path blockNodeRootPath;
    private long blockNodeFileNameIndex;
    private Path currentBlockDir;
    private final FileAttribute<Set<PosixFilePermission>> filePerms;
    private final BlockRemover blockRemover;
    private final BlockNodeContext blockNodeContext;

    /**
     * Constructor for the BlockAsDirWriter class. It initializes the BlockAsDirWriter with the
     * given key, config, block remover, and file permissions.
     *
     * @param key the key to use to retrieve the block node root path from the config
     * @param config the config to use to retrieve the block node root path
     * @param blockRemover the block remover to use to remove blocks if there is an exception while
     *     writing a partial block
     * @param filePerms the file permissions to set on the block node root path
     * @throws IOException if an error occurs while initializing the BlockAsDirWriter
     */
    BlockAsDirWriter(
            @NonNull final String key,
            @NonNull final Config config,
            @NonNull final BlockRemover blockRemover,
            @NonNull final FileAttribute<Set<PosixFilePermission>> filePerms,
            @NonNull final BlockNodeContext blockNodeContext)
            throws IOException {

        LOGGER.log(System.Logger.Level.INFO, "Initializing FileSystemBlockStorage");

        final Path blockNodeRootPath = Path.of(config.get(key).asString().get());

        LOGGER.log(System.Logger.Level.INFO, config.toString());
        LOGGER.log(System.Logger.Level.INFO, "Block Node Root Path: " + blockNodeRootPath);

        this.blockNodeRootPath = blockNodeRootPath;
        this.blockRemover = blockRemover;
        this.filePerms = filePerms;

        if (!blockNodeRootPath.isAbsolute()) {
            throw new IllegalArgumentException(
                    BLOCKNODE_STORAGE_ROOT_PATH_KEY + " must be an absolute path");
        }

        // Initialize the block node root directory if it does not exist
        createPath(blockNodeRootPath, System.Logger.Level.INFO);

        this.blockNodeContext = blockNodeContext;
    }

    /**
     * Writes the given block item to the filesystem.
     *
     * @param blockItem the block item to write
     * @throws IOException if an error occurs while writing the block item
     */
    @Override
    public void write(@NonNull final BlockItem blockItem) throws IOException {

        if (blockItem.hasHeader()) {
            resetState(blockItem);
        }

        @NonNull final Path blockItemFilePath = calculateBlockItemPath();
        for (int retries = 0; ; retries++) {
            try {
                write(blockItemFilePath, blockItem);
                break;
            } catch (IOException e) {

                LOGGER.log(
                        System.Logger.Level.ERROR,
                        "Error writing the BlockItem protobuf to a file: ",
                        e);

                // Remove the block if repairing the permissions fails
                if (retries > 0) {
                    // Attempt to remove the block
                    blockRemover.remove(Long.parseLong(currentBlockDir.toString()));
                    throw e;
                } else {
                    // Attempt to repair the permissions on the block path
                    // and the blockItem path
                    repairPermissions(blockNodeRootPath);
                    repairPermissions(calculateBlockPath());
                    LOGGER.log(
                            System.Logger.Level.INFO,
                            "Retrying to write the BlockItem protobuf to a file");
                }
            }
        }
    }

    /**
     * Writes the given block item to the filesystem. This method is protected to allow for testing.
     *
     * @param blockItemFilePath the path to the block item file
     * @param blockItem the block item to write
     * @throws IOException if an error occurs while writing the block item
     */
    protected void write(@NonNull final Path blockItemFilePath, @NonNull final BlockItem blockItem)
            throws IOException {
        try (@NonNull
                final FileOutputStream fos = new FileOutputStream(blockItemFilePath.toString())) {
            blockItem.writeTo(fos);
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Successfully wrote the block item file: {0}",
                    blockItemFilePath);
        } catch (IOException e) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Error writing the BlockItem protobuf to a file: ",
                    e);
            throw e;
        }
    }

    private void resetState(@NonNull final BlockItem blockItem) throws IOException {
        // Here a "block" is represented as a directory of BlockItems.
        // Create the "block" directory based on the block_number
        currentBlockDir = Path.of(String.valueOf(blockItem.getHeader().getBlockNumber()));

        // Check the blockNodeRootPath permissions and
        // attempt to repair them if possible
        repairPermissions(blockNodeRootPath);

        // Construct the path to the block directory
        createPath(calculateBlockPath(), System.Logger.Level.DEBUG);

        // Reset
        blockNodeFileNameIndex = 0;

        // Increment the block counter
        @NonNull final MetricsService metricsService = blockNodeContext.metricsService();
        metricsService.blocksPersisted.increment();
    }

    private void repairPermissions(@NonNull final Path path) throws IOException {
        final boolean isWritable = Files.isWritable(path);
        if (!isWritable) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Block node root directory is not writable. Attempting to change the"
                            + " permissions.");

            try {
                // Attempt to restore the permissions on the block node root directory
                Files.setPosixFilePermissions(path, filePerms.value());
            } catch (IOException e) {
                LOGGER.log(
                        System.Logger.Level.ERROR,
                        "Error setting permissions on the path: " + path,
                        e);
                throw e;
            }
        }
    }

    @NonNull
    private Path calculateBlockItemPath() {
        // Build the path to a .blk file
        @NonNull final Path blockPath = calculateBlockPath();
        blockNodeFileNameIndex++;
        return blockPath.resolve(blockNodeFileNameIndex + BLOCK_FILE_EXTENSION);
    }

    @NonNull
    private Path calculateBlockPath() {
        return blockNodeRootPath.resolve(currentBlockDir);
    }

    private void createPath(
            @NonNull final Path blockNodePath, @NonNull final System.Logger.Level logLevel)
            throws IOException {
        // Initialize the Block directory if it does not exist
        if (Files.notExists(blockNodePath)) {
            Files.createDirectory(blockNodePath, filePerms);
            LOGGER.log(logLevel, "Created block node root directory: " + blockNodePath);
        } else {
            LOGGER.log(logLevel, "Using existing block node root directory: " + blockNodePath);
        }
    }
}
