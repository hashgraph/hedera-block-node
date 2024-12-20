// SPDX-License-Identifier: Apache-2.0
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
        Preconditions.requireWhole(blockNumber);
        return liveRootPath.resolve(String.valueOf(blockNumber));
    }
}
