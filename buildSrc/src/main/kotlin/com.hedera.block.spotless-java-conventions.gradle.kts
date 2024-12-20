// SPDX-License-Identifier: Apache-2.0
plugins { id("com.diffplug.spotless") }

spotless {
    java {
        targetExclude("build/generated/**/*.java", "build/generated/**/*.proto")
        // Enables the spotless:on and spotless:off comments
        toggleOffOn()
        // don't need to set target, it is inferred from java
        // apply a specific flavor of palantir-java-format
        // and do not format javadoc because the default setup
        // is _very_ bad for javadoc. We need to figure out a
        // "correct" _separate_ setup for that.
        palantirJavaFormat("2.50.0").formatJavadoc(false)
        // Fix some left-out items from the palantir plugin
        indentWithSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
        // make sure every file has the following SPDX header.
        // The delimiter override below is required to support some
        // of our test classes which are in the default package.
        licenseHeader(
            """
           // SPDX-License-Identifier: Apache-2.0
        """
                .trimIndent(),
            "(package|import)"
        )
    }
}
