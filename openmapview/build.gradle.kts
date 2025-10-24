plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.afarber.openmapview"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("io.ktor:ktor-client-android:2.3.7")
}

apply(plugin = "com.diffplug.spotless")

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.3.1")
        trimTrailingWhitespace()
        endWithNewline()
        indentWithSpaces(4)
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
}

apply(plugin = "maven-publish")

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

    repositories {
        // Uncomment this to also publish to GitHub Packages
        /*
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/afarber/OpenMapView")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        */

        // Maven Central (via OSSRH)
        maven {
            name = "MavenCentral"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

apply(plugin = "signing")

signing {
    useInMemoryPgpKeys(
        System.getenv("SIGNING_KEY"),
        System.getenv("SIGNING_PASSWORD")
    )
    sign(publishing.publications)
}


