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
public class BlockAsDirRemover implements BlockRemover {
    private final Logger LOGGER = System.getLogger(getClass().getName());
    private final Path blockNodeRootPath;

    /**
     * Create a block remover to manage removing blocks from storage.
     *
     * @param blockNodeRootPath the root path where blocks are stored.
     */
    public BlockAsDirRemover(@NonNull final Path blockNodeRootPath) {
        this.blockNodeRootPath = Objects.requireNonNull(blockNodeRootPath);
    }

    /**
     * Removes a block from the file system.
     *
     * @param id the id of the block to remove
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void remove(final long id) throws IOException {
        // Calculate the block path and proactively set the permissions
        // for removal
        final Path blockPath = blockNodeRootPath.resolve(String.valueOf(id));
        if (Files.notExists(blockPath)) {
            LOGGER.log(ERROR, "Block does not exist: {0}", id);
            return;
        }
        // Best effort to delete the block
        if (!delete(blockPath.toFile())) {
            LOGGER.log(ERROR, "Failed to delete block: {0}", id);
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
