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

package com.hedera.block.simulator.simulator;

public enum BlockStreamRateMode {

    /** There will be a small delay between block, block item stream will be constant and fixed */
    CONSTANT_RATE,

    /**
     * It will attempt to stream a whole block every X amount of time, only exception when the block
     * is big that takes more than X
     */
    SECONDS_PER_BLOCK,

    /** The rate mode is random. Between X and Y (Not yet implemented) */
    RANDOM;

    /**
     * Returns the rate mode from the given string.
     *
     * @param rateMode the rate mode string
     * @return the rate mode
     */
    public static BlockStreamRateMode fromString(String rateMode) {
        return switch (rateMode) {
            case "CONSTANT_RATE" -> CONSTANT_RATE;
            case "SECONDS_PER_BLOCK" -> SECONDS_PER_BLOCK;
            case "RANDOM" -> RANDOM;
            default -> throw new IllegalArgumentException("Invalid rate mode: " + rateMode);
        };
    }
}
