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

import static org.junit.jupiter.api.Assertions.*;

class StreamingModeTest {

    @org.junit.jupiter.api.Test
    void fromString() {
        assertEquals(StreamingMode.CONSTANT_RATE, StreamingMode.fromString("CONSTANT_RATE"));
        assertEquals(StreamingMode.MILLIS_PER_BLOCK, StreamingMode.fromString("MILLIS_PER_BLOCK"));
        assertThrows(IllegalArgumentException.class, () -> StreamingMode.fromString("INVALID"));
    }
}
