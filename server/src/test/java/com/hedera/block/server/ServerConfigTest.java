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
