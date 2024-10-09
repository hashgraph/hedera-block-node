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

package com.hedera.block.suites.persistance;

import com.hedera.block.suites.persistance.positive.PositiveDataPersistenceTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for running data persistence tests, including both positive and negative test
 * scenarios.
 *
 * <p>This suite esaggregates the tts from {@link PositiveDataPersistenceTests}. The {@code @Suite}
 * annotation allows running all selected classes in a single test run.
 */
@Suite
@SelectClasses({PositiveDataPersistenceTests.class})
public class DataPersistenceTestSuites {

    /**
     * Default constructor for the {@link DataPersistenceTestSuites} class. This constructor is
     * empty as it does not need to perform any initialization.
     */
    public DataPersistenceTestSuites() {}
}
