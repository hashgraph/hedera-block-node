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
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A Block path resolver for block-as-dir.
 */
public final class BlockAsLocalDirPathResolver implements BlockPathResolver {
    private final Path liveRootPath;

    /**
     * Constructor.
     *
     * @param liveRootPath valid, {@code non-null} instance of {@link Path}
     * that points to the live root of the block storage
     */
    private BlockAsLocalDirPathResolver(@NonNull final Path liveRootPath) {
        this.liveRootPath = Objects.requireNonNull(liveRootPath);
    }

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalDirPathResolver}.
     *
     * @param liveRootPath valid, {@code non-null} instance of {@link Path}
     * that points to the live root of the block storage
     * @return a new, fully initialized instance of
     * {@link BlockAsLocalDirPathResolver}
     */
    public static BlockAsLocalDirPathResolver of(@NonNull final Path liveRootPath) {
        return new BlockAsLocalDirPathResolver(liveRootPath);
    }

    @NonNull
    @Override
    public Path resolvePathToBlock(final long blockNumber) {
        return resolvePathToBlock(blockNumber, "");
    }

    @NonNull
    @Override
    public Path resolvePathToBlock(final long blockNumber, @NonNull final String compressionExtension) {
        Preconditions.requireWhole(blockNumber);
        return liveRootPath.resolve(String.valueOf(blockNumber));
    }
}
