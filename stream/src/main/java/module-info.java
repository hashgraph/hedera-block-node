module com.hedera.block.stream {
    exports com.hedera.hapi.block;
    exports com.hedera.hapi.block.stream;
    exports com.hedera.hapi.block.stream.input;
    exports com.hedera.hapi.block.stream.output;

    requires transitive com.google.common;
    requires transitive com.google.protobuf;
    requires transitive com.hedera.pbj.runtime;
    requires transitive io.grpc.stub;
    requires transitive io.grpc;
    requires io.grpc.protobuf;
    requires org.antlr.antlr4.runtime;
    requires static com.github.spotbugs.annotations;
    requires static java.annotation;
}
