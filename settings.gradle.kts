/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
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
    id("com.gradle.enterprise").version("3.15.1")
}

// Include the subprojects
include(":suites")
include(":stream")
include(":server")
include(":simulator")

includeBuild(".") // https://github.com/gradle/gradle/issues/21490#issuecomment-1458887481

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Define a constant for the platform SDK version.
            // Platform SDK modules are all released together with matching versions.
            val swirldsVersion = "0.51.5"

            // Define a constant for the Dagger version.
            val daggerVersion = "2.42"

            // Compile time dependencies
            version("io.helidon.webserver.http2", "4.1.0")
            version("io.helidon.webserver.grpc", "4.1.0")
            version("io.helidon.logging", "4.1.0")
            version("com.lmax.disruptor", "4.0.0")
            version("com.github.spotbugs.annotations", "4.7.3")
            version("com.swirlds.metrics.api", swirldsVersion)
            version("com.swirlds.metrics.impl", swirldsVersion)
            version("com.swirlds.common", swirldsVersion)
            version("com.swirlds.config.impl", swirldsVersion)
            version("com.swirlds.config.processor", swirldsVersion)
            version("com.swirlds.config.extensions", swirldsVersion)
            version("com.google.auto.service.processor", "1.1.1")
            version("com.google.auto.service", "1.1.1")
            version("org.hyperledger.besu.nativelib.secp256k1", "0.8.2")

            // gRPC dependencies
            version("io.grpc", "1.65.1")
            version("io.grpc.protobuf", "1.65.1")
            version("io.grpc.stub", "1.65.1")

            // Reference from the protobuf plugin
            version("google.proto", "4.27.3")
            version("grpc.protobuf.grpc", "1.65.1")

            // Google protobuf dependencies
            version("com.google.protobuf", "4.27.3")
            version("com.google.protobuf.util", "4.27.3")

            // PBJ dependencies
            plugin("pbj", "com.hedera.pbj.pbj-compiler").version("0.9.2")
            version("com.hedera.pbj.runtime", "0.9.2")
            version("org.antlr.antlr4.runtime", "4.13.1")

            version("java.annotation", "1.3.2")
            version("javax.inject", "1")
            version("com.google.common", "33.0.0-jre")

            version("org.apache.commons.codec", "1.15")
            version("org.apache.commons.collections4", "4.4")
            version("org.apache.commons.io", "2.15.1")
            version("org.apache.commons.lang3", "3.14.0")
            version("org.apache.commons.compress", "1.26.0")
            version("org.apache.logging.log4j.slf4j2.impl", "2.21.1")

            // needed for dagger
            version("dagger", daggerVersion)
            version("dagger.compiler", daggerVersion)
            version("com.squareup.javapoet", "1.13.0")

            // Testing only versions
            version("org.assertj.core", "3.23.1")
            version("org.junit.jupiter.api", "5.10.2")
            version("org.junit.platform", "1.11.0")
            version("org.mockito", "5.8.0")
            version("org.mockito.junit.jupiter", "5.8.0")
            version("org.testcontainers", "1.20.1")
            version("org.testcontainers.junit-jupiter", "1.20.1")
            version("com.github.docker-java", "3.4.0")
            version("io.github.cdimascio", "5.2.2")
        }
    }
}


// Build cache configuration
val isCiServer = System.getenv().containsKey("CI")
val gradleCacheUsername: String? = System.getenv("GRADLE_CACHE_USERNAME")
val gradleCachePassword: String? = System.getenv("GRADLE_CACHE_PASSWORD")
val gradleCacheAuthorized =
    (gradleCacheUsername?.isNotEmpty() ?: false) && (gradleCachePassword?.isNotEmpty() ?: false)

buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.gradle.hedera.svcs.eng.swirldslabs.io/cache/")
        isPush = isCiServer && gradleCacheAuthorized

        isUseExpectContinue = true
        isEnabled = !gradle.startParameter.isOffline

        if (isCiServer && gradleCacheAuthorized) {
            credentials {
                username = gradleCacheUsername
                password = gradleCachePassword
            }
        }
    }
}
