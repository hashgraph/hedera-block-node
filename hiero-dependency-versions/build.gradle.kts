// SPDX-License-Identifier: Apache-2.0
plugins {
    id("org.hiero.gradle.base.lifecycle")
    id("org.hiero.gradle.base.jpms-modules")
    id("org.hiero.gradle.check.spotless")
    id("org.hiero.gradle.check.spotless-kotlin")
}

dependencies { api(platform("com.google.cloud:libraries-bom:26.49.0")) }

dependencies.constraints {
    val daggerVersion = "2.55"
    val grpcIoVersion = "1.69.0"
    val helidonVersion = "4.1.6"
    val pbjVersion = "0.9.12"
    val protobufVersion = "4.28.2"
    val swirldsVersion = "0.58.1"

    api("com.github.luben:zstd-jni:1.5.6-8") { because("com.github.luben.zstd_jni") }
    api("com.github.spotbugs:spotbugs-annotations:4.7.3") {
        because("com.github.spotbugs.annotations")
    }
    api("com.google.auto.service:auto-service-annotations:1.1.1") {
        because("com.google.auto.service")
    }
    api("com.google.guava:guava:33.3.1-jre") { because("com.google.common") }
    api("com.google.j2objc:j2objc-annotations:3.0.0") { because("com.google.j2objc.annotations") }
    api("com.google.protobuf:protobuf-java-util:$protobufVersion") {
        because("com.google.protobuf.util")
    }
    api("com.google.protobuf:protoc:$protobufVersion") { because("google.proto") }
    api("com.hedera.pbj:pbj-grpc-helidon:${pbjVersion}") { because("com.hedera.pbj.grpc.helidon") }
    api("com.hedera.pbj:pbj-grpc-helidon-config:${pbjVersion}") {
        because("com.hedera.pbj.grpc.helidon.config")
    }
    api("com.hedera.pbj:pbj-runtime:${pbjVersion}") { because("com.hedera.pbj.runtime") }
    api("com.lmax:disruptor:4.0.0") { because("com.lmax.disruptor") }
    api("com.swirlds:swirlds-common:$swirldsVersion") { because("com.swirlds.common") }
    api("com.swirlds:swirlds-config-impl:$swirldsVersion") { because("com.swirlds.config.impl") }
    api("io.helidon.logging:helidon-logging-jul:$helidonVersion") {
        because("io.helidon.logging.jul")
    }
    api("io.helidon.webserver:helidon-webserver-grpc:$helidonVersion") {
        because("io.helidon.webserver.grpc")
    }
    api("io.helidon.webserver:helidon-webserver:$helidonVersion") {
        because("io.helidon.webserver")
    }
    api("org.jetbrains:annotations:26.0.1") { because("org.jetbrains.annotations") }

    // gRPC dependencies
    api("io.grpc:grpc-api:$grpcIoVersion") { because("io.grpc") }
    api("io.grpc:grpc-stub:$grpcIoVersion") { because("io.grpc.stub") }
    api("io.grpc:grpc-protobuf:$grpcIoVersion") { because("io.grpc.protobuf") }
    api("io.grpc:grpc-netty:$grpcIoVersion") { because("io.grpc.netty") }
    api("io.grpc:protoc-gen-grpc-java:1.69.0")

    // command line tool
    api("info.picocli:picocli:4.7.6") { because("info.picocli") }

    // needed for dagger
    api("com.google.dagger:dagger:$daggerVersion") { because("dagger") }
    api("com.google.dagger:dagger-compiler:$daggerVersion") { because("dagger.compiler") }

    // Testing only versions
    api("com.github.docker-java:docker-java-api:3.4.0") { because("com.github.dockerjava.api") }
    api("io.github.cdimascio:dotenv-java:3.0.2") { because("io.github.cdimascio.dotenv.java") }
    api("org.assertj:assertj-core:3.23.1") { because("org.assertj.core") }
    api("org.junit.jupiter:junit-jupiter-api:5.10.2") { because("org.junit.jupiter.api") }
    api("org.mockito:mockito-core:5.15.2") { because("org.mockito") }
    api("org.mockito:mockito-junit-jupiter:5.8.0") { because("org.mockito.junit.jupiter") }
    api("org.testcontainers:junit-jupiter:1.20.1") { because("org.testcontainers.junit.jupiter") }
    api("org.testcontainers:testcontainers:1.20.1") { because("org.testcontainers") }

    api("com.google.auto.service:auto-service:1.1.1") {
        because("com.google.auto.service.processor")
    }
}
