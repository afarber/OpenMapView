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
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // future tile fetching
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

