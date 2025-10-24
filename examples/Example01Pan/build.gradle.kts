plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.diffplug.spotless")
}

android {
    namespace = "de.afarber.openmapview.example01pan"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.afarber.openmapview.example01pan"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }
}

dependencies {
    implementation(project(":openmapview"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui-viewbinding:1.7.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}

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
