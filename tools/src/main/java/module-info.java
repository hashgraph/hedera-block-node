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

    requires com.google.cloud.storage;
    requires com.hedera.block.common;
    requires com.github.luben.zstd_jni;

//    implementation(platform("com.google.cloud:libraries-bom:26.49.0"))
//            implementation("com.google.cloud:google-cloud-storage")
//            implementation("com.hedera.hashgraph:hapi:0.56.4")
//            implementation("com.github.luben:zstd-jni:1.5.6-6")
}
