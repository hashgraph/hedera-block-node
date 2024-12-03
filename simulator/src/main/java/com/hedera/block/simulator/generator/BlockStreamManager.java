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

package com.hedera.block.simulator.generator;

import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.hapi.block.stream.protoc.Block;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import java.io.IOException;

/** The block stream manager interface. */
public interface BlockStreamManager {

    /**
     * Initialize the block stream manager and load blocks into memory.
     */
    default void init() {}

    /**
     * Get the generation mode.
     *
     * @return the generation mode
     */
    GenerationMode getGenerationMode();

    /**
     * Get the next block item.
     *
     * @return the next block item
     * @throws IOException if a I/O error occurs
     * @throws BlockSimulatorParsingException if a parse error occurs
     */
    BlockItem getNextBlockItem() throws IOException, BlockSimulatorParsingException;

    /**
     * Get the next block.
     *
     * @return the next block
     * @throws IOException if a I/O error occurs
     * @throws BlockSimulatorParsingException if a parse error occurs
     */
    Block getNextBlock() throws IOException, BlockSimulatorParsingException;
}
