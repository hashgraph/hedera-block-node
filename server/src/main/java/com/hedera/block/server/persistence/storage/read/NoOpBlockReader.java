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

import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Optional;

/**
 * A no-op Block reader.
 */
public final class NoOpBlockReader implements BlockReader<BlockUnparsed> {
    /**
     * Constructor.
     */
    private NoOpBlockReader() {}

    /**
     * This method creates and returns a new instance of
     * {@link NoOpBlockReader}.
     *
     * @return a new, fully initialized instance of
     * {@link NoOpBlockReader}
     */
    public static NoOpBlockReader newInstance() {
        return new NoOpBlockReader();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<BlockUnparsed> read(final long blockNumber) throws IOException, ParseException {
        return Optional.empty();
    }
}
