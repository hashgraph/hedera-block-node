// SPDX-License-Identifier: Apache-2.0
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
