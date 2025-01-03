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
 * A Block path resolver. Used to resolve path to a given Block and all the
 * supporting related operations.
 */
public interface BlockPathResolver {
    /**
     * This method resolves the fs {@link Path} to a Block by a given input
     * number. This method does not guarantee that the returned {@link Path}
     * exists! This method is guaranteed to return a {@code non-null}
     * {@link Path}. No compression extension is appended to the file name.
     *
     * @param blockNumber to be resolved the path for
     * @return the resolved path to the given Block by a number
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @NonNull
    Path resolvePathToBlock(final long blockNumber);
}
