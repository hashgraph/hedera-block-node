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

package com.hedera.block.server.persistence.storage.remove;

import java.io.IOException;

/**
 * A Block remover that handles block-as-local-file.
 */
public final class BlockAsLocalFileRemover implements LocalBlockRemover {
    /**
     * Constructor.
     */
    private BlockAsLocalFileRemover() {}

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalFileRemover}.
     *
     * @return a new fully initialized instance of
     * {@link BlockAsLocalFileRemover}
     */
    public static BlockAsLocalFileRemover newInstance() {
        return new BlockAsLocalFileRemover();
    }

    /**
     * Removes a block from the file system.
     *
     * @param blockNumber the id of the block to remove
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void remove(final long blockNumber) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
