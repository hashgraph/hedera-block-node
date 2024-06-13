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

import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    id("java")
    id("jacoco")
    id("org.gradlex.java-module-dependencies")
    id("com.adarshr.test-logger")
    id("com.hedera.block.repositories")
    id("com.hedera.block.jpms-modules")
    id("com.hedera.block.spotless-conventions")
    id("com.hedera.block.spotless-java-conventions")
    id("com.hedera.block.spotless-kotlin-conventions")
    id("com.hedera.block.maven-publish")
}

group = "com.hedera.block"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

    // Enable JAR file generation required for publishing
    withJavadocJar()
    withSourcesJar()
}

testing {
    @Suppress("UnstableApiUsage") suites.getByName<JvmTestSuite>("test") { useJUnitJupiter() }
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    fileMode = 436 // octal: 0664
    dirMode = 509 // octal: 0775
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).tags(
        "apiNote:a:API Note:",
        "implSpec:a:Implementation Requirements:",
        "implNote:a:Implementation Note:"
    )
}

testlogger {
    theme = ThemeType.MOCHA
    slowThreshold = 10000
    showStandardStreams = true
    showPassedStandardStreams = false
    showSkippedStandardStreams = false
    showFailedStandardStreams = true
}

// Ensure JaCoCo coverage is generated and aggregated
tasks.jacocoTestReport.configure {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val testExtension = tasks.test.get().extensions.getByType<JacocoTaskExtension>()
    executionData.setFrom(testExtension.destinationFile)

    shouldRunAfter(tasks.named("check"))
}

tasks.check {
    // Ensure the check task also runs the JaCoCo coverage report
    dependsOn(tasks.jacocoTestReport)
    // Check dependency scopes in module-info.java
    dependsOn(tasks.checkAllModuleInfo)
}