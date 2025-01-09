// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.remove;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * The BlockAsDirRemover class removes a block from the file system. The block is stored as a
 * directory containing block items. The block items are stored as files within the block directory.
 */
public final class BlockAsLocalDirRemover implements LocalBlockRemover {
    private final Logger LOGGER = System.getLogger(getClass().getName());
    private final BlockPathResolver blockPathResolver;

    /**
     * Constructor.
     *
     * @param blockPathResolver valid, {@code non-null} instance of
     * {@link BlockPathResolver} to be used internally to resolve paths to Block
     */
    private BlockAsLocalDirRemover(@NonNull final BlockPathResolver blockPathResolver) {
        this.blockPathResolver = Objects.requireNonNull(blockPathResolver);
    }

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalDirRemover}.
     *
     * @param blockPathResolver valid, {@code non-null} instance of
     * {@link BlockPathResolver} to be used internally to resolve paths to Block
     * @return a new, fully initialized instance of
     * {@link BlockAsLocalDirRemover}
     */
    public static BlockAsLocalDirRemover of(@NonNull final BlockPathResolver blockPathResolver) {
        return new BlockAsLocalDirRemover(blockPathResolver);
    }

    /**
     * Removes a block from the file system.
     *
     * @param blockNumber the id of the block to remove
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @Override
    public void remove(final long blockNumber) throws IOException {
        Preconditions.requireWhole(blockNumber);
        final Path blockPath = blockPathResolver.resolveLiveRawPathToBlock(blockNumber);
        if (Files.notExists(blockPath)) {
            LOGGER.log(ERROR, "Block cannot be deleted as it does not exist: {0}", blockNumber);
        } else {
            final boolean deleted = delete(blockPath.toFile());
            if (!deleted) {
                LOGGER.log(ERROR, "Failed to delete block: {0}", blockNumber);
            }
        }
    }

    private static boolean delete(@NonNull final File file) {
        // Recursively delete the contents
        // of the directory
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files != null) {
                for (final File f : files) {
                    delete(f);
                }
            }
        }
        return file.delete();
    }
}
