plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.spotless)
}

android {
    namespace = "de.afarber.openmapview.examples.views.overlays"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.afarber.openmapview.examples.views.overlays"
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
}

dependencies {
    implementation(project(":openmapview"))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}

spotless {
    java {
        target("src/**/*.java")
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
