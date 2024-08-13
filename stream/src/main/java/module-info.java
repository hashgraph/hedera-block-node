module com.hedera.block.stream {
    exports com.hedera.hapi.block;
    exports com.hedera.hapi.block.stream;
    exports com.hedera.hapi.block.stream.input;
    exports com.hedera.hapi.block.stream.output;

    requires com.google.common;
    requires transitive com.google.protobuf;
    requires com.hedera.pbj.runtime;
    requires io.grpc.stub;
    requires transitive io.grpc;
    requires io.grpc.protobuf;
    requires org.antlr.antlr4.runtime;
    requires static com.github.spotbugs.annotations;
//    requires static java.annotation;
//    requires static java.compiler;
}
