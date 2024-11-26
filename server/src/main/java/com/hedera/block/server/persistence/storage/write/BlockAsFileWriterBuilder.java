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

package com.hedera.block.server.persistence.storage.write;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.BlockItemUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Objects;

/**
 * A builder for {@link BlockAsFileWriter}.
 */
public final class BlockAsFileWriterBuilder {
    private final BlockNodeContext blockNodeContext;
    private final BlockRemover blockRemover;
    private final BlockPathResolver blockPathResolver;

    /**
     * Constructor.
     *
     * @param blockNodeContext valid, {@code non-null} instance of {@link BlockNodeContext}
     * @param blockRemover valid, {@code non-null} instance of {@link BlockRemover}
     * @param blockPathResolver valid, {@code non-null} instance of {@link BlockPathResolver}
     */
    private BlockAsFileWriterBuilder(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final BlockPathResolver blockPathResolver) {
        this.blockNodeContext = Objects.requireNonNull(blockNodeContext);
        this.blockRemover = Objects.requireNonNull(blockRemover);
        this.blockPathResolver = Objects.requireNonNull(blockPathResolver);
    }

    /**
     * This method creates a new instance of {@link BlockAsFileWriterBuilder}.
     *
     * @param blockNodeContext valid, {@code non-null} instance of {@link BlockNodeContext}
     * @param blockRemover valid, {@code non-null} instance of {@link BlockRemover}
     * @param blockPathResolver valid, {@code non-null} instance of {@link BlockPathResolver}
     * @return a new instance of {@link BlockAsFileWriterBuilder}
     */
    public static BlockAsFileWriterBuilder newBuilder(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final BlockPathResolver blockPathResolver) {
        return new BlockAsFileWriterBuilder(blockNodeContext, blockRemover, blockPathResolver);
    }

    /**
     * This method builds a new instance of {@link BlockAsFileWriter} using the
     * properties set within this builder.
     *
     * @return a new, fully initialized instance of {@link BlockAsFileWriter}
     */
    public BlockWriter<List<BlockItemUnparsed>> build() {
        return new BlockAsFileWriter(blockNodeContext, blockRemover, blockPathResolver);
    }
}
