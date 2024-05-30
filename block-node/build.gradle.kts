/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
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
    id("com.hedera.gradle.blocknode")
}

description = "Hedera Block Node"

//testModuleInfo {
//    requires("com.hedera.node.hapi")
//    // we depend on the protoc compiled hapi during test as we test our pbj generated code
//    // against it to make sure it is compatible
//    requires("com.google.protobuf.util")
//    requires("org.junit.jupiter.api")
//    requires("org.junit.jupiter.params")
//}
