/** Runtime module of the server. */
module com.hedera.block.server {
    requires com.hedera.block.protos;
    requires com.google.protobuf;
    requires io.grpc.stub;
    requires io.helidon.common;
    requires io.helidon.webserver.grpc;
    requires io.helidon.webserver;
}
