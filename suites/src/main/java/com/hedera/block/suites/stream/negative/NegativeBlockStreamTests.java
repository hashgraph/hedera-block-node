/*
 * Copyright (C) 2022-2024 Hedera Hashgraph, LLC
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

package com.hedera.block.suites.stream.negative;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.block.suites.BaseSuite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TBD */
@DisplayName("Negative Block Stream Tests")
public class NegativeBlockStreamTests extends BaseSuite {

    /**
     * Default constructor for the NegativeBlockStreamTests class.
     *
     * <p>This constructor is required by the testing framework (JUnit) to create instances of the
     * test class. It does not perform any additional setup.
     */
    public NegativeBlockStreamTests() {
        // No additional setup required
    }

    /** TBD */
    @Test
    public void testInvalidBlockStreamProcessing() {
        assertTrue(blockNodeContainer.isRunning());
    }
}
