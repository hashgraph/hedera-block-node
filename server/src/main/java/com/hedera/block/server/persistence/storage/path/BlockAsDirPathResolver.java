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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A Block path resolver for block-as-dir.
 */
public final class BlockAsDirPathResolver implements BlockPathResolver {
    private final Path blockStorageRoot;

    /**
     * Constructor.
     *
     * @param blockStorageRoot valid, {@code non-null} instance of {@link Path}
     * that points to the root of the block storage
     */
    public BlockAsDirPathResolver(@NonNull final Path blockStorageRoot) {
        this.blockStorageRoot = Objects.requireNonNull(blockStorageRoot);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Path resolvePathToBlock(final long blockNumber) {
        return blockStorageRoot.resolve(String.valueOf(blockNumber));
    }
}
