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

application {
    mainModule = "com.hedera.block.suites"
    mainClass = "com.hedera.block.suites.BaseSuite"
}

mainModuleInfo {
    requires("org.junit.jupiter.api")
    requires("org.junit.platform.suite.api")
    requires("org.testcontainers")
    requires("io.github.cdimascio")
    runtimeOnly("org.testcontainers.junit-jupiter")
    runtimeOnly("org.junit.jupiter.engine")
}

// Let's discuss: we have two repeating task names with the suites and server - 'updateDockerEnv'
// and 'createDockerImage'. When running any of these tasks from repo root w/o any specific qualifiers
// like ':suites:updateDockerEnv', both will run. While the overhead is only once since this gets
// cached, it might be a good idea to change the task names so they are different, and we can be sure
// what will be run from repo root w/o having qualifiers. For these building tasks are probably not
// going to mess up some common files between them, it is a concern for the future, since not clearly
// knowing what is being run could lead to silent bugs that are hard to come by. Not to mention, if
// we have a task that will run a project, and then a task with the same name that will also run a
// project, now when executing from root we will run two projects which might be problematic.
// Having qualifiers is a 'need-to-know' solution which is not preferable. But there is also the case
// that it would be a good idea to have tasks with the same name that we know for sure run isolated,
// and we need them to run on all projects easily, so running the task from repo root will execute
// everywhere it is present. So the question is should these remain with the same name and what
// should be our approach for the future?

val updateDockerEnv =
    tasks.register<Exec>("updateDockerEnv") {
        description =
            "Creates the .env file in the docker folder that contains environment variables for Docker"
        group = "docker"

        workingDir(layout.projectDirectory.dir("../server/docker"))
        commandLine("./update-env.sh", project.version)
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
    dependsOn("createDockerImage")

    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
    testClassesDirs = sourceSets["main"].output.classesDirs
    classpath = sourceSets["main"].runtimeClasspath
}
