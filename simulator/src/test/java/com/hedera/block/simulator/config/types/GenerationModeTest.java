// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.config.types;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GenerationModeTest {

    @Test
    void testGenerationMode() {
        GenerationMode mode = GenerationMode.DIR;
        assertEquals(GenerationMode.DIR, mode);
        mode = GenerationMode.ADHOC;
        assertEquals(GenerationMode.ADHOC, mode);
    }
}
