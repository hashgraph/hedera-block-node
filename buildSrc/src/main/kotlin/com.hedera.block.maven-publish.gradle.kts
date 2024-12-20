// SPDX-License-Identifier: Apache-2.0
plugins {
    id("java")
    id("maven-publish")
    id("signing")
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            packaging = findProperty("maven.project.packaging")?.toString() ?: "jar"
            name.set(project.name)
            url.set("https://www.hedera.com/")
            inceptionYear.set("2024")

            description.set(
                "An implementation of a Block Node for the Hedera public distributed ledger."
            )

            organization {
                name.set("Hedera Hashgraph, LLC")
                url.set("https://www.hedera.com")
            }

            licenses {
                license {
                    name.set("Apache 2.0 License")
                    url.set("https://raw.githubusercontent.com/hashgraph/pbj/main/LICENSE")
                }
            }

            developers {
                developer {
                    name.set("Jasper Potts")
                    email.set("jasper.potts@swirldslabs.com")
                    organization.set("Swirlds Labs, Inc.")
                    organizationUrl.set("https://www.swirldslabs.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/hashgraph/hedera-block-node.git")
                developerConnection.set("scm:git:ssh://github.com:hashgraph/hedera-block-node.git")
                url.set("https://github.com/hashgraph/hedera-block-node")
            }
        }
    }
}

signing { useGpgCmd() }

tasks.withType<Sign> {
    onlyIf { providers.gradleProperty("publishSigningEnabled").getOrElse("false").toBoolean() }
}
