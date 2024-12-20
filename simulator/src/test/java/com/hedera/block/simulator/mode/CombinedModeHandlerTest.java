// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.mode;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CombinedModeHandlerTest {

    private CombinedModeHandler combinedModeHandler;

    @Test
    void testStartThrowsUnsupportedOperationException() {
        combinedModeHandler = new CombinedModeHandler();

        assertThrows(UnsupportedOperationException.class, () -> combinedModeHandler.start());
    }
}
