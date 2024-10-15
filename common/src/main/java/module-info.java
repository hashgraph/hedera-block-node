module com.hedera.block.common {
    exports com.hedera.block.common.constants;

    requires com.hedera.block.stream;
    requires com.google.protobuf;
    requires com.hedera.pbj.runtime;
    requires static com.github.spotbugs.annotations;
}
