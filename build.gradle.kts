// Auto-detect version from Git tag (e.g., "v0.2.0" -> "0.2.0")
fun getGitVersion(): String {
    val output =
        providers
            .exec {
                commandLine("git", "describe", "--tags", "--abbrev=0")
                isIgnoreExitValue = true
            }.standardOutput.asText
            .get()
            .trim()
    val cleanVersion = output.removePrefix("v")
    return cleanVersion.ifEmpty { "0.0.1-SNAPSHOT" }
}

ext["libVersion"] = try {
    getGitVersion()
} catch (e: Exception) {
    println("No Git tag found; using 0.0.1-SNAPSHOT")
    "0.0.1-SNAPSHOT"
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.nmcp)
}

nmcpAggregation {
    centralPortal {
        username = System.getenv("OSSRH_USERNAME") ?: ""
        password = System.getenv("OSSRH_PASSWORD") ?: ""
        publishingType = "AUTOMATIC"
    }
}

dependencies {
    // Only the libraries are published; the example apps are not
    nmcpAggregation(project(":openmapview"))
    nmcpAggregation(project(":openmapview-compose"))
}
