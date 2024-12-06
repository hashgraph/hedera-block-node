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

/**
 * A no-op Block remover.
 */
public final class NoOpBlockRemover implements BlockRemover {
    /**
     * Constructor.
     */
    private NoOpBlockRemover() {}

    /**
     * This method creates and returns a new instance of {@link NoOpBlockRemover}.
     *
     * @return a new, fully initialized instance of {@link NoOpBlockRemover}
     */
    public static NoOpBlockRemover newInstance() {
        return new NoOpBlockRemover();
    }

    /**
     * No-op remover. Does nothing and returns immediately. No preconditions
     * check also.
     */
    @Override
    public void remove(final long blockNumber) {
        // do nothing
    }
}
