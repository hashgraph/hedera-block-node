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

package com.hedera.block.server.pbj;

import static com.hedera.block.server.Constants.FULL_SERVICE_NAME_BLOCK_STREAM;
import static com.hedera.block.server.Constants.SERVICE_NAME_BLOCK_STREAM;

import com.hedera.pbj.runtime.grpc.ServiceInterface;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;
import java.util.List;

/**
 * The PbjBlockStreamService interface provides type definitions and default method
 * implementations for PBJ to route gRPC requests to the block stream services.
 */
public interface PbjBlockStreamService extends ServiceInterface {

    /**
     * BlockStreamMethod types define the gRPC methods available on the BlockStreamService.
     */
    enum BlockStreamMethod implements Method {
        /**
         * The publishBlockStream method represents the bidirectional gRPC streaming method
         * Consensus Nodes should use to publish the BlockStream to the Block Node.
         */
        publishBlockStream,

        /**
         * The subscribeBlockStream method represents the server-streaming gRPC method
         * consumers should use to subscribe to the BlockStream from the Block Node.
         */
        subscribeBlockStream
    }

    /**
     * Streams the block item.
     *
     * @return the block item
     */
    @NonNull
    default String serviceName() {
        return SERVICE_NAME_BLOCK_STREAM;
    }

    /**
     * Provides the full name of the BlockStreamService.
     *
     * @return the full name of the BlockStreamService.
     */
    @NonNull
    default String fullName() {
        return FULL_SERVICE_NAME_BLOCK_STREAM;
    }

    /**
     * Provides the methods of the methods on the BlockStreamService.
     *
     * @return the methods of the BlockStreamService.
     */
    @NonNull
    default List<Method> methods() {
        return Arrays.asList(BlockStreamMethod.values());
    }
}
