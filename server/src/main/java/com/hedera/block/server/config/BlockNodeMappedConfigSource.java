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

package com.hedera.block.server.config;

import com.swirlds.config.api.source.ConfigSource;
import com.swirlds.config.extensions.sources.ConfigMapping;
import com.swirlds.config.extensions.sources.MappedConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;

/**
 * A class that extends {@link MappedConfigSource} ir order to have project-relevant initialization.
 */
public final class BlockNodeMappedConfigSource extends MappedConfigSource {
    private static final List<ConfigMapping> MAPPINGS =
            List.of(
                    new ConfigMapping("mediator.ringBufferSize", "MEDIATOR_RING_BUFFER_SIZE"),
                    new ConfigMapping("notifier.ringBufferSize", "NOTIFIER_RING_BUFFER_SIZE"));

    /**
     * Constructor.
     *
     * @param wrappedSource valid, non-null instance of {@link ConfigSource} to be used internally
     */
    public BlockNodeMappedConfigSource(@NonNull final ConfigSource wrappedSource) {
        super(wrappedSource); // super checks for null
        init();
    }

    private void init() {
        MAPPINGS.forEach(super::addMapping);
    }
}
