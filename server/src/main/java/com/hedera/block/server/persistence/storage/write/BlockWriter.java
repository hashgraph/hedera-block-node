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
 */
public interface BlockWriter<T> {
    /**
     * Write the block item to storage.
     *
     * @param valueToWrite to storage.
     * @return an optional containing the item written to storage if the item
     * was a block proof signaling the end of the block, an empty optional otherwise.
     * @throws IOException when failing to write the item to storage.
     * @throws ParseException when failing to parse a block item.
     */
    @NonNull
    Optional<T> write(@NonNull final T valueToWrite) throws IOException, ParseException;
}
