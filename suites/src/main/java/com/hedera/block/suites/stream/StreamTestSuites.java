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

package com.hedera.block.suites.stream;

import com.hedera.block.suites.stream.negative.NegativeBlockStreamTests;
import com.hedera.block.suites.stream.positive.PositiveBlockStreamTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * StreamTestSuites is a test suite that aggregates and runs a set of test classes related to the
 * stream processing functionality of the Block Node.
 *
 * <p>This suite includes both positive and negative test cases.
 *
 * <p>The suite uses the {@link org.junit.platform.suite.api.Suite} annotation to indicate that this
 * class is an entry point for running multiple test classes together. It selects the following test
 * classes:
 *
 * <ul>
 *   <li>{@link com.hedera.block.suites.stream.positive.PositiveBlockStreamTests}
 *   <li>{@link com.hedera.block.suites.stream.negative.NegativeBlockStreamTests}
 * </ul>
 */
@Suite
@SelectClasses({PositiveBlockStreamTests.class, NegativeBlockStreamTests.class})
public class StreamTestSuites {
    /**
     * Default constructor for the StreamTestSuites class.
     *
     * <p>This constructor is required by the JUnit framework to run the suite.
     */
    public StreamTestSuites() {
        // No additional setup required
    }
}
