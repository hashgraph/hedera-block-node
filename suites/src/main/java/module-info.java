/** Runtime module of the suites. */
module com.hedera.block.node.suites {
    requires com.hedera.block.simulator;

    // Require testing libraries
    requires io.github.cdimascio;
    requires org.junit.jupiter.api;
    requires org.junit.platform.suite.api;
    requires org.testcontainers;

    exports com.hedera.block.suites to
            org.junit.platform.commons;
}
