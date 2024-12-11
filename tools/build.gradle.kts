/*
 * Copyright (C) 2022-2024 Hedera Hashgraph, LLC
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
    id("application")
    id("com.hedera.block.tools")
}

description = "Hedera Block Stream Tools"

application {
    mainModule = "com.hedera.block.tools"
    mainClass = "com.hedera.block.tools.BlockStreamTool"
}

// Switch compilation from modules back to classpath because 3rd party libraries are not modularized
tasks.compileJava {
    // Do not use '--module-path' despite `module-info.java`
    modularity.inferModulePath = false
    // Do not compile module-info, we use it only to extract dependencies for now
    exclude("module-info.java")
}

extraJavaModuleInfo {
    // Allow non-module Jar
    failOnMissingModuleInfo = false
    failOnAutomaticModules = false

    module("com.google.cloud:google-cloud-core", "com.google.cloud.core"){
        exportAllPackages()
    }
    module("com.google.cloud:google-cloud-storage", "com.google.cloud.storage")
    // dependencies for google cloud storage API
    module("org.apache.httpcomponents:httpclient", "org.apache.httpcomponents.httpclient")
    module("org.apache.httpcomponents:httpcore", "org.apache.httpcomponents.httpcore")
    module("com.google.api-client:google-api-client", "google.api.client")
    module("com.google.api.grpc:gapic-google-cloud-storage-v2", "com.google.api.grpc.cloud.storage.v2")
    module("com.google.api.grpc:grpc-google-cloud-storage-v2", "com.google.api.grpc.cloud.storage.v2")
    module("com.google.api.grpc:proto-google-cloud-monitoring-v3", "com.google.api.grpc.cloud.monitoring.v3")
    module("com.google.api.grpc:proto-google-cloud-storage-v2", "com.google.api.grpc.cloud.storage.v2")
    module("com.google.api.grpc:proto-google-iam-v1", "com.google.api.grpc.iam.v1") {
        requires("com.google.cloud.core")
    }
    module("com.google.api:api-common", "com.google.api.apicommon")
    module("com.google.api:gax", "com.google.api.gax") {
        requires("com.google.cloud.core")
    }
    module("com.google.api:gax-grpc", "com.google.api.gax.grpc")
    module("com.google.api:gax-httpjson", "com.google.api.gax.httpjson")
    module("com.google.apis:google-api-services-storage", "com.google.api.services.storage")
    module("com.google.apis:google-api-services-storage", "com.google.api.services.storage")
    module("com.google.auth:google-auth-library-credentials", "com.google.auth")
    module("com.google.auth:google-auth-library-oauth2-http", "com.google.auth.oauth2")
    module("com.google.auto.value:auto-value-annotations", "com.google.auto.value")
    module("com.google.cloud.opentelemetry:detector-resources-support", "com.google.cloud.opentelemetry.detector.resources.support")
    module("com.google.cloud.opentelemetry:exporter-metrics", "com.google.cloud.opentelemetry.exporter.metrics")
    module("com.google.cloud.opentelemetry:shared-resourcemapping", "com.google.cloud.opentelemetry.shared.resourcemapping")
    module("com.google.cloud:google-cloud-core", "com.google.cloud.core") {
        mergeJar("com.google.api.grpc:proto-google-common-protos")
        exportAllPackages()
    }
    module("com.google.cloud:google-cloud-core-grpc", "com.google.cloud.core.grpc")
    module("com.google.cloud:google-cloud-core-http", "com.google.cloud.core.http")
    module("com.google.cloud:google-cloud-monitoring", "com.google.cloud.monitoring")
    module("com.google.http-client:google-http-client", "com.google.api.client")
    module("com.google.http-client:google-http-client-apache-v2", "com.google.api.client.apache")
    module("com.google.http-client:google-http-client-apache-v2", "com.google.api.client.http.apache.v2")
    module("com.google.http-client:google-http-client-appengine", "com.google.api.client.extensions.appengine")
    module("com.google.http-client:google-http-client-gson", "com.google.api.client.json.gson")
    module("com.google.http-client:google-http-client-jackson2", "com.google.api.client.json.jackson2")
    module("com.google.j2objc:j2objc-annotations", "com.google.j2objc.annotations") {
        exportAllPackages()
        patchRealModule()
    }
    module("com.google.oauth-client:google-oauth-client", "com.google.api.client.auth")
    module("com.google.re2j:re2j", "com.google.re2j")
    module("commons-logging:commons-logging", "org.apache.commons.logging") {
        exportAllPackages()
    }
    module("io.grpc:grpc-alts", "io.grpc.alts")
    module("io.grpc:grpc-auth", "io.grpc.auth")
    module("io.grpc:grpc-googleapis", "io.grpc.googleapis")
    module("io.grpc:grpc-grpclb", "io.grpc.grpclb")
    module("io.grpc:grpc-inprocess", "io.grpc.inprocess")
    module("io.grpc:grpc-opentelemetry", "io.grpc.opentelemetry")
    module("io.grpc:grpc-rls", "io.grpc.rls")
    module("io.grpc:grpc-xds", "io.grpc.xds")
    module("io.opencensus:opencensus-api", "io.opencensus.api")
    module("io.opencensus:opencensus-contrib-http-util", "io.opencensus.contrib.http.util")
    module("io.opencensus:opencensus-proto", "io.opencensus.proto")
    module("io.opentelemetry.contrib:opentelemetry-gcp-resources", "io.opentelemetry.contrib.gcp.resources")
    module("io.opentelemetry.semconv:opentelemetry-semconv", "io.opentelemetry.semconv")
    module("io.opentelemetry:opentelemetry-api", "io.opentelemetry.api")
    module("io.opentelemetry:opentelemetry-api-incubator", "io.opentelemetry.api.incubator")
    module("io.opentelemetry:opentelemetry-context", "io.opentelemetry.context")
    module("io.opentelemetry:opentelemetry-sdk", "io.opentelemetry.sdk")
    module("io.opentelemetry:opentelemetry-sdk-common", "io.opentelemetry.sdk.common")
    module("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi", "io.opentelemetry.sdk.autoconfigure.spi")
    module("io.opentelemetry:opentelemetry-sdk-logs", "io.opentelemetry.sdk.logs")
    module("io.opentelemetry:opentelemetry-sdk-metrics", "io.opentelemetry.sdk.metrics")
    module("io.opentelemetry:opentelemetry-sdk-trace", "io.opentelemetry.sdk.trace")
    module("org.conscrypt:conscrypt-openjdk-uber", "org.conscrypt")
    module("org.threeten:threetenbp", "org.threeten.bp")
}

mainModuleInfo {
    runtimeOnly("com.swirlds.config.impl")
    runtimeOnly("org.apache.logging.log4j.slf4j2.impl")
    runtimeOnly("io.grpc.netty.shaded")
}

testModuleInfo { requiresStatic("com.github.spotbugs.annotations") }

dependencies {
    implementation(platform("com.google.cloud:libraries-bom:26.49.0"))
    implementation("com.google.cloud:google-cloud-storage")
    implementation("com.github.luben:zstd-jni:1.5.6-6")
}