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

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.Constants;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TODO: add documentation
 */
public final class BlockAsFilePathResolver extends AbstractPathResolver {
    private static final int MAX_LONG_DIGITS = 19;

    public BlockAsFilePathResolver(@NonNull final Path root) {
        super(root);
    }

    @Override
    public Path resolvePathToBlock(final long blockNumber) {
        Preconditions.requirePositive(blockNumber); // todo do we have block number 0?
        final String inputString = String.format("%0" + MAX_LONG_DIGITS + "d", blockNumber);
        final String[] blockPath = inputString.split("");
        final String blockFileName = blockPath[blockPath.length - 1].concat(Constants.BLOCK_FILE_EXTENSION);
        blockPath[blockPath.length -1] = blockFileName;
        return Paths.get(root.toAbsolutePath().toString(), blockPath);
    }
}