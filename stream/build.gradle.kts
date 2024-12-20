// SPDX-License-Identifier: Apache-2.0
plugins {
    id("com.hedera.block.protobuf")
    alias(libs.plugins.pbj)
}

group = "com.hedera.block"

description = "Hedera API"

// Remove the following line to enable all 'javac' lint checks that we have turned on by default
// and then fix the reported issues.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-exports,-deprecation,-removal")
}

// Add downloaded HAPI repo protobuf files into build directory and add to sources to build them
tasks.cloneHederaProtobufs {
    // uncomment below to use a specific tag
    // tag = "v0.53.0" or a specific commit like "0047255"
    tag = "eab8b58e30336512bcf387c803e6fc86b6ebe010"

    // uncomment below to use a specific branch
    // branch = "main"
}

sourceSets {
    main {
        pbj {
            srcDir(tasks.cloneHederaProtobufs.flatMap { it.localCloneDirectory.dir("services") })
            srcDir(tasks.cloneHederaProtobufs.flatMap { it.localCloneDirectory.dir("block") })
            srcDir(tasks.cloneHederaProtobufs.flatMap { it.localCloneDirectory.dir("platform") })
            srcDir(tasks.cloneHederaProtobufs.flatMap { it.localCloneDirectory.dir("streams") })
        }
        proto {
            srcDir(tasks.cloneHederaProtobufs.flatMap { it.localCloneDirectory.dir("services") })
            srcDir(tasks.cloneHederaProtobufs.flatMap { it.localCloneDirectory.dir("block") })
            srcDir(tasks.cloneHederaProtobufs.flatMap { it.localCloneDirectory.dir("platform") })
            srcDir(tasks.cloneHederaProtobufs.flatMap { it.localCloneDirectory.dir("streams") })
        }
    }
}

testModuleInfo {
    // we depend on the protoc compiled hapi during test as we test our pbj generated code
    // against it to make sure it is compatible
    requires("com.google.protobuf.util")
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
}
