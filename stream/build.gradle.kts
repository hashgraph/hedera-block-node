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

tasks.withType<Copy>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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

dependencies {

    // requires transitive in the module-info.java
    api("com.google.protobuf:protobuf-java:3.24.0") {
        because("com.google.protobuf")
    }
    api("com.hedera.pbj:pbj-runtime:0.8.9") {
        because("com.hedera.pbj.runtime")
    }
    api("io.grpc:grpc-stub:1.64.0") {
        because("io.grpc.stub")
    }
//    api("io.grpc:grpc-api:1.64.0") {
//        because("io.grpc")
//    }
//    api("com.google.guava:guava:33.0.0-jre") {
//        because("com.google.common")
//    }

    // requires static in the module-info.java
    runtimeOnly("com.github.spotbugs:spotbugs-annotations:4.7.3") {
        because("com.github.spotbugs.annotations")
    }
    runtimeOnly("javax.annotation:javax.annotation-api:1.3.2") {
        because("java.annotation")
    }

//    compileOnly("java.annotation")
//    api(libs.findVersion("com.google.common").get())
//    api(libs.findVersion("com.google.protobuf").get())
//    compileOnly(libs.findVersion("java.annotation").get())
//    implementation("com.google.guava:guava:33.0.0-jre")
//    implementation("com.hedera.pbj:pbj-runtime:0.8.9")
//    implementation("io.grpc:grpc-stub:1.64.0")
}

//mainModuleInfo {
//    runtimeOnly("javax.annotation:javax.annotation-api")
//}

testModuleInfo {
    requires("com.hedera.node.hapi")
//     we depend on the protoc compiled hapi during test as we test our pbj generated code
//     against it to make sure it is compatible
    requires("com.google.protobuf.util")
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
}
