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

import com.hedera.hapi.block.BlockItemUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The NoOpBlockWriter class is a stub implementation of the block writer intended for testing purposes only. It is
 * designed to isolate the Producer and Mediator components from storage implementation during testing while still
 * providing metrics and logging for troubleshooting.
 */
public final class NoOpBlockWriter implements BlockWriter<List<BlockItemUnparsed>> {
    /**
     * Constructor.
     */
    private NoOpBlockWriter() {}

    /**
     * This method creates and returns a new instance of
     * {@link NoOpBlockWriter}.
     *
     * @return a new, fully initialized instance of {@link NoOpBlockWriter}
     */
    public static NoOpBlockWriter newInstance() {
        return new NoOpBlockWriter();
    }

    @NonNull
    @Override
    public Optional<List<BlockItemUnparsed>> write(@NonNull final List<BlockItemUnparsed> valueToWrite)
            throws IOException {
        if (valueToWrite.getLast().hasBlockProof()) {
            // Returning the BlockItems triggers a
            // PublishStreamResponse to be sent to the
            // upstream producer.
            return Optional.of(valueToWrite);
        } else {
            return Optional.empty();
        }
    }
}
