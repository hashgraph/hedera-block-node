rootProject.name = "hedera-block-node"

pluginManagement { includeBuild("gradle/plugins") }

plugins { id("com.hedera.gradle.settings") }

include("hedera-dependency-versions")
//include(":hapi", "hapi")
include(":block-node", "block-node")

fun include(name: String, path: String) {
    include(name)
    project(name).projectDir = File(rootDir, path)
}

fun includeAllProjects(containingFolder: String) {
    File(rootDir, containingFolder).listFiles()?.forEach { folder ->
        if (File(folder, "build.gradle.kts").exists()) {
            val name = ":${folder.name}"
            include(name)
            project(name).projectDir = folder
        }
    }
}

// The HAPI API version to use for Protobuf sources.
val hapiProtoVersion = "0.50.0"

dependencyResolutionManagement {
    // Protobuf tool versions
    versionCatalogs.create("libs") {
        version("google-proto", "3.19.4")
        version("grpc-proto", "1.45.1")
        version("hapi-proto", hapiProtoVersion)

        plugin("pbj", "com.hedera.pbj.pbj-compiler").version("0.8.9")
    }
}
