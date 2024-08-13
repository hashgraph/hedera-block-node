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

dependencies {
    implementation("io.grpc:grpc-protobuf:1.64.0")
    implementation("io.grpc:grpc-stub:1.64.0")
//    implementation("com.hedera.pbj:pbj-runtime:0.8.9")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    implementation("com.google.guava:guava:33.0.0-jre")

    // explicit in services config
//    implementation("com.google.api.grpc:proto-google-common-protos:2.29.0")
//    implementation("io.grpc:grpc-api:1.64.0")
    implementation("com.github.spotbugs:spotbugs-annotations:4.7.3")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    // annotations?
    implementation("org.checkerframework:checker-qual:3.41.0")
    implementation("com.google.errorprone:error_prone_annotations:2.23.0")

//    implementation("com.google.guava:failureaccess:1.0.2")
//    implementation("com.google.protobuf:protobuf-java:3.25.1")
//    implementation("com.google.j2objc:j2objc-annotations:2.8")

    // breaks every time
//    > Failed to transform javax.annotation-api-1.3.2.jar (javax.annotation:javax.annotation-api:1.3.2) to match attributes {artifactType=jar, javaModule=true, org.gradle.category=library, org.gradle.libraryelements=jar, org.gradle.status=release, org.gradle.usage=java-api}.
//    > Execution failed for ExtraJavaModuleInfoTransform: /Users/mattpeterson/.gradle/caches/modules-2/files-2.1/javax.annotation/javax.annotation-api/1.3.2/934c04d3cfef185a8008e7bf34331b79730a9d43/javax.annotation-api-1.3.2.jar.
//    > Found an automatic module: java.annotation (javax.annotation-api-1.3.2.jar)
//    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // old?
//    > Failed to transform guava-32.1.3-jre.jar (com.google.guava:guava:32.1.3-android) to match attributes {artifactType=jar, javaModule=true, org.gradle.category=library, org.gradle.dependency.bundling=external, org.gradle.jvm.environment=standard-jvm, org.gradle.jvm.version=8, org.gradle.libraryelements=jar, org.gradle.status=release, org.gradle.usage=java-api}.
//    > Execution failed for ExtraJavaModuleInfoTransform: /Users/mattpeterson/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/32.1.3-jre/f306708742ce2bf0fb0901216183bc14073feae/guava-32.1.3-jre.jar.
//    > [requires directives from metadata] Cannot find dependencies for 'com.google.common'. Are 'com.google.guava:guava' the correct component coordinates?
//    implementation("com.google.guava:guava:31.1-jre")
}


//dependencies.constraints {
//    implementation("javax.annotation:javax.annotation-api:1.3.2")
//}

//testModuleInfo {
//    requires("com.hedera.node.hapi")
    // we depend on the protoc compiled hapi during test as we test our pbj generated code
    // against it to make sure it is compatible
//    requires("com.google.protobuf.util")
//    requires("org.junit.jupiter.api")
//    requires("org.junit.jupiter.params")
//}
