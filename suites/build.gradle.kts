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
    id("com.hedera.block.suites")
}

description = "Hedera Block Node E2E Suites"

dependencies { implementation(project(":simulator")) }

application {
    mainModule = "com.hedera.block.suites"
    mainClass = "com.hedera.block.suites.BaseSuite"
}

mainModuleInfo {
    runtimeOnly("org.testcontainers.junit-jupiter")
    runtimeOnly("org.junit.jupiter.engine")
    runtimeOnly("org.testcontainers")
    runtimeOnly("com.swirlds.config.impl")
}

// workaround until https://github.com/hashgraph/hedera-block-node/pull/216 is integrated
dependencies.constraints { implementation("org.slf4j:slf4j-api:2.0.6") }

tasks.register<Test>("runSuites") {
    description = "Runs E2E Test Suites"
    group = "suites"
    modularity.inferModulePath = false

    // @todo(#343) - :server:createDotEnv should disappear
    dependsOn(":server:createDockerImage", ":server:createProductionDotEnv")

    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
    testClassesDirs = sourceSets["main"].output.classesDirs
    classpath = sourceSets["main"].runtimeClasspath
}
