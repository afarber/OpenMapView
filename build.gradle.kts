// Auto-detect version from Git tag (e.g., "v0.2.0" -> "0.2.0")
fun getGitVersion(): String {
    val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    process.waitFor()
    val output = process.inputStream.bufferedReader().readText().trim()
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
    alias(libs.plugins.kotlin.android) apply false
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

    publishAllProjectsProbablyBreakingProjectIsolation()
}

