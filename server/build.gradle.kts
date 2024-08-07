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

testModuleInfo {
    requires("org.junit.jupiter.api")
    requires("org.mockito")
    requires("org.mockito.junit.jupiter")
    requiresStatic("com.github.spotbugs.annotations")
}

var updateDockerEnv =
    tasks.register<Exec>("updateDockerEnv") {
        description =
            "Creates the .env file in the docker folder that contains environment variables for docker"
        group = "docker"

        workingDir(layout.projectDirectory.dir("docker"))
        commandLine("./update-env.sh", project.version)
    }

tasks.register<Exec>("createDockerImage") {
    description = "Creates the docker image of the Block Node Server based on the current version"
    group = "docker"

    dependsOn(updateDockerEnv, tasks.assemble)
    workingDir(layout.projectDirectory.dir("docker"))
    commandLine("./docker-build.sh", project.version, layout.projectDirectory.dir("..").asFile)
}

tasks.register<Exec>("startDockerContainer") {
    description = "Starts the docker container of the Block Node Server of the current version"
    group = "docker"

    dependsOn(updateDockerEnv)
    workingDir(layout.projectDirectory.dir("docker"))
    commandLine("sh", "-c", "docker-compose -p block-node up -d")
}

tasks.register<Exec>("startDockerDebugContainer") {
    description = "Starts the docker container of the Block Node Server of the current version"
    group = "docker"

    workingDir(layout.projectDirectory.dir("docker"))

    commandLine(
        "sh",
        "-c",
        "./update-env.sh ${project.version} true && docker compose -p block-node up -d"
    )
}

tasks.register<Exec>("stopDockerContainer") {
    description = "Stops running docker containers of the Block Node Server"
    group = "docker"

    dependsOn(updateDockerEnv)
    workingDir(layout.projectDirectory.dir("docker"))
    commandLine("sh", "-c", "docker-compose -p block-node stop")
}
