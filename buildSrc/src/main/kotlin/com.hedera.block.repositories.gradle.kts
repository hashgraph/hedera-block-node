// SPDX-License-Identifier: Apache-2.0
repositories {
    mavenCentral()
    maven {
        url = uri("https://hyperledger.jfrog.io/artifactory/besu-maven")
        content { includeGroupByRegex("org\\.hyperledger\\..*") }
    }
}
