// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.read;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.persistence.storage.compression.Compression;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * A Block reader that reads block-as-file.
 */
public final class BlockAsLocalFileReader implements LocalBlockReader<BlockUnparsed> {
    private final BlockPathResolver pathResolver;
    private final Compression compression;

    /**
     * Constructor.
     *
     * @param pathResolver valid, {@code non-null} instance of
     * {@link BlockPathResolver} used to resolve paths to block files
     */
    private BlockAsLocalFileReader(
            @NonNull final Compression compression, @NonNull final BlockPathResolver pathResolver) {
        this.pathResolver = Objects.requireNonNull(pathResolver);
        this.compression = Objects.requireNonNull(compression);
    }

    /**
     * This method creates and returns a new instance of {@link BlockAsLocalFileReader}.
     *
     * @param compression valid, {@code non-null} instance of {@link Compression}
     * @param pathResolver valid, {@code non-null} instance of
     * {@link BlockPathResolver} used to resolve paths to block files
     * @return a new, fully initialized instance of {@link BlockAsLocalFileReader}
     */
    public static BlockAsLocalFileReader of(
            @NonNull final Compression compression, @NonNull final BlockPathResolver pathResolver) {
        return new BlockAsLocalFileReader(compression, pathResolver);
    }

    @NonNull
    @Override
    public Optional<BlockUnparsed> read(final long blockNumber) throws IOException, ParseException {
        Preconditions.requireWhole(blockNumber);
        final Optional<Path> optBlockPath = pathResolver.findBlock(blockNumber);
        if (optBlockPath.isPresent()) {
            return Optional.of(doRead(optBlockPath.get()));
        } else {
            return Optional.empty();
        }
    }

    private BlockUnparsed doRead(final Path resolvedBlockPath) throws IOException, ParseException {
        try (final ReadableStreamingData data =
                new ReadableStreamingData(compression.wrap(Files.newInputStream(resolvedBlockPath)))) {
            return BlockUnparsed.PROTOBUF.parse(data);
        } // todo this method must be extended to try to read uncompressed data if compressed data is not found
          // need some mechanism that would be convenient to use not only here but also in other places
    }
}
