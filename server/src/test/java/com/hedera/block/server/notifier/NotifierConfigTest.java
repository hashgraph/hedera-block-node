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

package com.hedera.block.server.notifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class NotifierConfigTest {

    @Test
    public void testNotifierConfig_happyPath() {
        NotifierConfig notifierConfig = new NotifierConfig(2048);
        assertEquals(2048, notifierConfig.ringBufferSize());
    }

    @Test
    public void testNotifierConfig_negativeRingBufferSize() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new NotifierConfig(-1));
        assertEquals("Ring buffer size must be greater than 0", exception.getMessage());
    }
}
