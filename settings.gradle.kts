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
include(":examples:Example04Polylines")
include(":examples:Example05Camera")
include(":examples:Example06Clicks")
include(":examples:Example07DraggableMarkers")
include(":examples:Example08Circles")
include(":examples:Example09Overlays")
include(":examples:Example10GroundOverlays")
include(":examples:Example11MapTypes")
