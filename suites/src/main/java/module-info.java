module hedera.block.node.suites {
    requires com.hedera.block.simulator;

    // Require testing libraries
    requires io.github.cdimascio;
    requires org.junit.jupiter.api;
    requires org.junit.platform.suite.api;
    requires org.testcontainers;
}
