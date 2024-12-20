// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerConfigTest {

    @BeforeEach
    void setUp() {}

    @AfterEach
    void tearDown() {}

    @Test
    void testValidValues() {

        ServerConfig serverConfig = new ServerConfig(4_194_304, 8080);

        assertEquals(4_194_304, serverConfig.maxMessageSizeBytes());
        assertEquals(8080, serverConfig.port());
    }

    @Test
    void testMessageSizeTooBig() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ServerConfig(16_777_216, 8080);
        });
    }

    @Test
    void testMessageSizeTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ServerConfig(10_239, 8080);
        });
    }

    @Test
    void testPortValueTooBig() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ServerConfig(4_194_304, 65_536);
        });
    }

    @Test
    void testPortValueTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ServerConfig(4_194_304, 1023);
        });
    }
}
