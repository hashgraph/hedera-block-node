import com.hedera.block.simulator.config.SimulatorConfigExtension;

module com.hedera.block.simulator {
    exports com.hedera.block.simulator.config.data to
            com.swirlds.config.impl,
            com.swirlds.config.extensions;

    requires static com.github.spotbugs.annotations;
    requires static com.google.auto.service;
    requires com.hedera.block.stream;
    // requires com.hedera.pbj.runtime; // leaving it here since it will be needed soon.
    requires com.swirlds.config.api;
    requires com.swirlds.config.extensions;
    requires dagger;
    requires javax.inject;

    provides com.swirlds.config.api.ConfigurationExtension with
            SimulatorConfigExtension;
}
