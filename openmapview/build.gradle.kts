plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.diffplug.spotless")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
    id("jacoco")
}

android {
    namespace = "de.afarber.openmapview"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
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

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("com.jakewharton:disklrucache:2.0.2")

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.mockk:mockk:1.13.8")
    // Robolectric enables Android framework classes (Bitmap, Canvas, etc.) to work in JVM unit tests
    // without requiring an emulator or device. It provides shadow implementations of Android APIs.
    testImplementation("org.robolectric:robolectric:4.14")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.ktor:ktor-client-mock:2.3.7")

    // Instrumentation testing
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.3.1")
        trimTrailingWhitespace()
        endWithNewline()
        leadingTabsToSpaces(4)
        licenseHeaderFile(rootProject.file("spotless.license.kt"), "(package|import)")
    }
    kotlinGradle {
        target("*.kts")
        ktlint("1.3.1")
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
    finalizedBy("jacocoTestReport")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
        )

    val debugTree =
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    val mainSrc = "${project.projectDir}/src/main/kotlin"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")

    violationRules {
        rule {
            limit {
                minimum = "0.50".toBigDecimal()
            }
        }
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
        )

    val debugTree =
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )
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

dokka {
    moduleName.set("OpenMapView")
    moduleVersion.set(rootProject.ext["libVersion"] as String)

    dokkaSourceSets.main {
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
