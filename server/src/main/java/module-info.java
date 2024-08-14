import com.hedera.block.server.config.BlockNodeConfigExtension;

/** Runtime module of the server. */
module com.hedera.block.server {
    exports com.hedera.block.server.consumer to
            com.swirlds.config.impl;
    exports com.hedera.block.server.persistence.storage to
            com.swirlds.config.impl;

    requires com.hedera.block.protos;
    requires com.google.protobuf;
    requires com.lmax.disruptor;
    requires com.swirlds.common;
    requires com.swirlds.config.api;
    requires com.swirlds.config.extensions;
    requires com.swirlds.metrics.api;
    requires io.grpc.stub;
    requires io.helidon.common;
    requires io.helidon.webserver.grpc;
    requires io.helidon.webserver;
    requires static com.github.spotbugs.annotations;
    requires static com.google.auto.service;

    provides com.swirlds.config.api.ConfigurationExtension with
            BlockNodeConfigExtension;
}
