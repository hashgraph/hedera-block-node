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

package com.hedera.block.server.persistence.storage.read;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileInputStream;
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

    /**
     * Constructor.
     *
     * @param pathResolver valid, {@code non-null} instance of
     * {@link BlockPathResolver} used to resolve paths to block files
     */
    private BlockAsLocalFileReader(@NonNull final BlockPathResolver pathResolver) {
        this.pathResolver = Objects.requireNonNull(pathResolver);
    }

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalFileReader}.
     *
     * @param pathResolver valid, {@code non-null} instance of
     * {@link BlockPathResolver} used to resolve paths to block files
     * @return a new, fully initialized instance of
     * {@link BlockAsLocalFileReader}
     */
    public static BlockAsLocalFileReader of(@NonNull final BlockPathResolver pathResolver) {
        return new BlockAsLocalFileReader(pathResolver);
    }

    @NonNull
    @Override
    public Optional<BlockUnparsed> read(final long blockNumber) throws IOException, ParseException {
        Preconditions.requireWhole(blockNumber);
        final Path resolvedBlockPath = pathResolver.resolvePathToBlock(blockNumber);
        if (Files.exists(resolvedBlockPath)) {
            return Optional.of(doRead(resolvedBlockPath));
        } else {
            // todo we generally expect the block to be there, so we
            // return an empty optional here, or do something else?
            // of course sometimes the block may not even exist yet.
            return Optional.empty();
        }
    }

    private BlockUnparsed doRead(final Path resolvedBlockPath) throws IOException, ParseException {
        // todo there are other ways of parsing, for example we can pass ReadableSequentialData instead of the fis
        // because the readAllBytes does not seem like the best idea, what is the correct (or better) way to parse
        // the block file?
        try (final FileInputStream fis = new FileInputStream(resolvedBlockPath.toFile())) {
            return BlockUnparsed.PROTOBUF.parse(Bytes.wrap(fis.readAllBytes()));
        }
    }
}
