// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.node.roster.bootstrap.tss;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static org.hiero.block.node.base.ParseHelper.standardParse;

import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.hiero.block.api.TssData;
import org.hiero.block.internal.BlockNodeSource;
import org.hiero.block.node.spi.ApplicationStateFacility;
import org.hiero.block.node.spi.BlockNodeContext;
import org.hiero.block.node.spi.BlockNodePlugin;
import org.hiero.block.node.spi.ServiceBuilder;
import org.hiero.metrics.LongCounter;
import org.hiero.metrics.ObservableGauge;
import org.hiero.metrics.core.MetricKey;
import org.hiero.metrics.core.MetricRegistry;

/// A block node plugin that tries to get the latest TssData and makes it available to the `BlockNodeApp` and
/// to the `ServerStatusServicePlugin`
///
/// The TssData is retrieved by querying the peer BlockNodes listed in the block node sources
/// file periodically. A peer's TssData is only installed when its ledger id has the length the
/// current TSS library verifies against; a peer that predates the library change answers with a
/// shorter ledger id, which is skipped.
public class RosterBootstrapTssPlugin implements BlockNodePlugin {
    /// Length in bytes of a ledger id: the hash of the genesis address book followed by the hash
    /// of the genesis hinTS verification key (the TSS library's network id). Mirrors the constant
    /// the verification plugin checks against.
    static final int LEDGER_ID_LENGTH = 64;

    public static final MetricKey<ObservableGauge> METRIC_TSS_DATA_PEERS =
            MetricKey.of("tss_data_peers", ObservableGauge.class).addCategory(METRICS_CATEGORY);
    public static final MetricKey<LongCounter> METRIC_TSS_DATA_ERRORS =
            MetricKey.of("tss_data_errors", LongCounter.class).addCategory(METRICS_CATEGORY);
    public static final MetricKey<LongCounter> METRIC_TSS_DATA_REQUESTS =
            MetricKey.of("tss_data_requests", LongCounter.class).addCategory(METRICS_CATEGORY);

    /// The logger for this class.
    private static final System.Logger LOGGER = System.getLogger(RosterBootstrapTssPlugin.class.getName());

    /// The block node context, for access to core facilities.
    private volatile BlockNodeContext blockNodeContext;
    /// The application state facility, for updating application state.
    private ApplicationStateFacility applicationStateFacility;

    private boolean hasBNSourcesPath = false;
    /// The ScheduledExecutorService used by the RosterBootstrapPlugin to query peer BNs for TssData
    private ScheduledExecutorService queryPeerExecutor;
    /// The config information for the RosterBootstrapTssConfig
    private RosterBootstrapTssConfig rosterBootstrapTssConfig;
    // Metrics holder containing all TssData metrics
    private MetricsHolder metricsHolder;
    // The class that fetches the TssData
    private TssDataFetcher tssDataFetcher;
    // State touched by multiple threads
    private final AtomicLong currentBlockNodePeers = new AtomicLong(0);

    /// {@inheritDoc}
    @Override
    public void init(BlockNodeContext context, ServiceBuilder serviceBuilder) {
        this.applicationStateFacility = Objects.requireNonNull(context.applicationStateFacility());
        rosterBootstrapTssConfig = context.configuration().getConfigData(RosterBootstrapTssConfig.class);
        this.blockNodeContext = context;

        initMetrics();

        BlockNodeSource blockNodeSources = getBlockNodeSource(rosterBootstrapTssConfig);

        if (blockNodeSources != null) {
            currentBlockNodePeers.set(blockNodeSources.nodes().size());
            hasBNSourcesPath = true;
            tssDataFetcher = new TssDataFetcher(blockNodeSources, rosterBootstrapTssConfig, metricsHolder);
        }
    }

    /// Get the BlockNodeSource
    /// @param config {@link RosterBootstrapTssConfig} configuration information for the plugin.
    /// @return The {@link BlockNodeSource} or null if it cannot be determined.
    private BlockNodeSource getBlockNodeSource(RosterBootstrapTssConfig config) {
        final String sourcesPath = config.blockNodeSourcesPath();
        if (sourcesPath != null && !sourcesPath.isBlank()) {
            Path blockNodeSourcesPath = Path.of(sourcesPath);
            if (Files.isRegularFile(blockNodeSourcesPath)) {
                try {
                    return standardParse(BlockNodeSource.JSON, Bytes.wrap(Files.readAllBytes(blockNodeSourcesPath)));
                } catch (ParseException | IOException e) {
                    // do nothing
                }
            }
        }
        final String parseFailedMsg =
                "Failed to read/parse block node sources from path: [%s], TssBootstrapPlugin will not query any peers"
                        .formatted(sourcesPath);
        LOGGER.log(WARNING, parseFailedMsg);
        return null;
    }

    /// {@inheritDoc}
    @Override
    public void stop() {
        shutdownExecutor();
        closeFetcherResources();
    }

    /// close tssDataFetcher resources
    private void closeFetcherResources() {
        if (tssDataFetcher != null) {
            try {
                tssDataFetcher.close();
            } catch (IOException e) {
                // Cleanup failure during shutdown is not on-call actionable; keep at DEBUG.
                LOGGER.log(DEBUG, "Unable to close tssDataFetcher", e);
            }
        }
    }

    /// Shutdown the executor
    private void shutdownExecutor() {
        if (queryPeerExecutor != null) {
            queryPeerExecutor.shutdown();
            try {
                if (!queryPeerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    final String executorTerminationMsg =
                            "queryPeerExecutor did not terminate in time, calling shutdownNow()";
                    queryPeerExecutor.shutdownNow();
                    LOGGER.log(INFO, executorTerminationMsg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /// UncaughtExceptionHandler for logging uncaught exceptions
    private void uncaughtExceptionHandler(Thread thread, Throwable throwable) {
        LOGGER.log(
                WARNING, "Uncaught exception in RosterBootstrapTssPlugin thread {0}: {1}", thread.getName(), throwable);
    }

    /// {@inheritDoc}
    @Override
    public void start() {
        // Don't start the querying thread if there are no peers defined.
        if (!hasBNSourcesPath) {
            LOGGER.log(WARNING, "RosterBootstrapTssPlugin: No BN Peers defined");
            return;
        }
        // save the reference of this volatile object.
        BlockNodeContext context = blockNodeContext;
        LOGGER.log(DEBUG, "RosterBootstrapTssPlugin start called");

        // Create thread executors via threadPoolManager.
        queryPeerExecutor = context.threadPoolManager()
                .createVirtualThreadScheduledExecutor(1, "queryPeerScanner", this::uncaughtExceptionHandler);

        // Schedule periodic checking of bn peers for their TssData
        // Should start immediately, initialDelay=0, as this is during plugin startup
        queryPeerExecutor.scheduleAtFixedRate(
                this::queryPeerTssData, 0, rosterBootstrapTssConfig.queryPeerInterval(), TimeUnit.MILLISECONDS);
    }

    /// Queries peer BlockNodes for their TssData and installs every datum whose ledger id has the
    /// length the current TSS library verifies against; any other length is skipped.
    private void queryPeerTssData() {
        final List<TssData> tssDataList = tssDataFetcher.getTssData();
        for (final TssData tssData : tssDataList) {
            final Bytes ledgerId = tssData.ledgerId();
            final long ledgerIdLength = ledgerId == null ? 0L : ledgerId.length();
            if (ledgerIdLength == LEDGER_ID_LENGTH) {
                applicationStateFacility.updateTssData(tssData);
            } else {
                LOGGER.log(
                        DEBUG,
                        "Skipping peer TSS data valid from block {0} with a ledger id of {1} bytes, expected {2}",
                        tssData.validFromBlock(),
                        ledgerIdLength,
                        LEDGER_ID_LENGTH);
            }
        }
    }

    /// Initializes the metrics for querying peer BlockNodes for TssData.
    private void initMetrics() {
        metricsHolder = MetricsHolder.createMetrics(blockNodeContext.metricRegistry(), currentBlockNodePeers);
    }

    /// Holder for all TssData-related metrics.
    /// This record groups all metrics used by the roster bootstrap TSS plugin and its components,
    /// allowing them to be passed as a single parameter.
    public record MetricsHolder(LongCounter.Measurement tssDataRequests, LongCounter.Measurement tssDataErrors) {

        /// Factory method to create a MetricsHolder with all metrics registered.
        ///
        /// @param metricRegistry the metrics registry instance to register metrics with
        /// @return a new MetricsHolder with all metrics created
        public static MetricsHolder createMetrics(
                @NonNull final MetricRegistry metricRegistry, @NonNull final AtomicLong currentBlockNodePeers) {
            metricRegistry.register(ObservableGauge.builder(METRIC_TSS_DATA_PEERS)
                    .setDescription("Current number of block node peers")
                    .observe(() -> Math.max(currentBlockNodePeers.get(), 0)));

            return new MetricsHolder(
                    metricRegistry
                            .register(LongCounter.builder(METRIC_TSS_DATA_REQUESTS)
                                    .setDescription("Number of TssData requests."))
                            .getOrCreateNotLabeled(),
                    metricRegistry
                            .register(LongCounter.builder(METRIC_TSS_DATA_ERRORS)
                                    .setDescription("Number of TssData request errors."))
                            .getOrCreateNotLabeled());
        }
    }
}
