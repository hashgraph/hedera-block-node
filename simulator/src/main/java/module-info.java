import com.hedera.block.simulator.config.SimulatorConfigExtension;

/** Runtime module of the simulator. */
module com.hedera.block.simulator {
    exports com.hedera.block.simulator.config.data;
    exports com.hedera.block.simulator.exception;
    exports com.hedera.block.simulator;

    requires com.hedera.block.stream;
    requires com.hedera.pbj.runtime;
    requires com.swirlds.config.api;
    requires com.swirlds.config.extensions;
    requires com.google.protobuf;
    requires dagger;
    requires io.grpc.stub;
    requires io.grpc;
    requires javax.inject;
    requires static com.github.spotbugs.annotations;
    requires static com.google.auto.service;
    requires static java.compiler; // javax.annotation.processing.Generated

    provides com.swirlds.config.api.ConfigurationExtension with
            SimulatorConfigExtension;
}
