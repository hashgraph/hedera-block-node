import org.gradle.api.internal.artifacts.dsl.dependencies.DependenciesExtensionModule.module

/*
 * Copyright (C) 2016-2024 Hedera Hashgraph, LLC
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
    id("org.gradlex.jvm-dependency-conflict-resolution")
    id("org.gradlex.extra-java-module-info")
}

// Fix or enhance the metadata of third-party Modules. This is about the metadata in the
// repositories: '*.pom' and '*.module' files.
jvmDependencyConflicts.patch {
//    val grpcModule = "io.helidon.grpc:io.grpc"
//    val grpcComponents = listOf("io.grpc:grpc-api", "io.grpc:grpc-context", "io.grpc:grpc-core")
    val annotationLibraries =
        listOf(
            "com.google.guava:listenablefuture"
        )

    module("com.google.guava:guava") {
        annotationLibraries.forEach {
            removeDependency(it)
        }
    }

}

// Fix or enhance the 'module-info.class' of third-party Modules. This is about the
// 'module-info.class' inside the Jar files. In our full Java Modules setup every
// Jar needs to have this file. If it is missing, it is added by what is configured here.
extraJavaModuleInfo {
    failOnAutomaticModules = false // Only allow Jars with 'module-info' on all module paths

    module("com.google.api.grpc:proto-google-common-protos", "com.google.api.grpc.common")
    module("com.google.protobuf:protobuf-java", "com.google.protobuf") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("com.google.code.findbugs:jsr305", "java.annotation") {
        exportAllPackages()
//        mergeJar("javax.annotation:javax.annotation-api")
    }
    module("com.google.guava:failureaccess", "com.google.common.util.concurrent.internal")
    module("com.google.j2objc:j2objc-annotations", "com.google.j2objc.annotations")
//    module("com.google.guava.listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
//        "maven.com.google.guava.guava.listenablefuture",)
}