// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import com.hedera.block.common.utils.FileUtilities;
import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A Block path resolver for block-as-dir.
 */
public final class BlockAsLocalDirPathResolver implements BlockPathResolver {
    private final Path liveRootPath;
    private final CompressionType compressionType;

    /**
     * Constructor.
     *
     * @param config valid, {@code non-null} instance of
     * {@link PersistenceStorageConfig} used for initializing the resolver
     */
    private BlockAsLocalDirPathResolver(@NonNull final PersistenceStorageConfig config) {
        this.liveRootPath = Path.of(config.liveRootPath());
        this.compressionType = config.compression();
    }

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalDirPathResolver}.
     *
     * @param config valid, {@code non-null} instance of
     * {@link PersistenceStorageConfig} used for initializing the resolver
     * @return a new, fully initialized instance of
     * {@link BlockAsLocalDirPathResolver}
     */
    public static BlockAsLocalDirPathResolver of(@NonNull final PersistenceStorageConfig config) {
        return new BlockAsLocalDirPathResolver(config);
    }

    @NonNull
    @Override
    public Path resolveLiveRawPathToBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        return liveRootPath.resolve(String.valueOf(blockNumber));
    }

    @NonNull
    @Override
    public Path resolveArchiveRawPathToBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @NonNull
    @Override
    public Optional<Path> findBlock(final long blockNumber) { // todo add archive root search & abstract if common
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
    }
}
