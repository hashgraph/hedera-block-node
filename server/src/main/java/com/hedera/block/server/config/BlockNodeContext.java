// SPDX-License-Identifier: Apache-2.0
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
public record BlockNodeContext(@NonNull MetricsService metricsService, @NonNull Configuration configuration) {}
