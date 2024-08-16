import com.hedera.block.server.config.BlockNodeConfigExtension;

/** Runtime module of the server. */
module com.hedera.block.server {
//    exports com.hedera.block.server.consumer to
//        com.swirlds.config.impl;
//    exports com.hedera.block.server.persistence.storage to
//        com.swirlds.config.impl;

//    exports java.util.logging to io.grpc;

    exports com.hedera.block.server;
    exports com.hedera.block.server.consumer;
    exports com.hedera.block.server.persistence.storage;
    exports com.hedera.block.server.persistence.storage.write;
    exports com.hedera.block.server.persistence.storage.read;
    exports com.hedera.block.server.persistence.storage.remove;
    exports com.hedera.block.server.config;
    exports com.hedera.block.server.mediator;
    exports com.hedera.block.server.data;

    requires transitive com.hedera.block.stream;
    requires com.google.protobuf;
    requires com.hedera.pbj.runtime;
    requires com.lmax.disruptor;
    requires com.swirlds.common;
    requires com.swirlds.config.api;
    requires com.swirlds.config.extensions;
    requires com.swirlds.metrics.api;

//      Please add the following requires directives:
//    requires io.grpc.stub;
//    requires transitive io.grpc.stub;
    requires io.grpc.stub;
    requires io.helidon.common;
    requires io.helidon.webserver.grpc;
    requires io.helidon.webserver;
    requires static com.github.spotbugs.annotations;
    requires static com.google.auto.service;
//    requires java.logging;
    requires java.logging;

    provides com.swirlds.config.api.ConfigurationExtension with
            BlockNodeConfigExtension;
}
