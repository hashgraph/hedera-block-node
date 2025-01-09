// SPDX-License-Identifier: Apache-2.0
import com.github.jengelman.gradle.plugins.shadow.internal.DefaultDependencyFilter

plugins {
    id("org.hiero.gradle.module.application")
    id("com.gradleup.shadow") version "8.3.5"
}

description = "Hedera Block Stream Tools"

application { mainClass = "com.hedera.block.tools.BlockStreamTool" }

// Allow non-module Jar
extraJavaModuleInfo {
    failOnMissingModuleInfo = false
    failOnAutomaticModules = false
}

mainModuleInfo {
    // depend on peer streams gradle module to get access to protobuf generated classes
    requires("com.hedera.block.stream")
    requires("com.hedera.pbj.runtime")
    requires("com.github.luben.zstd_jni")
    requires("com.google.auth.oauth2")
    requires("com.google.gson")
    requires("info.picocli")
    runtimeOnly("com.swirlds.config.impl")
    runtimeOnly("org.apache.logging.log4j.slf4j2.impl")
    runtimeOnly("io.grpc.netty")
}

testModuleInfo { requiresStatic("com.github.spotbugs.annotations") }

dependencies {
    implementation("com.google.api:gax")
    implementation("com.google.cloud:google-cloud-core")
    implementation("com.google.cloud:google-cloud-storage")
}

// == Shadow plugin configuration ==
tasks.shadowJar {
    // Generate Manifest with Main-Class and Implementation-Title
    manifest {
        attributes(
            "Main-Class" to application.mainClass,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }

    // There is an issue in the shadow plugin that it automatically accesses the
    // files in 'runtimeClasspath' while Gradle is building the task graph.
    // See: https://github.com/GradleUp/shadow/issues/882
    dependencyFilter = NoResolveDependencyFilter()
}

class NoResolveDependencyFilter : DefaultDependencyFilter(project) {
    override fun resolve(configuration: FileCollection): FileCollection {
        return configuration
    }
}
