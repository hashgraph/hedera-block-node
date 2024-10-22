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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Utility class for chunking collections. */
public final class ChunkUtils {
    /** Chunks a collection into a list of lists of the specified size.
     * @param collection the collection to chunk, if the collection is empty, an empty list is returned.
     * @param chunkSize the size of each chunk
     *  */
    public static <T> List<List<T>> chunkify(
            @NonNull final Collection<T> collection, final int chunkSize) {
        Objects.requireNonNull(collection);
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than 0");
        }
        if (collection.isEmpty()) {
            return Collections.emptyList(); // or throw, depends on how we want to handle
        }
        final List<T> localCollection = List.copyOf(collection);
        final int localCollectionSize = localCollection.size();
        return IntStream.iterate(0, i -> i < localCollectionSize, i -> i + chunkSize)
                .mapToObj(
                        i ->
                                localCollection.subList(
                                        i, Math.min(i + chunkSize, localCollectionSize)))
                .collect(Collectors.toList());
    }

    private ChunkUtils() {}
}
