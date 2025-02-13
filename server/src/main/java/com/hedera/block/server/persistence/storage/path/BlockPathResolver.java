// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
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
     * {@value com.hedera.block.server.Constants#BLOCK_FILE_EXTENSION} is
     * appended to the resolved Block path. No compression extension is appended
     * to the file name. No other file extension is appended to the file name.
     * The provided path is the raw resolved path to the Block inside the live
     * root storage.
     * <br/>
     * <br/>
     * E.G. (illustrative example, actual path may vary):
     * <pre>
     *     If the blockNumber is 10, the resolved path will be:
     *     <b>/path/to/live/block/storage/0.../1/0000000000000000010.blk</b>
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
     * {@link Path}. The Unverified Block File extension
     * {@value com.hedera.block.server.Constants#UNVERIFIED_BLOCK_FILE_EXTENSION} is
     * appended to the resolved Block path. No compression extension is appended
     * to the file name. No other file extension is appended to the file name.
     * The provided path is the raw resolved path to the Block inside the live
     * root storage.
     * <br/>
     * <br/>
     * E.G. (illustrative example, actual path may vary):
     * <pre>
     *     If the blockNumber is 10, the resolved path will be:
     *     <b>/path/to/live/block/storage/0.../1/0000000000000000010.blk.unverified</b>
     * </pre>
     *
     * @param blockNumber to be resolved the path for
     * @return the resolved path to the given Block by a number
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @NonNull
    Path resolveLiveRawUnverifiedPathToBlock(final long blockNumber);

    /**
     * This method attempts to find a Block by a given number under the
     * persistence storage live root. This method will ONLY check for VERIFIED
     * persisted Blocks. If the Block is found, the method returns a non-empty
     * {@link Optional} of {@link LiveBlockPath}, else an empty {@link Optional}
     * is returned.
     *
     * @param blockNumber to be resolved the path for
     * @return a {@link Optional} of {@link LiveBlockPath} if the block is found
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @NonNull
    Optional<LiveBlockPath> findLiveBlock(final long blockNumber);

    /**
     * This method attempts to find a Block by a given number under the
     * persistence storage archive. This method will ONLY check for
     * ARCHIVED persisted Blocks. Unverified Blocks cannot be archived, so it is
     * inferred that all Blocks, found in the are verified. If the Block is
     * found, the method returns a non-empty {@link Optional} of
     * {@link ArchiveBlockPath}, else an empty {@link Optional} is returned.
     *
     * @param blockNumber to be resolved the path for
     * @return a {@link Optional} of {@link ArchiveBlockPath} if the block is found
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    @NonNull
    Optional<ArchiveBlockPath> findArchivedBlock(final long blockNumber);

    /**
     * This method attempts to find a VERIFIED Block by a given number under the
     * persistence storage live root, OR an ARCHIVED Block by that given number.
     * If any of those checks produce a successful find, the method returns
     * {@code true}, else {@code false}.
     *
     * @param blockNumber to be resolved the path for
     * @return a {@link Optional} of {@link LiveBlockPath} if the block is found
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    boolean existsVerifiedBlock(final long blockNumber);

    /**
     * This method marks a Block as verified. The Block is identified by the
     * given block number. The method will attempt to find the unverified Block
     * under the live root storage, and if found, it will mark it as verified.
     * If the Block is found under the archive storage, it will be marked as
     * verified. If the unverified Block is not found under the live root
     * storage,the method will do nothing.
     *
     * @param blockNumber to be marked as verified
     * @throws IOException when failing to mark a block as verified
     * @throws IllegalArgumentException if the blockNumber IS NOT a whole number
     */
    void markVerified(final long blockNumber) throws IOException;
}
