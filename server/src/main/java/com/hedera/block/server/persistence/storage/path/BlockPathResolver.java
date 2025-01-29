// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A Block path resolver. Used to resolve path to a given Block and all the
 * supporting related operations.
 */
public interface BlockPathResolver {
    /**
     * This method resolves the fs {@link Path} to a Block by a given input
     * number. This method does not guarantee that the returned {@link Path}
     * exists! This method is guaranteed to return a {@code non-null}
     * {@link Path}. The Block File extension
     * {@link com.hedera.block.server.Constants#BLOCK_FILE_EXTENSION} is
     * appended to the resolved Block path. No compression extension is appended
     * to the file name. No other file extension is appended to the file name.
     * The provided path is the raw resolved path to the Block inside the live
     * root storage.
     * <br/>
     * <br/>
     * E.G. (illustrative example, actual path may vary):
     * <pre>
     *     If the blockNumber is 10, the resolved path will be:
     *     <b>/path/to/live/block/storage/0000000000000000010.blk</b>
     * </pre>
     *
     * @param blockNumber to be resolved the path for
     * @return the resolved path to the given Block by a number
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @NonNull
    Path resolveLiveRawPathToBlock(final long blockNumber);

    Optional<LiveBlockPath> findLiveBlock(final long blockNumber);

    Optional<ArchiveBlockPath> findArchivedBlock(final long blockNumber);
}
