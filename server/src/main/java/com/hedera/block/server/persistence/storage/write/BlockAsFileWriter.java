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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * TODO: add documentation
 */
class BlockAsFileWriter extends AbstractBlockWriter<List<BlockItem>> {
    BlockAsFileWriter(@NonNull final BlockNodeContext blockNodeContext, @NonNull final BlockRemover blockRemover) {
        super(blockNodeContext.metricsService(), blockRemover);
    }

    @Override
    public Optional<List<BlockItem>> write(@NonNull final List<BlockItem> toWrite) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
