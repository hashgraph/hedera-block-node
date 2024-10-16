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

package com.hedera.block.simulator.config.types;

/** The StreamingMode enum defines the different modes for streaming blocks. */
public enum StreamingMode {

    /** It will wait X Nanos between each block. */
    CONSTANT_RATE,

    /** It will attempt to send a block each X Millis. */
    MILLIS_PER_BLOCK;

    /**
     * Converts a string to a StreamingMode.
     *
     * @param mode the string to convert
     * @return the StreamingMode
     */
    public static StreamingMode fromString(String mode) {
        return switch (mode) {
            case "CONSTANT_RATE" -> CONSTANT_RATE;
            case "MILLIS_PER_BLOCK" -> MILLIS_PER_BLOCK;
            default -> throw new IllegalArgumentException("Invalid mode: " + mode);
        };
    }
}
