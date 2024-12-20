// SPDX-License-Identifier: Apache-2.0
plugins {
    id("application")
    id("com.hedera.block.conventions")
    id("me.champeau.jmh")
}

val maven = publishing.publications.create<MavenPublication>("maven") { from(components["java"]) }

signing.sign(maven)
