/** Runtime module of block stream tools. */
module com.hedera.block.tools {
    exports com.hedera.block.tools;

    opens com.hedera.block.tools to
            info.picocli;
    opens com.hedera.block.tools.commands to
            info.picocli;

    requires static com.github.spotbugs.annotations;
    requires static com.google.auto.service;
    requires com.hedera.block.stream;
    requires com.hedera.pbj.runtime;
    requires info.picocli;
}
