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

package com.hedera.block.simulator.metrics;

import com.swirlds.metrics.api.Counter;
import edu.umd.cs.findbugs.annotations.NonNull;

/** Use member variables of this class to update metric data for the Hedera Block Node. */
public interface MetricsService {
    /**
     * Use this method to get a specific counter for the given metric type.
     *
     * @param key to get a specific counter
     * @return the counter
     */
    Counter get(@NonNull SimulatorMetricTypes.Counter key);
}
