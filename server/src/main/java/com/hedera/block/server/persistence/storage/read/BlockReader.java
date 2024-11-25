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

package com.hedera.block.server.persistence.storage.read;

import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Optional;

/**
 * The BlockReader interface defines the contract for reading a block from storage.
 *
 * @param <V> the type of the block to read
 */
public interface BlockReader<V> {
    /**
     * Reads the block with the given block number.
     *
     * @param blockNumber the block number of the block to read
     * @return the block with the given block number
     * @throws IOException if an I/O error occurs fetching the block
     * @throws ParseException if the PBJ codec encounters a problem caused by I/O issues, malformed
     *     input data, or any other reason that prevents the parse() method from completing the
     *     operation when fetching the block.
     */
    @NonNull
    Optional<V> read(final long blockNumber) throws IOException, ParseException;
}
