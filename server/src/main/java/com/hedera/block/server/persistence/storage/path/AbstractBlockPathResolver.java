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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * TODO: add documentation
 */
abstract class AbstractBlockPathResolver implements BlockPathResolver {
    protected final Path blockStorageRoot;

    AbstractBlockPathResolver(@NonNull final Path blockStorageRoot) {
        this.blockStorageRoot = Objects.requireNonNull(blockStorageRoot);
    }

    @Override
    public final boolean existsBlock(final long blockNumber) {
        return Files.exists(resolvePathToBlock(Preconditions.requirePositive(blockNumber)));
        // todo do we have block number 0? change precondition?
    }

    @Override
    public final boolean notExistsBlock(final long blockNumber) {
        return Files.notExists(resolvePathToBlock(Preconditions.requirePositive(blockNumber)));
        // todo do we have block number 0? change precondition?
    }
}
