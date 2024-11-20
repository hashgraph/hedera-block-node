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
public interface PathResolver {
    // TODO extend this interface, we probably need more methods for checking if
    // a path exists or resolving for compressed/decompressed or resolving for
    // archived non archived etc. For now I only care to start writing blocks
    // as files to the disk and clean up the approach on how to do that.
    Path resolvePathToBlock(final long blockNumber);
}
