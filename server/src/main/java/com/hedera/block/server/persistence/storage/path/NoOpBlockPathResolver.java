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

package com.hedera.block.server.persistence.storage.path;

import java.nio.file.Path;

/**
 * A no-op path resolver.
 */
public final class NoOpBlockPathResolver implements BlockPathResolver {
    /**
     * No-op resolver. Does nothing and always returns null. No preconditions
     * check also.
     */
    @Override
    public Path resolvePathToBlock(final long blockNumber) {
        return null;
        // todo should we return some path here so it is not null?
        // reason would be to not have null pointer somewhere in our code
        // if for whatever reason this no op impl needs to be used?
    }
}
