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

    /**
     * This method resolves the fs {@link Path} to a Block by a given input
     * number. This method does not guarantee that the returned {@link Path}
     * exists! This method is guaranteed to return a {@code non-null}
     * {@link Path}. The Block File extension
     * {@link com.hedera.block.server.Constants#BLOCK_FILE_EXTENSION} is
     * appended to the resolved Block path. No compression extension is appended
     * to the file name. No other file extension is appended to the file name.
     * The provided path is the raw resolved path to the Block inside the
     * archive root storage.
     * <br/>
     * <br/>
     * E.G. (illustrative example, actual path may vary):
     * <pre>
     *     If the blockNumber is 10, the resolved path will be:
     *     <b>/path/to/archive/block/storage.zip/0000000000000000010.blk</b>
     * </pre>
     *
     * @param blockNumber to be resolved the path for
     * @return the resolved path to the given Block by a number
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @NonNull
    Path resolveArchiveRawPathToBlock(final long blockNumber);

    /**
     * This method resolves the fs {@link Path} to a Block by a given input
     * number. This method attempts to find the Block file, meaning it will try
     * to resolve the Block file path based on multiple tries:
     * <br/>
     * <br/>
     * <pre>
     *     1. Try to resolve the Block file path by the given Block number with
     *        the current compression extension appended to the file name in the
     *        live root.
     *     2. Try to resolve the Block file path by the given Block number with
     *        the current compression extension appended to the file name in the
     *        archive root.
     *     3. Try to resolve the Block file path by the given Block number with
     *        no compression extension appended to the file name in the live root.
     *     4. Try to resolve the Block file path by the given Block number with
     *        no compression extension appended to the file name in the archive root.
     * </pre>
     * We generally expect the Block files to always be compressed, but for best
     * effort, we must also try to resolve the Block file without the
     * compression extension if the Block file is not found with the compression
     * extension. This method is guaranteed to return a {@code non-null}
     * {@link Optional} value.
     *
     * @param blockNumber the block number to find the block file for
     * @return optional value, the path to the Block file by the given Block number
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @NonNull
    Optional<Path> findBlock(final long blockNumber);
}
