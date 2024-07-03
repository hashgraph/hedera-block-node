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

package com.hedera.block.server.data;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The ObjectEvent class defines a simple object event used to publish data to downstream
 * subscribers through the LMAX Disruptor RingBuffer.
 *
 * @param <T> the type of the data to publish
 */
public class ObjectEvent<T> {

    /** Constructor for the ObjectEvent class. */
    public ObjectEvent() {}

    private T val;

    /**
     * Sets the given value to be published to downstream subscribers through the LMAX Disruptor.
     * The value must not be null and the method is thread-safe.
     *
     * @param val the value to set
     */
    public void set(@NonNull final T val) {
        this.val = val;
    }

    /**
     * Gets the value of the event from the LMAX Disruptor on the consumer side. The method is
     * thread-safe.
     *
     * @return the value of the event
     */
    @NonNull
    public T get() {
        return val;
    }
}
