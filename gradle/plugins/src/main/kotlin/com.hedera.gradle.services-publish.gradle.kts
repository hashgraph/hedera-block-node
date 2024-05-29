/*
 * Copyright (C) 2023-2024 Hedera Hashgraph, LLC
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
    id("java")
    id("com.hedera.gradle.maven-publish")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            pom.developers {
                developer {
                    name = "Hedera Base Team"
                    email = "hedera-base@swirldslabs.com"
                    organization = "Hedera Hashgraph"
                    organizationUrl = "https://www.hedera.com"
                }
                developer {
                    name = "Hedera Services Team"
                    email = "hedera-services@swirldslabs.com"
                    organization = "Hedera Hashgraph"
                    organizationUrl = "https://www.hedera.com"
                }
                developer {
                    name = "Hedera Smart Contracts Team"
                    email = "hedera-smart-contracts@swirldslabs.com"
                    organization = "Hedera Hashgraph"
                    organizationUrl = "https://www.hedera.com"
                }
                developer {
                    name = "Release Engineering Team"
                    email = "release-engineering@swirldslabs.com"
                    organization = "Hedera Hashgraph"
                    organizationUrl = "https://www.hedera.com"
                }
            }
        }
    }
}
