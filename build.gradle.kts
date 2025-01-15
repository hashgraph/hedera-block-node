// SPDX-License-Identifier: Apache-2.0
import net.swiftzer.semver.SemVer

tasks.versionAsSpecified {
    val chartFiles =
        fileTree(rootDir) {
            include("charts/**/Chart.yaml")
            exclude("**/node_modules/")
        }
    doLast {
        val newVersion = SemVer.parse(inputs.properties["newVersion"] as String).toString()
        chartFiles.forEach { file ->
            val yaml = file.readText()
            val oldVersion = Regex("(?<=^(appVersion|version): ).+", RegexOption.MULTILINE)
            file.writeText(yaml.replace(oldVersion, newVersion))
        }
    }
}
