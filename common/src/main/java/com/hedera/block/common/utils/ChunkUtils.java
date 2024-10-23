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

package com.hedera.block.common.utils;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Utility class for chunking collections. */
public final class ChunkUtils {
    /**
     * Chunk a collection into a list of lists.
     * The resulting list will have a specified size.
     *
     * @param dataToSplit the collection to chunk, if the collection is empty, an empty list is returned.
     * @param chunkSize the size of each chunk
     * @param <T> the type of the collection
     * @return a list of lists of the specified size
     *  */
    public static <T> List<List<T>> chunkify(
            @NonNull final Collection<T> dataToSplit, final int chunkSize) {
        Objects.requireNonNull(dataToSplit);
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than 0");
        }
        if (dataToSplit.isEmpty()) {
            return Collections.emptyList(); // or throw, depends on how we want to handle
        }
        final List<T> localCollection = List.copyOf(dataToSplit);
        final int localCollectionSize = localCollection.size();

        List<List<T>> result = new ArrayList<>();

        for (int i = 0; i < localCollectionSize; i += chunkSize) {
            int end = Math.min(i + chunkSize, localCollectionSize);
            result.add(localCollection.subList(i, end));
        }

        return result;
    }

    private ChunkUtils() {}
}
