// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ConsumerConfigTest {

    @Test
    public void testConsumerConfig_happyPath() {
        ConsumerConfig consumerConfig = new ConsumerConfig(3000);
        assert (consumerConfig.timeoutThresholdMillis() == 3000);
    }

    @Test
    public void testConsumerConfig_negativeTimeoutThresholdMillis() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new ConsumerConfig(-1));
        assertEquals("Timeout threshold must be greater than 0", exception.getMessage());
    }
}
