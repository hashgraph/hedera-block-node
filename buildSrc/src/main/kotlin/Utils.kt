// SPDX-License-Identifier: Apache-2.0
import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

class Utils {
    companion object {
        @JvmStatic
        fun updateVersion(project: Project, newVersion: SemVer) {
            val gradlePropFile = File(project.projectDir, "gradle.properties")
            var lines: List<String> = mutableListOf()

            if (gradlePropFile.exists()) {
                lines = gradlePropFile.readLines(Charsets.UTF_8)
            }

            var versionStr = "version=$newVersion"
            val finalLines: List<String>


            if (lines.isNotEmpty()) {
                finalLines = lines.map {
                    if (it.trimStart().startsWith("version=")) {
                        versionStr
                    } else {
                        it
                    }
                }
            } else {
                finalLines = listOf(versionStr)
            }


            gradlePropFile.bufferedWriter(Charsets.UTF_8).use {
                val writer = it
                finalLines.forEach {
                    writer.write(it)
                    writer.newLine()
                }
                writer.flush()
            }
        }

        @JvmStatic
        fun generateProjectVersionReport(rootProject: Project, ostream: OutputStream) {
            val writer = PrintStream(ostream, false, Charsets.UTF_8)

            ostream.use {
                writer.use {
                    // Writer headers
                    writer.println("### Deployed Version Information")
                    writer.println()
                    writer.println("| Artifact Name | Version Number |")
                    writer.println("| --- | --- |")

                    // Write table rows
                    rootProject.childProjects.values.onEach {
                        writer.printf("| %s | %s |\n", it.name, it.version.toString())
                    }
                    writer.flush()
                    ostream.flush()
                }
            }
        }
    }
}
