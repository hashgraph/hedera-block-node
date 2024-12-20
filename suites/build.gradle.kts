// SPDX-License-Identifier: Apache-2.0
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

    // @todo(#343) - :server:createProductionDotEnv should disappear
    dependsOn(":server:createDockerImage", ":server:createProductionDotEnv")

    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
    testClassesDirs = sourceSets["main"].output.classesDirs
    classpath = sourceSets["main"].runtimeClasspath
}
