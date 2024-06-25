/*
 * Hedera Block Node
 *
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

package com.hedera.block.server.persistence.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A custom implementation of a LinkedHashMap that removes the eldest entry when the size exceeds a
 * specified limit.
 *
 * @param <K> the type of the keys
 * @param <V> the type of the values
 */
public class BNLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    private final long maxEntries;

    /**
     * Constructor for the BNLinkedHashMap class.
     *
     * @param maxEntries the maximum number of entries in the map
     */
    BNLinkedHashMap(final long maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * Removes the eldest entry when the size exceeds the maximum number of entries.
     *
     * @param eldest the eldest entry
     * @return true if the eldest entry should be removed, false otherwise
     */
    @Override
    protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {

        if (size() > maxEntries) {
            return true;
        }

        return size() > maxEntries;
    }
}
