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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class MediatorConfigTest {

    @Test
    public void testMediatorConfig_happyPath() {
        MediatorConfig mediatorConfig = new MediatorConfig(2048, "");
        assertEquals(2048, mediatorConfig.ringBufferSize());
    }

    @Test
    public void testMediatorConfig_negativeRingBufferSize() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new MediatorConfig(-1, ""));
        assertEquals("Mediator Ring Buffer Size must be positive", exception.getMessage());
    }

    @Test
    public void testMediatorConfig_powerOf2Values() {

        int[] powerOf2Values = IntStream.iterate(2, n -> n * 2).limit(30).toArray();

        // Test the power of 2 values
        for (int powerOf2Value : powerOf2Values) {
            MediatorConfig mediatorConfig = new MediatorConfig(powerOf2Value, "");
            assertEquals(powerOf2Value, mediatorConfig.ringBufferSize());
        }

        // Test the non-power of 2 values
        for (int powerOf2Value : powerOf2Values) {
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> new MediatorConfig(powerOf2Value + 1, ""));
            assertEquals("Mediator Ring Buffer Size must be a power of 2", exception.getMessage());
        }
    }
}
