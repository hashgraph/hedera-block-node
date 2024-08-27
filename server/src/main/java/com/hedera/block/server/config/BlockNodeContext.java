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

package com.hedera.block.server.config;

import com.hedera.block.server.metrics.MetricsService;
import com.swirlds.config.api.Configuration;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Context for the block node. This record is returned by the BlockNodeContextFactory when a new
 * configuration is created.
 *
 * @param metricsService the service responsible for handling metrics
 * @param configuration the configuration settings for the block node
 */
public record BlockNodeContext(
        @NonNull MetricsService metricsService, @NonNull Configuration configuration) {}
