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
 * TODO: add documentation
 */
public final class NoOpBlockPathResolver implements BlockPathResolver {
    @Override
    public Path resolvePathToBlock(final long blockNumber) {
        return null;
    }

    @Override
    public boolean existsBlock(final long blockNumber) {
        return false;
    }

    @Override
    public boolean notExistsBlock(final long blockNumber) {
        return false;
    }
}
