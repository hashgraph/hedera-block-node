// SPDX-License-Identifier: Apache-2.0
import io.github.gradlenexus.publishplugin.CloseNexusStagingRepository

plugins {
    id("com.hedera.block.repositories")
    id("com.hedera.block.aggregate-reports")
    id("com.hedera.block.spotless-conventions")
    id("com.hedera.block.spotless-kotlin-conventions")
    id("com.autonomousapps.dependency-analysis")
    id("io.github.gradle-nexus.publish-plugin")
}

group = "com.hedera.block"

spotless { kotlinGradle { target("buildSrc/**/*.gradle.kts") } }

nexusPublishing {
    repositories {
        sonatype {
            username = System.getenv("OSSRH_USERNAME")
            password = System.getenv("OSSRH_PASSWORD")
        }
    }
}

tasks.withType<CloseNexusStagingRepository> {
    // The publishing of all components to Maven Central (in this case only 'pbj-runtime') is
    // automatically done before close (which is done before release).
    dependsOn(":server:publishToSonatype")
}

tasks.register("release") {
    group = "release"
    dependsOn(tasks.closeAndReleaseStagingRepository)
}

tasks.register("releaseSnapshot") {
    group = "release"
    dependsOn(":server:publishToSonatype")
}
