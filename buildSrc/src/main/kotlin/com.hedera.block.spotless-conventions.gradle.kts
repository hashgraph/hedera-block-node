// SPDX-License-Identifier: Apache-2.0
plugins { id("com.diffplug.spotless") }

spotless {
    // Disable strong enforcement during gradle check tasks
    isEnforceCheck = false

    // optional: limit format enforcement to just the files changed by this feature branch
    ratchetFrom("origin/main")

    format("misc") {
        // define the files to apply `misc` to
        target("*.gradle", "*.md", ".gitignore")

        // define the steps to apply to those files
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }

    format("actionYaml") {
        target(".github/workflows/*.yaml")

        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()

        licenseHeader(
            """
            # SPDX-License-Identifier: Apache-2.0
        """
                .trimIndent(),
            "(name|on)"
        )
    }
}
