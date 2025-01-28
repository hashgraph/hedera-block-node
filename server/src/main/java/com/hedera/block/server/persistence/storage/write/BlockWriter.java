// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.write;

import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Optional;

/**
 * BlockWriter defines the contract for writing block items to storage.
 *
 * @param <T> the type of the block item to write
 * @param <R> the type of the return value
 */
public interface BlockWriter<T, R> {
    /**
     * Write the block item to storage.
     *
     * @param valueToWrite to storage.
     * @return an optional containing the block number written to storage if the item
     * was a block proof signaling the end of the block, an empty optional otherwise.
     * @throws IOException when failing to write the item to storage.
     * @throws ParseException when failing to parse a block item.
     */
    @NonNull
    Optional<R> write(@NonNull final T valueToWrite) throws IOException, ParseException;
}
