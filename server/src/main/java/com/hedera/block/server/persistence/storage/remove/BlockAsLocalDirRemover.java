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

package com.hedera.block.server.persistence.storage.remove;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.ERROR;

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
     */
    @Override
    public void remove(final long blockNumber) throws IOException {
        // todo should we add file permissions normalization?
        final Path blockPath = blockPathResolver.resolvePathToBlock(blockNumber);
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
