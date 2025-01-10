// SPDX-License-Identifier: Apache-2.0
plugins { id("org.hiero.gradle.build") version "0.2.0" }

rootProject.name = "hedera-block-node"

javaModules {
    directory(".") {
        group = "com.hedera.block"
        module("tools") // no 'module-info' yet
    }
}
