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
