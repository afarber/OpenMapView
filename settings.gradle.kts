pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Must differ from ":openmapview" (case-insensitively), which nmcp rejects as a duplicate project name
rootProject.name = "OpenMapView-root"

include(":openmapview")
include(":openmapview-compose")
// Every folder under examples/compose and examples/views with a build file is an example app,
// e.g. examples/views/Pan becomes :examples:views:Pan
listOf("compose", "views").forEach { kind ->
    file("examples/$kind")
        .listFiles { dir -> dir.resolve("build.gradle.kts").isFile }
        .orEmpty()
        .sortedBy { it.name }
        .forEach { include(":examples:$kind:${it.name}") }
}
