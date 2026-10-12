plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.license.report)
    id("maven-publish")
    // Collects the publications for the nmcp aggregation in the root project
    id("com.gradleup.nmcp")
    id("signing")
    // Only for the Robolectric agent setting below; AGP creates the coverage report
    id("jacoco")
}

android {
    namespace = "de.afarber.openmapview"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = false
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
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
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

// Generates LibraryVersion.kt so the User-Agent always carries the published version (from the Git tag)
@CacheableTask
abstract class GenerateLibraryVersionTask : DefaultTask() {
    @get:Input
    abstract val libraryVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val file = outputDir.file("de/afarber/openmapview/LibraryVersion.kt").get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package de.afarber.openmapview
            |
            |internal const val LIBRARY_VERSION = "${libraryVersion.get()}"
            |
            |internal const val USER_AGENT = "OpenMapView/${'$'}LIBRARY_VERSION (https://github.com/afarber/OpenMapView)"
            |
            """.trimMargin(),
        )
    }
}

val generateLibraryVersion =
    tasks.register<GenerateLibraryVersionTask>("generateLibraryVersion") {
        libraryVersion.set(rootProject.ext["libVersion"] as String)
    }

androidComponents {
    onVariants { variant ->
        variant.sources.java?.addGeneratedSourceDirectory(
            generateLibraryVersion,
            GenerateLibraryVersionTask::outputDir,
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.ktor.client.android)
    implementation(libs.disklrucache)

    // Unit testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    // Robolectric enables Android framework classes (Bitmap, Canvas, etc.) to work in JVM unit tests
    // without requiring an emulator or device. It provides shadow implementations of Android APIs.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)

    // Instrumentation testing
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.junit.ktx)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
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
        showStandardStreams = true
    }
    // Count classes loaded by Robolectric's instrumenting class loader (AGP coverage
    // reports about 15% instead of 44% without this)
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "de.afarber"
            artifactId = "openmapview"
            version = rootProject.ext["libVersion"] as String

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("OpenMapView")
                description.set("A modern, Kotlin-first MapView replacement for Android powered by OpenStreetMap.")
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

    // Note: Repository configuration is handled by the nmcp plugin in root build.gradle.kts
    // The nmcp plugin uploads publications to Central Portal using their API
}

signing {
    useInMemoryPgpKeys(
        System.getenv("SIGNING_KEY"),
        System.getenv("SIGNING_PASSWORD"),
    )
    sign(publishing.publications)
}

licenseReport {
    // Include only runtime dependencies (exclude test deps)
    configurations = arrayOf("releaseRuntimeClasspath")

    // Generate HTML report
    renderers =
        arrayOf(
            com.github.jk1.license.render
                .InventoryHtmlReportRenderer("licenses.html", "Dependency Licenses"),
        )

    // Output directory
    outputDir = "${layout.buildDirectory.get()}/reports/licenses"
}

dokka {
    moduleName.set("OpenMapView")
    moduleVersion.set(rootProject.ext["libVersion"] as String)

    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public,
                org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Protected,
            ),
        )

        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl.set(uri("https://github.com/afarber/OpenMapView/tree/main/openmapview/src/main/kotlin"))
            remoteLineSuffix.set("#L")
        }

        perPackageOption {
            matchingRegex.set(".*\\.internal.*")
            suppress.set(true)
        }

        enableAndroidDocumentationLink.set(true)
    }
}
