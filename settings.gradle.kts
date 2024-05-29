rootProject.name = "hedera-block-node"

pluginManagement { includeBuild("gradle/plugins") }

include("block-node")
include("hedera-dependency-versions")
//include("hapi")

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
include("gradle:plugins:plugins")
findProject(":gradle:plugins:plugins")?.name = "plugins"
