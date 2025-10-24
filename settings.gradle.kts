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

rootProject.name = "OpenMapView"

include(":openmapview")
include(":examples:Example01Pan")
include(":examples:Example02Zoom")
include(":examples:Example03Markers")
