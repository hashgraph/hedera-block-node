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

import static com.hedera.block.server.Constants.FULL_SERVICE_NAME_BLOCK_ACCESS;
import static com.hedera.block.server.Constants.SERVICE_NAME_BLOCK_ACCESS;

import com.hedera.pbj.runtime.grpc.ServiceInterface;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;
import java.util.List;

/**
 * The PbjBlockAccessService interface provides type definitions and default method
 * implementations for PBJ to route gRPC requests to the block access services.
 */
public interface PbjBlockAccessService extends ServiceInterface {

    /**
     * BlockAccessMethod types define the gRPC methods available on the BlockAccessService.
     */
    enum BlockAccessMethod implements Method {
        /**
         * The singleBlock method represents the unary gRPC method
         * consumers should use to get specific Blocks from the Block Node.
         */
        singleBlock
    }

    /**
     * Streams the block item.
     *
     * @return the block item
     */
    @NonNull
    default String serviceName() {
        return SERVICE_NAME_BLOCK_ACCESS;
    }

    /**
     * Provides the full name of the BlockStreamService.
     *
     * @return the full name of the BlockStreamService.
     */
    @NonNull
    default String fullName() {
        return FULL_SERVICE_NAME_BLOCK_ACCESS;
    }

    /**
     * Provides the methods of the methods on the BlockStreamService.
     *
     * @return the methods of the BlockStreamService.
     */
    @NonNull
    default List<Method> methods() {
        return Arrays.asList(PbjBlockAccessService.BlockAccessMethod.values());
    }
}
