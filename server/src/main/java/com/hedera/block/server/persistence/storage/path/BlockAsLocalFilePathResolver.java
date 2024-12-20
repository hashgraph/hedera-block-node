// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.Constants;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A Block path resolver for block-as-file.
 */
public final class BlockAsLocalFilePathResolver implements BlockPathResolver {
    private static final int MAX_LONG_DIGITS = 19;
    private final Path liveRootPath;

    /**
     * Constructor.
     *
     * @param liveRootPath valid, {@code non-null} instance of {@link Path}
     * that points to the live root of the block storage
     */
    private BlockAsLocalFilePathResolver(@NonNull final Path liveRootPath) {
        this.liveRootPath = Objects.requireNonNull(liveRootPath);
    }

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalFilePathResolver}.
     *
     * @param liveRootPath valid, {@code non-null} instance of {@link Path}
     * that points to the live root of the block storage
     * @return a new, fully initialized instance of
     * {@link BlockAsLocalFilePathResolver}
     */
    public static BlockAsLocalFilePathResolver of(@NonNull final Path liveRootPath) {
        return new BlockAsLocalFilePathResolver(liveRootPath);
    }

    @NonNull
    @Override
    public Path resolvePathToBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        final String rawBlockNumber = String.format("%0" + MAX_LONG_DIGITS + "d", blockNumber);
        final String[] blockPath = rawBlockNumber.split("");
        final String blockFileName = rawBlockNumber.concat(Constants.BLOCK_FILE_EXTENSION);
        blockPath[blockPath.length - 1] = blockFileName;
        return Path.of(liveRootPath.toString(), blockPath);
    }
}
