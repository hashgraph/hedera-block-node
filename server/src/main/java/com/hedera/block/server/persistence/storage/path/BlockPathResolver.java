// SPDX-License-Identifier: Apache-2.0
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
