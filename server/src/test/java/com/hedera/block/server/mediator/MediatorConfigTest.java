// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.mediator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class MediatorConfigTest {

    @Test
    public void testMediatorConfig_happyPath() {
        MediatorConfig mediatorConfig = new MediatorConfig(2048, null);
        assertEquals(2048, mediatorConfig.ringBufferSize());
    }

    @Test
    public void testMediatorConfig_negativeRingBufferSize() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new MediatorConfig(-1, null));
        assertEquals("Mediator Ring Buffer Size must be positive", exception.getMessage());
    }

    @Test
    public void testMediatorConfig_powerOf2Values() {

        int[] powerOf2Values = IntStream.iterate(2, n -> n * 2).limit(30).toArray();

        // Test the power of 2 values
        for (int powerOf2Value : powerOf2Values) {
            MediatorConfig mediatorConfig = new MediatorConfig(powerOf2Value, null);
            assertEquals(powerOf2Value, mediatorConfig.ringBufferSize());
        }

        // Test the non-power of 2 values
        for (int powerOf2Value : powerOf2Values) {
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> new MediatorConfig(powerOf2Value + 1, null));
            assertEquals("Mediator Ring Buffer Size must be a power of 2", exception.getMessage());
        }
    }
}
