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

package com.hedera.block.simulator.grpc;

import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;

/**
 * The PublishStreamGrpcClient interface provides the methods to stream the block and block item.
 */
public interface PublishStreamGrpcClient {
    /**
     * Streams the block item.
     *
     * @param blockItem the block item to be streamed
     * @return true if the block item is streamed successfully, false otherwise
     */
    boolean streamBlockItem(BlockItem blockItem);

    /**
     * Streams the block.
     *
     * @param block the block to be streamed
     * @return true if the block is streamed successfully, false otherwise
     */
    boolean streamBlock(Block block);
}
