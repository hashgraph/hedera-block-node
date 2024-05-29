plugins {
    id("com.hedera.gradle.versions")
}

//repositories {
//    mavenCentral()
//}

dependencies.constraints {
    org.gradle.api.artifacts.dsl.DependencyConstraintHandler.api()
    api("com.hedera.pbj:pbj-runtime:0.8.9") {
        because("com.hedera.pbj.runtime")
    }
}
