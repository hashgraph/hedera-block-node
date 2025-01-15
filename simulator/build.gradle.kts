// SPDX-License-Identifier: Apache-2.0
plugins {
    id("org.hiero.gradle.module.library")
    id("application")
}

description = "Hedera Block Stream Simulator"

// Remove the following line to enable all 'javac' lint checks that we have turned on by default
// and then fix the reported issues.
tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-Xlint:-exports") }

application {
    mainModule = "com.hedera.block.simulator"
    mainClass = "com.hedera.block.simulator.BlockStreamSimulator"
}

mainModuleInfo {
    annotationProcessor("dagger.compiler")
    annotationProcessor("com.google.auto.service.processor")
    runtimeOnly("com.swirlds.config.impl")
    runtimeOnly("org.apache.logging.log4j.slf4j2.impl")
    runtimeOnly("io.grpc.netty")
}

testModuleInfo {
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.mockito")
    requires("org.mockito.junit.jupiter")
    requires("org.assertj.core")
    requiresStatic("com.github.spotbugs.annotations")
    requires("com.google.protobuf")
}

// Task to run simulator in Publisher mode
tasks.register<JavaExec>("runPublisherClient") {
    description = "Run the simulator in Publisher Client mode"
    group = "application"

    mainClass = application.mainClass
    mainModule = application.mainModule
    classpath = sourceSets["main"].runtimeClasspath

    environment("BLOCK_STREAM_SIMULATOR_MODE", "PUBLISHER_CLIENT")
    environment("PROMETHEUS_ENDPOINT_ENABLED", "true")
    environment("PROMETHEUS_ENDPOINT_PORT_NUMBER", "9998")
}

tasks.register<JavaExec>("runPublisherServer") {
    description = "Run the simulator in Publisher Server mode"
    group = "application"

    mainClass = application.mainClass
    mainModule = application.mainModule
    classpath = sourceSets["main"].runtimeClasspath

    environment("BLOCK_STREAM_SIMULATOR_MODE", "PUBLISHER_SERVER")
    environment("PROMETHEUS_ENDPOINT_ENABLED", "true")
    environment("PROMETHEUS_ENDPOINT_PORT_NUMBER", "9996")
}

// Task to run simulator in Consumer mode
tasks.register<JavaExec>("runConsumer") {
    description = "Run the simulator in Consumer mode"
    group = "application"

    mainClass = application.mainClass
    mainModule = application.mainModule
    classpath = sourceSets["main"].runtimeClasspath

    environment("BLOCK_STREAM_SIMULATOR_MODE", "CONSUMER")
    environment("PROMETHEUS_ENDPOINT_ENABLED", "true")
    environment("PROMETHEUS_ENDPOINT_PORT_NUMBER", "9997")
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

// Vals
val dockerProjectRootDirectory: Directory = layout.projectDirectory.dir("docker")
var resourcesProjectRootDirectory: Directory = layout.projectDirectory.dir("src/main/resources")
var distributionBuildRootDirectory: Directory = layout.buildDirectory.dir("distributions").get()
val dockerBuildRootDirectory: Directory = layout.buildDirectory.dir("docker").get()

// Docker related tasks
val copyDockerFolder: TaskProvider<Copy> =
    tasks.register<Copy>("copyDockerFolder") {
        description = "Copies the docker folder to the build root directory"
        group = "docker"

        from(dockerProjectRootDirectory)
        into(dockerBuildRootDirectory)
    }

// Docker related tasks
val copyDependenciesFolders: TaskProvider<Copy> =
    tasks.register<Copy>("copyDependenciesFolders") {
        description = "Copies the docker folder to the build root directory"
        group = "docker"

        dependsOn(copyDockerFolder, tasks.assemble)
        from(resourcesProjectRootDirectory)
        from(distributionBuildRootDirectory)
        into(dockerBuildRootDirectory)
    }

val createDockerImage: TaskProvider<Exec> =
    tasks.register<Exec>("createDockerImage") {
        description = "Creates the docker image of the Block Stream Simulator"
        group = "docker"

        dependsOn(copyDependenciesFolders, tasks.assemble)
        workingDir(dockerBuildRootDirectory)
        commandLine("sh", "-c", "docker buildx build -t hedera-block-simulator:latest .")
    }

val startDockerContainer: TaskProvider<Exec> =
    tasks.register<Exec>("startDockerContainer") {
        description = "Creates and starts the docker image of the Block Stream Simulator"
        group = "docker"

        dependsOn(createDockerImage, tasks.assemble)
        workingDir(dockerBuildRootDirectory)

        commandLine("sh", "-c", "./update-env.sh && docker compose -p simulator up -d")
    }

tasks.register<Exec>("stopDockerContainer") {
    description = "Stops running docker containers of the Block Stream Simulator"
    group = "docker"

    dependsOn(copyDockerFolder)
    workingDir(dockerBuildRootDirectory)
    commandLine("sh", "-c", "docker compose -p simulator stop")
}
