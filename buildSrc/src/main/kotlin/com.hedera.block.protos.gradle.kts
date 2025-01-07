// SPDX-License-Identifier: Apache-2.0
plugins {
    id("java")
    id("com.google.protobuf")
    id("com.hedera.block.conventions")
}

sourceSets { main { proto { srcDir("src/main/protobuf") } } }

protobuf {
    // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:3.21.10"
    }
}
