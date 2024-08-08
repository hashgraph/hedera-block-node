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

include(":protos")
include(":server")

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

            // Compile time dependencies
            version("com.google.protobuf", "3.24.0")
            version("io.helidon.webserver.http2", "4.0.10")
            version("io.helidon.webserver.grpc", "4.0.10")
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
            // Testing only versions
            version("org.assertj.core", "3.23.1")
            version("org.junit.jupiter.api", "5.10.2")
            version("org.mockito", "5.8.0")
            version("org.mockito.junit.jupiter", "5.8.0")
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
