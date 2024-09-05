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

import static com.hedera.block.server.Constants.BLOCK_FILE_EXTENSION;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.BlocksPersisted;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.FileUtils;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
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

    private final Logger LOGGER = System.getLogger(getClass().getName());

    private final Path blockNodeRootPath;
    private long blockNodeFileNameIndex;
    private Path currentBlockDir;
    private final FileAttribute<Set<PosixFilePermission>> filePerms;
    private final BlockRemover blockRemover;
    private final MetricsService metricsService;

    /**
     * Use the corresponding builder to construct a new BlockAsDirWriter with the given parameters.
     *
     * @param blockRemover the block remover to use for removing blocks
     * @param filePerms the file permissions to use for writing blocks
     * @param blockNodeContext the block node context to use for writing blocks
     * @throws IOException if an error occurs while initializing the BlockAsDirWriter
     */
    BlockAsDirWriter(
            @NonNull final BlockRemover blockRemover,
            @NonNull final FileAttribute<Set<PosixFilePermission>> filePerms,
            @NonNull final BlockNodeContext blockNodeContext)
            throws IOException {

        LOGGER.log(INFO, "Initializing FileSystemBlockStorage");

        PersistenceStorageConfig config =
                blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        final Path blockNodeRootPath = Path.of(config.rootPath());

        LOGGER.log(INFO, "Block Node Root Path: " + blockNodeRootPath);

        this.blockNodeRootPath = blockNodeRootPath;
        this.blockRemover = blockRemover;
        this.filePerms = filePerms;

        // Initialize the block node root directory if it does not exist
        FileUtils.createPathIfNotExists(blockNodeRootPath, INFO, filePerms);

        this.metricsService = blockNodeContext.metricsService();
    }

    /**
     * Writes the given block item to the filesystem.
     *
     * @param blockItem the block item to write
     * @throws IOException if an error occurs while writing the block item
     */
    @Override
    public void write(@NonNull final BlockItem blockItem) throws IOException {

        if (blockItem.hasBlockHeader()) {
            resetState(blockItem);
            metricsService.get(BlocksPersisted).increment();
        }

        final Path blockItemFilePath = calculateBlockItemPath();
        for (int retries = 0; ; retries++) {
            try {
                write(blockItemFilePath, blockItem);
                break;
            } catch (IOException e) {

                LOGGER.log(ERROR, "Error writing the BlockItem protobuf to a file: ", e);

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
                    LOGGER.log(INFO, "Retrying to write the BlockItem protobuf to a file");
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
        try (final FileOutputStream fos = new FileOutputStream(blockItemFilePath.toString())) {

            BlockItem.PROTOBUF.toBytes(blockItem).writeTo(fos);
            LOGGER.log(DEBUG, "Successfully wrote the block item file: {0}", blockItemFilePath);
        } catch (IOException e) {
            LOGGER.log(ERROR, "Error writing the BlockItem protobuf to a file: ", e);
            throw e;
        }
    }

    private void resetState(@NonNull final BlockItem blockItem) throws IOException {
        // Here a "block" is represented as a directory of BlockItems.
        // Create the "block" directory based on the block_number
        currentBlockDir = Path.of(String.valueOf(blockItem.blockHeader().number()));

        // Check the blockNodeRootPath permissions and
        // attempt to repair them if possible
        repairPermissions(blockNodeRootPath);

        // Construct the path to the block directory
        FileUtils.createPathIfNotExists(calculateBlockPath(), DEBUG, filePerms);

        // Reset
        blockNodeFileNameIndex = 0;
    }

    private void repairPermissions(@NonNull final Path path) throws IOException {
        final boolean isWritable = Files.isWritable(path);
        if (!isWritable) {
            LOGGER.log(
                    ERROR,
                    "Block node root directory is not writable. Attempting to change the"
                            + " permissions.");

            try {
                // Attempt to restore the permissions on the block node root directory
                Files.setPosixFilePermissions(path, filePerms.value());
            } catch (IOException e) {
                LOGGER.log(ERROR, "Error setting permissions on the path: " + path, e);
                throw e;
            }
        }
    }

    @NonNull
    private Path calculateBlockItemPath() {
        // Build the path to a .blk file
        final Path blockPath = calculateBlockPath();
        blockNodeFileNameIndex++;
        return blockPath.resolve(blockNodeFileNameIndex + BLOCK_FILE_EXTENSION);
    }

    @NonNull
    private Path calculateBlockPath() {
        return blockNodeRootPath.resolve(currentBlockDir);
    }
}
