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
    id("com.hedera.block.simulator")
}

// dependencies { implementation(project(":common")) }

description = "Hedera Block Stream Simulator"

application {
    mainModule = "com.hedera.block.simulator"
    mainClass = "com.hedera.block.simulator.BlockStreamSimulator"
}

mainModuleInfo {
    annotationProcessor("dagger.compiler")
    annotationProcessor("com.google.auto.service.processor")
    runtimeOnly("com.swirlds.config.impl")
    runtimeOnly("org.apache.logging.log4j.slf4j2.impl")
    runtimeOnly("io.grpc.netty.shaded")
}

testModuleInfo {
    requires("org.junit.jupiter.api")
    requires("org.mockito")
    requires("org.mockito.junit.jupiter")
    requiresStatic("com.github.spotbugs.annotations")
    requires("com.google.protobuf")
}

tasks.register<Copy>("untarTestBlockStream") {
    description = "Untar the test block stream data"
    group = "build"

    val targetDir = file("src/main/resources")

    from(tarTree(resources.gzip(file("src/main/resources/block-0.0.3.tar.gz"))))
    into(targetDir)

    // Mark task as not up-to-date if the directory is empty
    outputs.upToDateWhen { targetDir.listFiles()?.isNotEmpty() ?: false }

    // Adding a simple logging to verify
    doLast { println("Untar task completed. Files should be in: ${targetDir.absolutePath}") }
}

tasks.named("processResources") { dependsOn(tasks.named("untarTestBlockStream")) }

tasks.named("sourcesJar") { dependsOn(tasks.named("untarTestBlockStream")) }
