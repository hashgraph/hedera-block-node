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

package com.hedera.block.server.mediator;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * The Publisher interface defines the contract for publishing data emitted by the producer to
 * downstream subscribers.
 *
 * @param <U> the type of data to publish
 */
public interface Publisher<U> {

    /**
     * Publishes the given data to the downstream subscribers.
     *
     * @param data the data emitted by an upstream producer to publish to downstream subscribers.
     * @throws IOException thrown if an I/O error occurs while publishing the item to the
     *     subscribers.
     */
    void publish(@NonNull final U data) throws IOException;
}