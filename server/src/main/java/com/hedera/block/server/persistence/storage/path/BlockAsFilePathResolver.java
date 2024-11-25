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

package com.hedera.block.server.persistence.storage.path;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.Constants;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * A Block path resolver for block-as-file.
 */
public final class BlockAsFilePathResolver implements BlockPathResolver {
    private static final int MAX_LONG_DIGITS = 19;
    private final Path blockStorageRoot;

    /**
     * Constructor.
     *
     * @param blockStorageRoot valid, {@code non-null} instance of {@link Path}
     * that points to the root of the block storage
     */
    public BlockAsFilePathResolver(@NonNull final Path blockStorageRoot) {
        this.blockStorageRoot = Objects.requireNonNull(blockStorageRoot);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Path resolvePathToBlock(final long blockNumber) {
        Preconditions.requirePositive(blockNumber); // todo do we have block number 0?
        final String rawBlockNumber = String.format("%0" + MAX_LONG_DIGITS + "d", blockNumber);
        final String[] blockPath = rawBlockNumber.split("");
        final String blockFileName = rawBlockNumber.concat(Constants.BLOCK_FILE_EXTENSION);
        blockPath[blockPath.length - 1] = blockFileName;
        return Paths.get(blockStorageRoot.toAbsolutePath().toString(), blockPath);
    }
}
