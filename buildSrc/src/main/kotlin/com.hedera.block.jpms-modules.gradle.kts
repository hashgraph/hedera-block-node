/*
 * Copyright (C) 2016-2024 Hedera Hashgraph, LLC
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
    id("org.gradlex.jvm-dependency-conflict-resolution")
    id("org.gradlex.extra-java-module-info")
}

// Fix or enhance the metadata of third-party Modules. This is about the metadata in the
// repositories: '*.pom' and '*.module' files.
jvmDependencyConflicts.patch {
    // These compile time annotation libraries are not of interest in our setup and are thus removed
    // from the dependencies of all components that bring them in.
    val annotationLibraries =
        listOf(
            "com.google.android:annotations",
            "com.google.code.findbugs:annotations",
            "com.google.code.findbugs:jsr305",
            "com.google.errorprone:error_prone_annotations",
            "com.google.guava:listenablefuture",
            "org.checkerframework:checker-compat-qual",
            "org.checkerframework:checker-qual",
            "org.codehaus.mojo:animal-sniffer-annotations"
        )

    module("io.grpc:grpc-netty-shaded") { annotationLibraries.forEach { removeDependency(it) } }

    module("io.grpc:grpc-api") { annotationLibraries.forEach { removeDependency(it) } }
    module("io.grpc:grpc-core") { annotationLibraries.forEach { removeDependency(it) } }
    module("io.grpc:grpc-context") { annotationLibraries.forEach { removeDependency(it) } }
    module("io.grpc:grpc-protobuf") { annotationLibraries.forEach { removeDependency(it) } }
    module("io.grpc:grpc-protobuf-lite") {
        annotationLibraries.forEach { removeDependency(it) }
        removeDependency(/* dependency = */ "com.google.protobuf:protobuf-javalite")
        addApiDependency("com.google.protobuf:protobuf-java")
    }
    module("io.grpc:grpc-services") { annotationLibraries.forEach { removeDependency(it) } }
    module("io.grpc:grpc-stub") { annotationLibraries.forEach { removeDependency(it) } }
    module("io.grpc:grpc-testing") { annotationLibraries.forEach { removeDependency(it) } }
    module("io.grpc:grpc-util") { annotationLibraries.forEach { removeDependency(it) } }
    module("com.google.dagger:dagger-compiler") {
        annotationLibraries.forEach { removeDependency(it) }
    }
    module("com.google.dagger:dagger-producers") {
        annotationLibraries.forEach { removeDependency(it) }
    }
    module("com.google.dagger:dagger-spi") { annotationLibraries.forEach { removeDependency(it) } }
    module("com.google.guava:guava") {
        (annotationLibraries -
                "com.google.code.findbugs:jsr305" -
                "com.google.errorprone:error_prone_annotations" -
                "org.checkerframework:checker-qual")
            .forEach { removeDependency(it) }
    }
    module("com.google.protobuf:protobuf-java-util") {
        annotationLibraries.forEach { removeDependency(it) }
    }
    module("com.google.cloud:google-cloud-storage") { annotationLibraries.forEach { removeDependency(it) } }
    module("com.google.api.grpc:proto-google-cloud-monitoring-v3") { annotationLibraries.forEach { removeDependency(it) } }
    module("com.google.cloud:google-cloud-monitoring") { annotationLibraries.forEach { removeDependency(it) } }
    module("io.prometheus:simpleclient") {
        removeDependency("io.prometheus:simpleclient_tracer_otel")
        removeDependency("io.prometheus:simpleclient_tracer_otel_agent")
    }
    module("org.jetbrains.kotlin:kotlin-stdlib") {
        removeDependency("org.jetbrains.kotlin:kotlin-stdlib-common")
    }
    module("junit:junit") { removeDependency("org.hamcrest:hamcrest-core") }
    module("org.hyperledger.besu:secp256k1") { addApiDependency("net.java.dev.jna:jna") }
}

// Fix or enhance the 'module-info.class' of third-party Modules. This is about the
// 'module-info.class' inside the Jar files. In our full Java Modules setup every
// Jar needs to have this file. If it is missing, it is added by what is configured here.
extraJavaModuleInfo {
    failOnAutomaticModules = true // Only allow Jars with 'module-info' on all module paths

    module("io.grpc:grpc-api", "io.grpc") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
        requires("com.google.cloud.core")
        uses("io.grpc.ManagedChannelProvider")
        uses("io.grpc.NameResolverProvider")
        uses("io.grpc.LoadBalancerProvider")
    }

    module("io.grpc:grpc-core", "io.grpc.internal") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("io.grpc:grpc-context", "io.grpc.context")
    module("io.grpc:grpc-stub", "io.grpc.stub") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("io.grpc:grpc-testing", "io.grpc.testing")
    module("io.grpc:grpc-services", "io.grpc.services")
    module("io.grpc:grpc-util", "io.grpc.util")
    module("io.grpc:grpc-protobuf", "io.grpc.protobuf") {
        requires("com.google.cloud.core")
        exportAllPackages()
    }
    module("io.grpc:grpc-protobuf-lite", "io.grpc.protobuf.lite")

    module("io.grpc:grpc-netty-shaded", "io.grpc.netty.shaded") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
        requires("jdk.unsupported")
        ignoreServiceProvider("reactor.blockhound.integration.BlockHoundIntegration")
    }

    module("com.github.spotbugs:spotbugs-annotations", "com.github.spotbugs.annotations")
    module("com.google.code.findbugs:jsr305", "java.annotation") {
        exportAllPackages()
        mergeJar("javax.annotation:javax.annotation-api")
    }
    module("com.google.errorprone:error_prone_annotations", "com.google.errorprone.annotations") {
        exportAllPackages()
        patchRealModule()
    }
    module("com.google.j2objc:j2objc-annotations", "com.google.j2objc.annotations")
    module("com.google.protobuf:protobuf-java", "com.google.protobuf") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("com.google.guava:guava", "com.google.common") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("com.google.guava:failureaccess", "com.google.common.util.concurrent.internal")
    module("com.google.dagger:dagger", "dagger")
    module("io.perfmark:perfmark-api", "io.perfmark")
    module("javax.inject:javax.inject", "javax.inject")

    module("commons-codec:commons-codec", "org.apache.commons.codec"){
        exportAllPackages()
        patchRealModule()
    }
    module("org.apache.commons:commons-math3", "org.apache.commons.math3")
    module("org.apache.commons:commons-collections4", "org.apache.commons.collections4")
    module("com.esaulpaugh:headlong", "headlong")

    module("org.checkerframework:checker-qual", "org.checkerframework.checker.qual") {
        exportAllPackages()
        patchRealModule()
    }
    module("net.i2p.crypto:eddsa", "net.i2p.crypto.eddsa")
    module("org.jetbrains:annotations", "org.jetbrains.annotations")
    module("org.antlr:antlr4-runtime", "org.antlr.antlr4.runtime")

    // needed for metrics and logging, but also several platform classes
    module("com.goterl:resource-loader", "resource.loader")
    module("com.goterl:lazysodium-java", "lazysodium.java")
    module("org.hyperledger.besu:secp256k1", "org.hyperledger.besu.nativelib.secp256k1")
    module("net.java.dev.jna:jna", "com.sun.jna") {
        exportAllPackages()
        requires("java.logging")
    }
    module("io.prometheus:simpleclient", "io.prometheus.simpleclient")
    module("io.prometheus:simpleclient_common", "io.prometheus.simpleclient_common")
    module("io.prometheus:simpleclient_httpserver", "io.prometheus.simpleclient.httpserver") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("jdk.httpserver")
    }

    // used in tools

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
//    module("com.google.api.grpc:proto-google-common-protos", "com.google.api.grpc.common")
    module("com.google.cloud:google-cloud-core-grpc", "com.google.cloud.core.grpc")
    module("com.google.cloud:google-cloud-core-http", "com.google.cloud.core.http")
    module("com.google.cloud:google-cloud-monitoring", "com.google.cloud.monitoring")
    module("com.google.http-client:google-http-client", "com.google.api.client")
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
    module("commons-logging:commons-logging", "org.apache.commons.logging")
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

    // Annotation processing only
    module("com.google.auto.service:auto-service-annotations", "com.google.auto.service")
    module("com.google.auto.service:auto-service", "com.google.auto.service.processor")
    module("com.google.auto:auto-common", "com.google.auto.common")
    module("com.google.dagger:dagger-compiler", "dagger.compiler")
    module("com.google.dagger:dagger-producers", "dagger.producers")
    module("com.google.dagger:dagger-spi", "dagger.spi")
    module(
        "com.google.devtools.ksp:symbol-processing-api",
        "com.google.devtools.ksp.symbolprocessingapi"
    )
    module("com.google.errorprone:javac-shaded", "com.google.errorprone.javac.shaded")
    module("com.google.googlejavaformat:google-java-format", "com.google.googlejavaformat")
    module("net.ltgt.gradle.incap:incap", "net.ltgt.gradle.incap")
    module("org.jetbrains.kotlinx:kotlinx-metadata-jvm", "kotlinx.metadata.jvm")

    // Test clients only
    module("com.github.docker-java:docker-java-api", "com.github.dockerjava.api")
    module("com.github.docker-java:docker-java-transport", "com.github.dockerjava.transport")
    module(
        "com.github.docker-java:docker-java-transport-zerodep",
        "com.github.dockerjava.transport.zerodep"
    )
    module("io.github.cdimascio:java-dotenv", "io.github.cdimascio")
    module("com.google.protobuf:protobuf-java-util", "com.google.protobuf.util")
    module("com.squareup:javapoet", "com.squareup.javapoet") {
        exportAllPackages()
        requires("java.compiler")
    }
    module("junit:junit", "junit")
    module("org.hamcrest:hamcrest", "org.hamcrest")
    module("org.json:json", "org.json")
    module("org.mockito:mockito-core", "org.mockito")
    module("org.objenesis:objenesis", "org.objenesis")
    module("org.rnorth.duct-tape:duct-tape", "org.rnorth.ducttape")
    module("org.testcontainers:junit-jupiter", "org.testcontainers.junit.jupiter")
    module("org.testcontainers:testcontainers", "org.testcontainers")
    module("org.mockito:mockito-junit-jupiter", "org.mockito.junit.jupiter")
}

// Make 'javax.annotation:javax.annotation-api' discoverable for merging it into
// 'com.google.code.findbugs:jsr305'
dependencies { "javaModulesMergeJars"("javax.annotation:javax.annotation-api:1.3.2") }
