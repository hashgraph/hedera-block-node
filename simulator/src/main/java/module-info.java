import com.hedera.block.simulator.config.SimulatorConfigExtension;

/** Runtime module of the simulator. */
module com.hedera.block.simulator {
    exports com.hedera.block.simulator.config.data;

    requires static com.github.spotbugs.annotations;
    requires static com.google.auto.service;
    requires com.hedera.block.stream;
    requires com.google.protobuf;
    requires com.hedera.pbj.runtime;
    requires com.swirlds.config.api;
    requires com.swirlds.config.extensions;
    requires dagger;
    requires io.grpc.stub;
    requires io.grpc;
    requires javax.inject;

    provides com.swirlds.config.api.ConfigurationExtension with
            SimulatorConfigExtension;
}
