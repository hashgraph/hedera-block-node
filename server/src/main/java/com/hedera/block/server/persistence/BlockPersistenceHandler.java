/*
 * Hedera Block Node
 *
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

package com.hedera.block.server.persistence;

import java.util.Optional;
import java.util.Queue;

/**
 * The BlockPersistenceHandler interface defines operations to persist and read blocks.
 * The interface is used to abstract underlying storage mechanisms.
 *
 * @param <V> the type of block to persist
 */
public interface BlockPersistenceHandler<V> {

    /**
     * Persists a block.
     *
     * @param block the block to persist
     * @return the id of the block
     */
    Long persist(final V block);

    /**
     * Reads a block.
     *
     * @param id the id of the block to read
     * @return the block
     */
    Optional<V> read(final long id);

    /**
     * Reads a range of blocks.
     *
     * @param startBlockId the id of the first block to read
     * @param endBlockId the id of the last block to read
     * @return a queue of blocks
     */
    Queue<V> readRange(final long startBlockId, final long endBlockId);
}
