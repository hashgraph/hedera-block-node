// SPDX-License-Identifier: Apache-2.0
import com.google.protobuf.gradle.id
import com.hedera.block.tasks.GitClone

plugins {
    id("java-library")
    id("com.hedera.block.conventions")
    id("com.google.protobuf")
}

tasks.register<GitClone>("cloneHederaProtobufs") {
    url = "https://github.com/hashgraph/hedera-protobufs.git"
    offline = gradle.startParameter.isOffline
    localCloneDirectory = layout.buildDirectory.dir("hedera-protobufs")
}

// Configure Protobuf Plugin to download protoc executable rather than using local installed version
protobuf {
    val libs = the<VersionCatalogsExtension>().named("libs")
    protoc { artifact = "com.google.protobuf:protoc:" + libs.findVersion("google.proto").get() }
    plugins {
        // Add GRPC plugin as we need to generate GRPC services
        id("grpc") {
            artifact =
                "io.grpc:protoc-gen-grpc-java:" + libs.findVersion("grpc.protobuf.grpc").get()
        }
    }
    generateProtoTasks { all().forEach { it.plugins { id("grpc") } } }
}

tasks.javadoc {
    options {
        this as StandardJavadocDocletOptions
        // There are violations in the generated protobuf code
        addStringOption("Xdoclint:-reference,-html", "-quiet")
    }
}

// Give JUnit more ram and make it execute tests in parallel
tasks.test {
    // We are running a lot of tests 10s of thousands, so they need to run in parallel. Make each
    // class run in parallel.
    systemProperties["junit.jupiter.execution.parallel.enabled"] = true
    systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
    // limit amount of threads, so we do not use all CPU
    systemProperties["junit.jupiter.execution.parallel.config.dynamic.factor"] = "0.9"
}
