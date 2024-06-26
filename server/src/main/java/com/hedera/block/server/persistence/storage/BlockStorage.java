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

package com.hedera.block.server.persistence.storage;

import java.util.Optional;

/**
 * The BlockStorage interface defines operations to write and read blocks to a persistent store.
 *
 * @param <V> the type of block to store
 */
public interface BlockStorage<V> {

    /**
     * Writes a block to storage.
     *
     * @param block the block to write
     * @return the id of the block
     */
    Optional<Long> write(final V block);

    /**
     * Reads a block from storage.
     *
     * @param blockId the id of the block to read
     * @return the block
     */
    Optional<V> read(final Long blockId);
}
