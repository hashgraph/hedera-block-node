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

package com.hedera.block.suites.grpc;

import com.hedera.block.suites.grpc.negative.NegativeServerAvailabilityTests;
import com.hedera.block.suites.grpc.positive.PositiveServerAvailabilityTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for running gRPC server availability tests, including both positive and negative test
 * scenarios.
 *
 * <p>This suite aggregates the tests from {@link PositiveServerAvailabilityTests} and {@link
 * NegativeServerAvailabilityTests}. The {@code @Suite} annotation allows running all selected
 * classes in a single test run.
 */
@Suite
@SelectClasses({PositiveServerAvailabilityTests.class, NegativeServerAvailabilityTests.class})
public class GrpcTestSuites {

    /**
     * Default constructor for the {@link GrpcTestSuites} class. This constructor is empty as it
     * does not need to perform any initialization.
     */
    public GrpcTestSuites() {}
}
