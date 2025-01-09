// SPDX-License-Identifier: Apache-2.0
plugins { id("org.hiero.gradle.build") version "0.1.2" }

rootProject.name = "hedera-block-node"

javaModules {
    directory(".") {
        group = "com.hedera.block"
        module("suites") // no 'module-info' yet
    }
}
