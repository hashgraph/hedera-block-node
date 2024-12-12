import org.gradlex.javamodule.dependencies.tasks.ModuleDirectivesScopeCheck

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
    mainClass = "com.hedera.block.tools.BlockStreamTool"
}

// Switch compilation from modules back to classpath because 3rd party libraries are not modularized
tasks.compileJava {
    // Do not use '--module-path' despite `module-info.java`
    modularity.inferModulePath = false
    // Do not compile module-info, we use it only to extract dependencies for now
    exclude("module-info.java")
}

// Allow non-module Jar
extraJavaModuleInfo {
    failOnMissingModuleInfo = false
    failOnAutomaticModules = false
}

// Disable module directives scope check as we are not using modules
tasks.withType<ModuleDirectivesScopeCheck>().configureEach {
    enabled = false
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
    implementation("info.picocli:picocli:4.7.6")
    // depend on peer streams gradle module to get access to protobuf generated classes
    implementation(project(":stream"))
}