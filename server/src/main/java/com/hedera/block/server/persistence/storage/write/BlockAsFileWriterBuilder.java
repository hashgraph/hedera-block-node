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
import com.hedera.block.server.persistence.storage.path.PathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Objects;

/**
 * TODO: add documentation
 */
public final class BlockAsFileWriterBuilder {
    private final BlockNodeContext blockNodeContext;
    private final BlockRemover blockRemover;
    private final PathResolver pathResolver;

    private BlockAsFileWriterBuilder(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final PathResolver pathResolver) {
        this.blockNodeContext = Objects.requireNonNull(blockNodeContext);
        this.blockRemover = Objects.requireNonNull(blockRemover);
        this.pathResolver = Objects.requireNonNull(pathResolver);
    }

    public static BlockAsFileWriterBuilder newBuilder(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final PathResolver pathResolver) {
        return new BlockAsFileWriterBuilder(blockNodeContext, blockRemover, pathResolver);
    }

    public BlockWriter<List<BlockItem>> build() {
        return new BlockAsFileWriter(blockNodeContext, blockRemover, pathResolver);
    }
}
