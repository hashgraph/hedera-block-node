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
import static com.hedera.block.server.Constants.BLOCK_NODE_LIVE_ROOT_DIRECTORY_SEMANTIC_NAME;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.BlocksPersisted;
import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.common.utils.FileUtilities;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
public class BlockAsLocalDirWriter implements LocalBlockWriter<List<BlockItemUnparsed>> {
    private static final Logger LOGGER = System.getLogger(BlockAsLocalDirWriter.class.getName());
    private static final FileAttribute<Set<PosixFilePermission>> DEFAULT_FOLDER_PERMISSIONS =
            PosixFilePermissions.asFileAttribute(Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE));
    private final Path liveRootPath;
    private final MetricsService metricsService;
    private final BlockRemover blockRemover;
    private final BlockPathResolver blockPathResolver;
    private long blockNodeFileNameIndex;
    private long currentBlockNumber;

    /**
     * Use the corresponding builder to construct a new BlockAsDirWriter with the given parameters.
     *
     * @param blockNodeContext the block node context to use for writing blocks
     * @param blockRemover the block remover to use for removing blocks
     * @param blockPathResolver used internally to resolve paths
     *
     * @throws IOException if an error occurs while initializing the BlockAsDirWriter
     */
    protected BlockAsLocalDirWriter(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final BlockPathResolver blockPathResolver)
            throws IOException {
        LOGGER.log(INFO, "Initializing %s...".formatted(getClass().getName()));

        this.metricsService = Objects.requireNonNull(blockNodeContext.metricsService());
        this.blockRemover = Objects.requireNonNull(blockRemover);
        this.blockPathResolver = Objects.requireNonNull(blockPathResolver);

        final PersistenceStorageConfig config =
                blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        this.liveRootPath = Path.of(config.liveRootPath());

        // Initialize the block node root directory if it does not exist
        FileUtilities.createFolderPathIfNotExists(
                liveRootPath, INFO, DEFAULT_FOLDER_PERMISSIONS, BLOCK_NODE_LIVE_ROOT_DIRECTORY_SEMANTIC_NAME);
    }

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalDirWriter}.
     *
     * @param blockNodeContext valid, {@code non-null} instance of
     * {@link BlockNodeContext} used to get the {@link MetricsService}
     * @param blockRemover valid, {@code non-null} instance of
     * {@link BlockRemover} used to remove blocks in case of cleanup
     * @param blockPathResolver valid, {@code non-null} instance of
     * {@link BlockPathResolver} used to resolve paths to Blocks
     * @return a new, fully initialized instance of {@link BlockAsLocalDirWriter}
     * @throws IOException if an error occurs while initializing the BlockAsDirWriter
     */
    public static BlockAsLocalDirWriter of(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final BlockPathResolver blockPathResolver)
            throws IOException {
        return new BlockAsLocalDirWriter(blockNodeContext, blockRemover, blockPathResolver);
    }

    /**
     * Writes the given block item to the filesystem.
     *
     * @param valueToWrite the block item to write
     * @throws IOException if an error occurs while writing the block item
     */
    @NonNull
    @Override
    public Optional<List<BlockItemUnparsed>> write(@NonNull final List<BlockItemUnparsed> valueToWrite)
            throws IOException, ParseException {
        final Bytes unparsedBlockHeader = valueToWrite.getFirst().blockHeader();
        if (unparsedBlockHeader != null) {
            resetState(BlockHeader.PROTOBUF.parse(unparsedBlockHeader));
        }

        for (final BlockItemUnparsed blockItemUnparsed : valueToWrite) {
            final Path blockItemFilePath = calculateBlockItemPath();
            for (int retries = 0; ; retries++) {
                try {
                    write(blockItemFilePath, blockItemUnparsed);
                    break;
                } catch (final IOException e) {

                    LOGGER.log(ERROR, "Error writing the BlockItem protobuf to a file: ", e);

                    // Remove the block if repairing the permissions fails
                    if (retries > 0) {
                        // Attempt to remove the block
                        blockRemover.remove(currentBlockNumber);
                        throw e;
                    } else {
                        // Attempt to repair the permissions on the block path
                        // and the blockItem path
                        repairPermissions(liveRootPath);
                        repairPermissions(blockPathResolver.resolvePathToBlock(currentBlockNumber));
                        LOGGER.log(INFO, "Retrying to write the BlockItem protobuf to a file");
                    }
                }
            }
        }

        if (valueToWrite.getLast().hasBlockProof()) {
            metricsService.get(BlocksPersisted).increment();
            return Optional.of(valueToWrite);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Writes the given block item to the filesystem. This method is protected to allow for testing.
     *
     * @param blockItemFilePath the path to the block item file
     * @param blockItem the block item to write
     * @throws IOException if an error occurs while writing the block item
     */
    protected void write(@NonNull final Path blockItemFilePath, @NonNull final BlockItemUnparsed blockItem)
            throws IOException {
        try (final FileOutputStream fos = new FileOutputStream(blockItemFilePath.toString())) {
            // Write the Bytes directly
            BlockItemUnparsed.PROTOBUF.toBytes(blockItem).writeTo(fos);
            LOGGER.log(DEBUG, "Successfully wrote the block item file: {0}", blockItemFilePath);
        } catch (final IOException e) {
            // fixme writeTo throws UncheckedIOException, we should handle other cases as well
            LOGGER.log(ERROR, "Error writing the BlockItem protobuf to a file: ", e);
            throw e;
        }
    }

    private void resetState(@NonNull final BlockHeader blockHeader) throws IOException {
        // Here a "block" is represented as a directory of BlockItems.
        // Create the "block" directory based on the block_number
        currentBlockNumber = blockHeader.number();

        // Check the blockNodeRootPath permissions and
        // attempt to repair them if possible
        repairPermissions(liveRootPath);

        // Construct the path to the block directory
        FileUtilities.createFolderPathIfNotExists(
                blockPathResolver.resolvePathToBlock(currentBlockNumber),
                DEBUG,
                DEFAULT_FOLDER_PERMISSIONS,
                BLOCK_NODE_LIVE_ROOT_DIRECTORY_SEMANTIC_NAME);

        // Reset
        blockNodeFileNameIndex = 0;
    }

    // todo do we need this method at all?
    private void repairPermissions(@NonNull final Path path) throws IOException {
        final boolean isWritable = Files.isWritable(path);
        if (!isWritable) {
            LOGGER.log(ERROR, "Block node root directory is not writable. Attempting to change the" + " permissions.");

            // Attempt to restore the permissions on the block node root directory
            Files.setPosixFilePermissions(path, DEFAULT_FOLDER_PERMISSIONS.value());
        }
    }

    @NonNull
    private Path calculateBlockItemPath() {
        // Build the path to a .blk file
        final Path blockPath = blockPathResolver.resolvePathToBlock(currentBlockNumber);
        blockNodeFileNameIndex++;
        return blockPath.resolve(blockNodeFileNameIndex + BLOCK_FILE_EXTENSION);
    }
}
