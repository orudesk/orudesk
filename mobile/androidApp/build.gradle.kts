// The Android app (Day 74): Jetpack Compose screens over the shared module's logic.
// AGP 9 compiles Kotlin by itself, so there's no separate Kotlin Android plugin; Compose needs its compiler plugin.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.orudesk.mobile.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.orudesk.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        // The emulator reaches the computer it runs on at 10.0.2.2: the API from `docker compose` is on 8081
        buildConfigField("String", "API_BASE_URL", "\"${project.findProperty("orudesk.apiBaseUrl") ?: "http://10.0.2.2:8081"}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)
}
