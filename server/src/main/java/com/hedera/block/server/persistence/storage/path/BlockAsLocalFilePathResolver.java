// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import com.hedera.block.common.utils.FileUtilities;
import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.Constants;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A Block path resolver for block-as-file.
 */
public final class BlockAsLocalFilePathResolver implements BlockPathResolver {
    private static final int MAX_LONG_DIGITS = 19;
    private final Path liveRootPath;
    private final CompressionType compressionType;

    /**
     * Constructor.
     *
     * @param config valid, {@code non-null} instance of
     * {@link PersistenceStorageConfig} used for initializing the resolver
     */
    private BlockAsLocalFilePathResolver(@NonNull final PersistenceStorageConfig config) {
        this.liveRootPath = Path.of(config.liveRootPath());
        this.compressionType = config.compression();
    }

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalFilePathResolver}.
     *
     * @param config valid, {@code non-null} instance of
     * {@link PersistenceStorageConfig} used for initializing the resolver
     * @return a new, fully initialized instance of {@link BlockAsLocalFilePathResolver}
     */
    public static BlockAsLocalFilePathResolver of(@NonNull final PersistenceStorageConfig config) {
        return new BlockAsLocalFilePathResolver(config);
    }

    @NonNull
    @Override
    public Path resolveLiveRawPathToBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        final String rawBlockNumber = String.format("%0" + MAX_LONG_DIGITS + "d", blockNumber);
        final String[] blockPath = rawBlockNumber.split("");
        final String blockFileName = rawBlockNumber.concat(Constants.BLOCK_FILE_EXTENSION);
        blockPath[blockPath.length - 1] = blockFileName;
        return Path.of(liveRootPath.toString(), blockPath);
    }

    @NonNull
    @Override
    public Path resolveArchiveRawPathToBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @NonNull
    @Override
    public Optional<Path> findBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        final Path liveRawRootPath = resolveLiveRawPathToBlock(blockNumber);
        final Path compressionExtendedLiveRawRootPath =
                FileUtilities.appendExtension(liveRawRootPath, compressionType.getFileExtension());
        if (Files.exists(compressionExtendedLiveRawRootPath)) {
            return Optional.of(compressionExtendedLiveRawRootPath);
        } else if (Files.exists(liveRawRootPath)) {
            return Optional.of(liveRawRootPath);
        } else {
            return Optional.empty();
        }
    } // todo consider to add additional handling here, like test for other compression types (currently not existing)
    // and also look for archived blocks (will be implemented in a follow-up PR)
}
