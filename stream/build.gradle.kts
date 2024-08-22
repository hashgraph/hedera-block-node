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
    // tag = "v0.53.0"
    // uncomment below to use a specific branch
    branch = "main"
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
