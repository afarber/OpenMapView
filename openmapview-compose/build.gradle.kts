plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.spotless)
    id("maven-publish")
    // Collects the publications for the nmcp aggregation in the root project
    id("com.gradleup.nmcp")
    id("signing")
}

android {
    namespace = "de.afarber.openmapview.compose"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    buildFeatures {
        buildConfig = false
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        // Dependency updates come from Dependabot; these checks need network access and fail at random
        disable += setOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    // The library is part of the public API (OpenMapView, CameraPosition, CameraUpdate)
    api(project(":openmapview"))
    // OpenMap takes a Modifier
    api(libs.androidx.compose.ui)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Only for testing the map inside a scrolling Column
    testImplementation(libs.androidx.compose.foundation)
    // Provides the empty activity that createComposeRule() starts
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
        leadingTabsToSpaces(4)
        licenseHeaderFile(rootProject.file("spotless.license.kt"), "(package|import)")
    }
    kotlinGradle {
        target("*.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "de.afarber"
            artifactId = "openmapview-compose"
            version = rootProject.ext["libVersion"] as String

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("OpenMapView Compose")
                description.set("Jetpack Compose wrapper for OpenMapView, an OpenStreetMap MapView for Android.")
                url.set("https://github.com/afarber/OpenMapView")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("afarber")
                        name.set("Alexander Farber")
                        url.set("https://afarber.de")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/afarber/OpenMapView.git")
                    developerConnection.set("scm:git:ssh://github.com/afarber/OpenMapView.git")
                    url.set("https://github.com/afarber/OpenMapView")
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("SIGNING_KEY"),
        System.getenv("SIGNING_PASSWORD"),
    )
    sign(publishing.publications)
}
