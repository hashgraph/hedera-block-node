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

/**
 * A no-op path resolver.
 */
public final class NoOpBlockPathResolver implements BlockPathResolver {
    /**
     * Constructor.
     */
    private NoOpBlockPathResolver() {}

    /**
     * This method creates and returns a new instance of
     * {@link NoOpBlockPathResolver}.
     *
     * @return a new, fully initialized instance of
     * {@link NoOpBlockPathResolver}
     */
    public static NoOpBlockPathResolver newInstance() {
        return new NoOpBlockPathResolver();
    }

    /**
     * No-op resolver. Does nothing and always returns a path under '/tmp' that
     * resolves to 'blockNumber.tmp.blk'. No preconditions check also.
     */
    @NonNull
    @Override
    public Path resolvePathToBlock(final long blockNumber) {
        final String blockName = String.format("%d.tmp.blk", blockNumber);
        return Path.of("/tmp/hashgraph/blocknode/data/").resolve(blockName);
    }
}
