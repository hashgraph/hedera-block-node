// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.suites.metrics;

import static org.hiero.block.suites.utils.BlockSimulatorUtils.createBlockSimulator;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;
import org.hiero.block.simulator.BlockStreamSimulatorApp;
import org.hiero.block.suites.BaseSuite;
import org.hiero.block.suites.BlockNodeContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

///
/// Regression guard for issue #3026: memory usage must stay within the nano profile's container
/// limit when the block node is deployed with it (`charts/block-node-server/values-overrides/nano.yaml`).
///
/// The nano profile targets memory-constrained environments (minikube, laptops). Its key overrides:
/// - `SERVER_SOCKET_RECEIVE_BUFFER_SIZE_BYTES=131072` (128 KB, vs the 8 MB production default)
/// - `JAVA_OPTS` with `-Xmx24m -XX:MaxDirectMemorySize=8m -XX:MaxMetaspaceSize=32m`
/// - Reduced messaging queue sizes
///
/// The streamed blocks carry TSS proofs, so the node loads the TSS library's WRAPS verification
/// parameters on the first block (about 200 MB off heap, plus verifier working memory): with
/// hedera-cryptography 3.18.0 the measured peak RSS under this load is 550 to 620 MB, which is the
/// floor the threshold is set above. If anyone removes or raises the socket buffer override from
/// nano.yaml (about 640 MB more), or increases the JVM heap, the test will fail.
///
@DisplayName("Memory Regression Tests")
public class MemoryRegressionTests extends BaseSuite {

    /// gRPC port for the nano-profile container. Must not conflict with the default (40840).
    private static final int NANO_PORT = 40841;

    /// Metrics port for the nano-profile container.
    private static final int NANO_METRICS_PORT = 16008;

    /// RSS threshold (KB) for the nano profile. Set to 800 MB: the measured peak with the TSS
    /// library's verification parameters loaded is 550 to 620 MB (see the class comment), and the
    /// threshold still catches regressions such as reverting the 8 MB socket buffer (about 640 MB
    /// more) or a larger heap.
    private static final long NANO_RSS_THRESHOLD_KB = 819_200L; // 800 MB

    /// Environment variable overrides mirroring `values-overrides/nano.yaml`.
    private static final Map<String, String> NANO_ENV = Map.of(
            "JAVA_OPTS",
            "-Xmx24m -XX:MaxDirectMemorySize=8m -XX:MaxMetaspaceSize=32m"
                    + " -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof",
            "SERVER_SOCKET_RECEIVE_BUFFER_SIZE_BYTES",
            "131072",
            "MESSAGING_BLOCK_ITEM_QUEUE_SIZE",
            "128",
            "MESSAGING_BLOCK_NOTIFICATION_QUEUE_SIZE",
            "16",
            // The TSS library holds about 200 MB of WRAPS verification parameters off heap once loaded;
            // the nano profile defers that to the first TSS block instead of loading at start.
            "VERIFICATION_TSS_WARM_UP_ENABLED",
            "false");

    private Future<?> simulatorFuture;

    /// Default constructor.
    public MemoryRegressionTests() {}

    @AfterEach
    void stopSimulatorAndContainers() throws InterruptedException {
        if (simulatorFuture != null) {
            simulatorFuture.cancel(true);
        }
        teardownBlockNodes();
    }

    @Test
    @DisplayName("Nano profile RSS stays under 800 MB (issue #3026 regression guard)")
    void nanoProfileStaysUnder800MbRss() throws IOException, InterruptedException {
        BlockNodeContainerConfig nanoConfig = new BlockNodeContainerConfig(NANO_PORT, NANO_METRICS_PORT, "", NANO_ENV);
        GenericContainer<?> nanoContainer = createContainer(nanoConfig);
        nanoContainer.start();
        blockNodeContainers.add(nanoContainer);

        BlockStreamSimulatorApp simulatorApp = createBlockSimulator(
                Map.of("grpc.port", String.valueOf(NANO_PORT), "generator.endBlockNumber", "9999"));
        simulatorFuture = startSimulatorInThread(simulatorApp);
        Thread.sleep(30_000);
        simulatorApp.stop();
        Thread.sleep(2_000);

        long rssKb = readMaxVmRssKb(nanoContainer);
        System.out.printf(
                "Nano profile RSS: %d KB / %.1f MB (threshold: %d KB / %.0f MB)%n",
                rssKb, rssKb / 1024.0, NANO_RSS_THRESHOLD_KB, NANO_RSS_THRESHOLD_KB / 1024.0);

        assertTrue(
                rssKb < NANO_RSS_THRESHOLD_KB,
                ("Nano profile RSS (%d KB / %.0f MB) exceeded the 800 MB threshold. "
                                + "Check values-overrides/nano.yaml — SERVER_SOCKET_RECEIVE_BUFFER_SIZE_BYTES "
                                + "must be 131072 (128 KB) and JAVA_OPTS must constrain heap and direct memory.")
                        .formatted(rssKb, rssKb / 1024.0));
    }

    /// Reads the maximum VmRSS (KB) across all processes in the container by scanning `/proc`.
    /// The JVM process is the largest consumer and will dominate the max.
    private static long readMaxVmRssKb(GenericContainer<?> container) throws IOException, InterruptedException {
        Container.ExecResult result = execInContainerWithRetry(
                container, "sh", "-c", "awk '/^VmRSS/{if($2>m)m=$2}END{print (m+0)}' /proc/[0-9]*/status 2>/dev/null");
        String out = result.getStdout().strip();
        if (out.isEmpty() || "0".equals(out)) {
            throw new IOException("Could not read VmRSS from container (exit=%d stderr=%s)."
                    .formatted(result.getExitCode(), result.getStderr()));
        }
        return Long.parseLong(out);
    }
}
