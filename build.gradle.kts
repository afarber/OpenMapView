// Auto-detect version from Git tag (e.g., "v0.2.0" -> "0.2.0")
fun getGitVersion(): String {
    val process = Runtime.getRuntime().exec("git describe --tags --abbrev=0")
    val output = process.inputStream.bufferedReader().readText().trim()
    val cleanVersion = output.removePrefix("v")
    return if (cleanVersion.isNotEmpty()) cleanVersion else "0.0.1-SNAPSHOT"
}

ext["libVersion"] = try {
    getGitVersion()
} catch (e: Exception) {
    println("No Git tag found; using 0.0.1-SNAPSHOT")
    "0.0.1-SNAPSHOT"
}

plugins {
    id("com.android.library") version "8.7.0" apply false
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
}

