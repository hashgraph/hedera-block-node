/*
 * Copyright (C) 2023 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("org.hiero.gradle.base.lifecycle")
    id("org.hiero.gradle.base.jpms-modules")
}

dependencies.constraints {
    // Define a constant for the platform SDK version.
    // Platform SDK modules are all released together with matching versions.
    val swirldsVersion = "0.51.5"
    val daggerVersion = "2.42"
    val grpcVersion = "1.65.1"

    api("com.github.spotbugs:spotbugs-annotations:4.7.3") { because("com.github.spotbugs.annotations") }
    api("com.google.auto.service:auto-service-annotations:1.1.1") { because("com.google.auto.service") }
    api("com.google.guava:guava:33.3.1-jre") { because("com.google.common") }
    api("com.google.j2objc:j2objc-annotations:3.0.0") {  because("com.google.j2objc.annotations") }
    api("com.google.protobuf:protobuf-java-util:4.28.2") { because("com.google.protobuf.util") }
    api("com.google.protobuf:protoc:4.28.2") { because("google.proto") }
    api("com.hedera.pbj:pbj-runtime:0.9.2") { because("com.hedera.pbj.runtime") }
    api("com.lmax:disruptor:4.0.0") { because("com.lmax.disruptor") }
    api("com.swirlds:swirlds-common:$swirldsVersion") { because("com.swirlds.common") }
    api("com.swirlds:swirlds-config-impl:$swirldsVersion") { because("com.swirlds.config.impl") }
    api("io.helidon.logging:helidon-logging-jul:4.1.0") { because("io.helidon.logging.jul") }
    api("io.helidon.webserver:helidon-webserver-grpc:4.1.0") { because("io.helidon.webserver.grpc") }
    api("io.helidon.webserver:helidon-webserver:4.1.0") { because("io.helidon.webserver") }

    // gRPC dependencies
    api("io.grpc:grpc-api:$grpcVersion") { because("io.grpc") }
    api("io.grpc:grpc-stub:$grpcVersion") { because("io.grpc.stub") }
    api("io.grpc:grpc-protobuf:$grpcVersion") { because("io.grpc.protobuf") }
    api("io.grpc:grpc-netty:$grpcVersion") { because("io.grpc.netty") }
    api("io.grpc:protoc-gen-grpc-java:1.66.0") { because("XXXXX") }

    // needed for dagger
    api("com.google.dagger:dagger:$daggerVersion") { because("dagger") }
    api("com.google.dagger:dagger-compiler:$daggerVersion") { because("dagger.compiler") }
    api("com.squareup:javapoet:1.13.0") { because("com.squareup.javapoet") }

    // Testing only versions
    api("org.junit.jupiter:junit-jupiter-api:5.10.2") { because("org.junit.jupiter.api") }
    api("org.mockito:mockito-core:5.8.0") { because("org.mockito") }
    api("org.mockito:mockito-junit-jupiter:5.8.0") { because("org.mockito.junit.jupiter") }
    api("org.testcontainers:testcontainers:1.20.1") { because("org.testcontainers") }
    api("org.testcontainers:junit-jupiter:1.20.1") { because("org.testcontainers.junit.jupiter") }
    api("com.github.docker-java:docker-java-api:3.4.0") { because("com.github.dockerjava.api") }
    api("io.github.cdimascio:dotenv-java:3.0.2") { because("io.github.cdimascio.dotenv.java") }

    api("com.google.auto.service:auto-service:1.1.1") { because("com.google.auto.service.processor") }
    api("com.google.dagger:dagger-compiler:$daggerVersion") { because("dagger.compiler") }
}


