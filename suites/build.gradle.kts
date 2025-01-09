// SPDX-License-Identifier: Apache-2.0
plugins {
    id("org.hiero.gradle.module.library")
    id("application")
}

description = "Hedera Block Node E2E Suites"

application {
    mainModule = "com.hedera.block.suites"
    mainClass = "com.hedera.block.suites.BaseSuite"
}

mainModuleInfo {
    runtimeOnly("org.testcontainers.junit.jupiter")
    runtimeOnly("org.junit.jupiter.engine")
    runtimeOnly("org.testcontainers")
    runtimeOnly("com.swirlds.config.impl")
}

val updateDockerEnv =
    tasks.register<Exec>("updateDockerEnv") {
        description =
            "Creates the .env file in the docker folder that contains environment variables for Docker"
        group = "docker"

        workingDir(layout.projectDirectory.dir("../server/docker"))
        commandLine("sh", "-c", "./update-env.sh ${project.version} false false")
    }

// Task to build the Docker image
tasks.register<Exec>("createDockerImage") {
    description = "Creates the Docker image of the Block Node Server based on the current version"
    group = "docker"

    dependsOn(updateDockerEnv, tasks.assemble)
    workingDir(layout.projectDirectory.dir("../server/docker"))
    commandLine("./docker-build.sh", project.version, layout.projectDirectory.dir("..").asFile)
}

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
