// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.node.spi.throttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.hiero.metrics.LongCounter;
import org.hiero.metrics.core.MetricRegistry;
import org.hiero.metrics.core.MetricRegistrySnapshot;
import org.hiero.metrics.core.MetricSnapshot;
import org.hiero.metrics.core.MetricsExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Verifies that [ThrottleMetrics] binds its shared, once-registered metrics to distinct
/// `(service, method, weightClass, outcome)` combinations via labels on one counter
/// (`throttle_calls`), instead of a separately-named counter per outcome, and that the
/// client-state-count gauge keeps its own narrower label set. See
/// `docs/design/apis/api-throttling.md`'s Metrics section and PR #3637's review thread 2.
class ThrottleMetricsTest {

    private ThrottleMetrics throttleMetrics;

    @BeforeEach
    void setUp() {
        final MetricRegistry metricRegistry = MetricRegistry.builder()
                .setMetricsExporter(new NoOpMetricsExporter())
                .build();
        throttleMetrics = new ThrottleMetrics(metricRegistry);
    }

    @Test
    @DisplayName("Two different services get independent counts under the one shared counter")
    void differentServicesGetIndependentCounts() {
        final LongCounter.Measurement blockAccess = throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.HEAVY, ThrottleMetrics.Outcome.ADMITTED);
        final LongCounter.Measurement serverStatus = throttleMetrics.recordCall(
                "BlockNodeService", "serverStatus", WeightClass.STANDARD, ThrottleMetrics.Outcome.ADMITTED);

        assertNotSame(
                blockAccess,
                serverStatus,
                "different (service, method, weightClass, outcome) combinations must not share a measurement");

        throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.HEAVY, ThrottleMetrics.Outcome.ADMITTED);

        assertEquals(2, blockAccess.get());
        assertEquals(1, serverStatus.get(), "incrementing one service's count must not affect another's");
    }

    @Test
    @DisplayName("Two methods on the same service/weightClass (e.g. BlockNodeService) get independent counts")
    void differentMethodsOfSameServiceGetIndependentCounts() {
        // Mirrors BlockNodeService's serverStatus and serverStatusDetail methods, each with their
        // own SingleWeightThrottle instance (own rate bucket, own concurrency ceiling) — still
        // worth covering explicitly, since method is part of this counter's label set.
        final LongCounter.Measurement serverStatus = throttleMetrics.recordCall(
                "BlockNodeService", "serverStatus", WeightClass.STANDARD, ThrottleMetrics.Outcome.ADMITTED);
        final LongCounter.Measurement serverStatusDetail = throttleMetrics.recordCall(
                "BlockNodeService", "serverStatusDetail", WeightClass.STANDARD, ThrottleMetrics.Outcome.ADMITTED);

        assertNotSame(serverStatus, serverStatusDetail);
        assertEquals(1, serverStatus.get());
        assertEquals(1, serverStatusDetail.get(), "an admitted call for one method must not count against another");
    }

    @Test
    @DisplayName("Different outcomes for the same (service, method, weightClass) get independent counts")
    void differentOutcomesGetIndependentCounts() {
        final LongCounter.Measurement admitted = throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.STANDARD, ThrottleMetrics.Outcome.ADMITTED);
        final LongCounter.Measurement rejectedRate = throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.STANDARD, ThrottleMetrics.Outcome.REJECTED_RATE);

        assertNotSame(admitted, rejectedRate, "outcome is part of the label set, not folded into one count");

        throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.STANDARD, ThrottleMetrics.Outcome.REJECTED_RATE);

        assertEquals(1, admitted.get());
        assertEquals(2, rejectedRate.get());
    }

    @Test
    @DisplayName("Two weight classes of the same service/method get independent counts")
    void differentWeightClassesOfSameMethodGetIndependentCounts() {
        final LongCounter.Measurement live = throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.STANDARD, ThrottleMetrics.Outcome.REJECTED_RATE);
        final LongCounter.Measurement historical = throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.HEAVY, ThrottleMetrics.Outcome.REJECTED_RATE);

        assertNotSame(live, historical);
        assertEquals(1, live.get());
        assertEquals(1, historical.get(), "a rejection in one weight class must not count in another");
    }

    @Test
    @DisplayName(
            "Asking for the same (service, method, weightClass, outcome) combination again reuses the same measurement")
    void sameCombinationReusesTheSameMeasurement() {
        final LongCounter.Measurement first = throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.STANDARD, ThrottleMetrics.Outcome.ADMITTED);
        final LongCounter.Measurement second = throttleMetrics.recordCall(
                "BlockAccessService", "getBlock", WeightClass.STANDARD, ThrottleMetrics.Outcome.ADMITTED);

        assertSame(
                first,
                second,
                "requesting the same labels twice must not attempt to register a duplicate metric, and must "
                        + "share state rather than silently tracking two independent counts");
        assertEquals(2, first.get());
    }

    @Test
    @DisplayName("The client-state-count gauge is labeled by service/method/weightClass, with no outcome dimension")
    void clientStateGaugeHasMethodButNoOutcomeLabel() {
        // Two different methods at the same (service, weightClass) must not collide: each
        // SingleWeightThrottle instance now registers its own gauge observer.
        throttleMetrics
                .gaugeFor("BlockNodeService", "serverStatus", WeightClass.STANDARD)
                .accept(() -> 0L);
        throttleMetrics
                .gaugeFor("BlockNodeService", "serverStatusDetail", WeightClass.STANDARD)
                .accept(() -> 0L);

        // But a second observer for the exact same (service, method, weightClass) must collide,
        // since SingleWeightThrottle only ever registers one per instance.
        assertThrows(IllegalArgumentException.class, () -> throttleMetrics
                .gaugeFor("BlockNodeService", "serverStatus", WeightClass.STANDARD)
                .accept(() -> 0L));
    }

    @Test
    @DisplayName("The calls counter and client-state gauge are registered under their documented metric names")
    void registersMetricsUnderDocumentedNames() {
        // The exported name a scraper sees can differ from the registered name (the exporter
        // appends _total to every counter itself, per OpenMetrics convention, verified separately
        // in BlockNodeThrottleTests' e2e scrape assertion) — this only verifies the name
        // ThrottleMetrics itself registers, which is what a future accidental rename would change.
        final CapturingMetricsExporter exporter = new CapturingMetricsExporter();
        final MetricRegistry metricRegistry =
                MetricRegistry.builder().setMetricsExporter(exporter).build();
        new ThrottleMetrics(metricRegistry);

        final Set<String> registeredNames = new HashSet<>();
        for (final MetricSnapshot snapshot : exporter.snapshot()) {
            registeredNames.add(snapshot.name());
        }

        // BlockNodePlugin.METRICS_CATEGORY ("blocknode") is prefixed onto every metric name here
        // via addCategory, so the registered name is category_name, not the bare name.
        assertTrue(
                registeredNames.contains("blocknode_throttle_calls"),
                "the calls counter must be named blocknode_throttle_calls, found: " + registeredNames);
        assertTrue(
                registeredNames.contains("blocknode_throttle_client_state"),
                "the client-state gauge must be named blocknode_throttle_client_state, found: " + registeredNames);
    }

    /// A no-op metrics exporter so tests don't need a real metrics backend.
    private static final class NoOpMetricsExporter implements MetricsExporter {
        @Override
        public void setSnapshotSupplier(final Supplier<MetricRegistrySnapshot> supplier) {}

        @Override
        public void close() {}
    }

    /// Captures the registry's snapshot supplier so a test can inspect registered metric names.
    private static final class CapturingMetricsExporter implements MetricsExporter {
        private Supplier<MetricRegistrySnapshot> supplier;

        MetricRegistrySnapshot snapshot() {
            return supplier.get();
        }

        @Override
        public void setSnapshotSupplier(final Supplier<MetricRegistrySnapshot> supplier) {
            this.supplier = supplier;
        }

        @Override
        public void close() {}
    }
}
