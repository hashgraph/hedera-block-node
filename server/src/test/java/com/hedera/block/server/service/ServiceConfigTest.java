// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ServiceConfigTest {
    @Test
    public void testServiceConfig_happyPath() {
        ServiceConfig serviceConfig = new ServiceConfig(2000);
        assertEquals(2000, serviceConfig.shutdownDelayMillis());
    }

    @Test
    public void testServiceConfig_negativeDelayMillis() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new ServiceConfig(-1));
        assertEquals("Delay milliseconds must be greater than 0", exception.getMessage());
    }
}
