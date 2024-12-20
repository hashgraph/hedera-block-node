// SPDX-License-Identifier: Apache-2.0
plugins {
    id("java-library")
    id("com.hedera.block.common")
}

description = "Commons module with logic that could be abstracted and reused."

testModuleInfo {
    requiresStatic("com.github.spotbugs.annotations")
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.assertj.core")
}
