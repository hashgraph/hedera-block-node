// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerConfigTest {

    private static final String RANGE_ERROR_TEMPLATE = "%s value %d is out of range [%d, %d]";

    @BeforeEach
    void setUp() {}

    @AfterEach
    void tearDown() {}

    @Test
    void testValidValues() {

        ServerConfig serverConfig = new ServerConfig(4_194_304, 32_768, 32_768, 8080);

        assertEquals(4_194_304, serverConfig.maxMessageSizeBytes());
        assertEquals(8080, serverConfig.port());
    }

    @Test
    void testMessageSizeTooBig() {
        try {
            new ServerConfig(16_777_216, 32_768, 32_768, 8080);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expectedErrorMsg = String.format(
                    RANGE_ERROR_TEMPLATE,
                    "server.maxMessageSizeBytes",
                    16_777_216,
                    ServerConfig.minMaxMessageSizeBytes,
                    ServerConfig.maxMaxMessageSizeBytes);
            assertEquals(expectedErrorMsg, e.getMessage());
        }
    }

    @Test
    void testMessageSizeTooSmall() {
        try {
            new ServerConfig(10_239, 32_768, 32_768, 8080);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expectedErrorMsg = String.format(
                    RANGE_ERROR_TEMPLATE,
                    "server.maxMessageSizeBytes",
                    10_239,
                    ServerConfig.minMaxMessageSizeBytes,
                    ServerConfig.maxMaxMessageSizeBytes);
            assertEquals(expectedErrorMsg, e.getMessage());
        }
    }

    @Test
    void testSocketSendBufferSizeTooSmall() {
        try {
            new ServerConfig(4_194_304, 32_767, 32_768, 8080);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expectedErrorMsg = String.format(
                    RANGE_ERROR_TEMPLATE,
                    "server.socketSendBufferSizeBytes",
                    32_767,
                    ServerConfig.minSocketSendBufferSizeBytes,
                    Integer.MAX_VALUE);
            assertEquals(expectedErrorMsg, e.getMessage());
        }
    }

    @Test
    void testSocketReceiveBufferSizeTooSmall() {
        try {
            new ServerConfig(4_194_304, 32_768, 32_767, 8080);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expectedErrorMsg = String.format(
                    RANGE_ERROR_TEMPLATE,
                    "server.socketReceiveBufferSizeBytes",
                    32_767,
                    ServerConfig.minSocketReceiveBufferSizeBytes,
                    Integer.MAX_VALUE);
            assertEquals(expectedErrorMsg, e.getMessage());
        }
    }

    @Test
    void testPortValueTooBig() {
        try {
            new ServerConfig(4_194_304, 32_768, 32_768, 65_536);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expectedErrorMsg = String.format(
                    RANGE_ERROR_TEMPLATE, "server.port", 65_536, ServerConfig.minPort, ServerConfig.maxPort);
            assertEquals(expectedErrorMsg, e.getMessage());
        }
    }

    @Test
    void testPortValueTooSmall() {
        try {
            new ServerConfig(4_194_304, 32_768, 32_768, 1023);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expectedErrorMsg = String.format(
                    RANGE_ERROR_TEMPLATE, "server.port", 1023, ServerConfig.minPort, ServerConfig.maxPort);
            assertEquals(expectedErrorMsg, e.getMessage());
        }
    }
}
