plugins {
    id("com.hedera.gradle.report.code-coverage")
}

dependencies {
    implementation(project(":server"))
    implementation(project(":simulator"))
    implementation(project(":suites"))
}
