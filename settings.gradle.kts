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


include(":stream")
include(":server")

includeBuild(".") // https://github.com/gradle/gradle/issues/21490#issuecomment-1458887481

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

// The HAPI API version to use for Protobuf sources.
val hapiProtoVersion = "0.53.0"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Define a constant for the platform SDK version.
            // Platform SDK modules are all released together with matching versions.
            val swirldsVersion = "0.51.5"

            // Compile time dependencies
            version("com.google.protobuf", "3.24.0")
            version("io.helidon.webserver.http2", "4.0.11")
            version("io.helidon.webserver.grpc", "4.0.11")
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

            // PBJ dependencies
//            version("io.grpc", "1.64.0")
            version("io.grpc.protobuf", "1.64.0")
            version("io.grpc.stub", "1.64.0")

            plugin("pbj", "com.hedera.pbj.pbj-compiler").version("0.8.9")
            version("com.hedera.pbj.runtime", "0.8.9")
            version("org.antlr.antlr4.runtime", "4.13.1")

            version("java.annotation", "1.3.2")
            version("javax.inject", "1")
            version("com.google.protobuf", "3.21.7")
            version("com.google.protobuf.util", "3.21.7")

            version("hapi-proto", hapiProtoVersion)
            version("com.google.common", "33.0.0-jre")
//            version("com.google.common", "31.1-jre")

            version("org.apache.commons.codec", "1.15")
            version("org.apache.commons.collections4", "4.4")
            version("org.apache.commons.io", "2.15.1")
            version("org.apache.commons.lang3", "3.14.0")
            version("org.apache.commons.compress", "1.26.0")
            version("org.slf4j", "1.7.30")

            // Testing only versions
            version("org.assertj.core", "3.23.1")
            version("org.junit.jupiter.api", "5.10.2")
            version("org.mockito", "5.8.0")
            version("org.mockito.junit.jupiter", "5.8.0")
            version("google.proto", "3.21.10")
            version("grpc.protobuf.grpc", "1.45.1")

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

fun include(name: String, path: String) {
    include(name)
    project(name).projectDir = File(rootDir, path)
}
