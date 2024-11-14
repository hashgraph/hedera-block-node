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
    id("com.hedera.block.server")
}

application {
    mainModule = "com.hedera.block.server"
    mainClass = "com.hedera.block.server.Server"
}

mainModuleInfo {
    annotationProcessor("dagger.compiler")
    annotationProcessor("com.google.auto.service.processor")
    runtimeOnly("com.swirlds.config.impl")
    runtimeOnly("org.apache.logging.log4j.slf4j2.impl")
    runtimeOnly("io.helidon.logging")
    runtimeOnly("com.hedera.pbj.grpc.helidon.config")
}

testModuleInfo {
    annotationProcessor("dagger.compiler")
    requires("org.junit.jupiter.api")
    requires("org.mockito")
    requires("org.mockito.junit.jupiter")
    requiresStatic("com.github.spotbugs.annotations")
}

// Release related tasks

fun replaceVersion(files: String, match: String) {
    ant.withGroovyBuilder {
        "replaceregexp"("match" to match, "replace" to project.version, "flags" to "gm") {
            "fileset"(
                "dir" to rootProject.projectDir,
                "includes" to files,
                "excludes" to "**/node_modules/"
            )
        }
    }
}

tasks.register("bumpVersion") {
    description = "Bump versions of the project"
    group = "release"

    replaceVersion("charts/**/Chart.yaml", "(?<=^(appVersion|version): ).+")
    replaceVersion("gradle.properties", "(?<=^version=).+")
}

// Vals
val dockerProjectRootDirectory: Directory = layout.projectDirectory.dir("docker")
val dockerBuildRootDirectory: Directory = layout.buildDirectory.dir("docker").get()

// Docker related tasks
val copyDockerFolder: TaskProvider<Copy> =
    tasks.register<Copy>("copyDockerFolder") {
        description = "Copies the docker folder to the build root directory"
        group = "docker"

        from(dockerProjectRootDirectory)
        into(dockerBuildRootDirectory)
    }

// @todo(#343) - createProductionDotEnv is temporary and used by the suites,
//  once the suites no longer rely on the .env file from the build root we
//  should remove this task
val createProductionDotEnv: TaskProvider<Exec> =
    tasks.register<Exec>("createProductionDotEnv") {
        description = "Creates the default dotenv file for the Block Node Server"
        group = "docker"

        dependsOn(copyDockerFolder)
        workingDir(dockerBuildRootDirectory)
        commandLine("sh", "-c", "./update-env.sh ${project.version} false false")
    }

val createDockerImage: TaskProvider<Exec> =
    tasks.register<Exec>("createDockerImage") {
        description =
            "Creates the production docker image of the Block Node Server based on the current version"
        group = "docker"

        dependsOn(copyDockerFolder, tasks.assemble)
        workingDir(dockerBuildRootDirectory)
        commandLine("sh", "-c", "./docker-build.sh ${project.version}")
    }

tasks.register<Exec>("startDockerContainer") {
    description =
        "Starts the docker productiongit a container of the Block Node Server for the current version"
    group = "docker"

    dependsOn(createDockerImage)
    workingDir(dockerBuildRootDirectory)
    commandLine(
        "sh",
        "-c",
        "./update-env.sh ${project.version} false false && docker compose -p block-node up -d"
    )
}

tasks.register<Exec>("startDockerContainerDebug") {
    description =
        "Starts the docker debug container of the Block Node Server for the current version"
    group = "docker"

    dependsOn(createDockerImage)
    workingDir(dockerBuildRootDirectory)
    commandLine(
        "sh",
        "-c",
        "./update-env.sh ${project.version} true false && docker compose -p block-node up -d"
    )
}

tasks.register<Exec>("startDockerContainerSmokeTest") {
    description =
        "Starts the docker smoke test container of the Block Node Server for the current version"
    group = "docker"

    dependsOn(createDockerImage)
    workingDir(dockerBuildRootDirectory)
    commandLine(
        "sh",
        "-c",
        "./update-env.sh ${project.version} false true && docker compose -p block-node up -d"
    )
}

tasks.register<Exec>("stopDockerContainer") {
    description = "Stops running docker containers of the Block Node Server"
    group = "docker"

    dependsOn(copyDockerFolder)
    workingDir(dockerBuildRootDirectory)
    commandLine("sh", "-c", "docker compose -p block-node stop")
}
