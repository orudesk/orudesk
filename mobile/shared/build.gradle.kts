// The logic both apps share (Day 71): talking to the API, logging in (with the two-factor step) and the session.
// No UI here: each platform draws its own screens (Compose on Android, SwiftUI on iOS).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    // A plain JVM target too: the tests run on it, quickly, without an emulator
    jvm()

    android {
        namespace = "com.orudesk.mobile.shared"
        compileSdk = 36
        minSdk = 26
    }

    // Built on a Mac only; on Linux (CI) these targets are skipped
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "OrudeskShared" }
    }

    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.serialization.json)
            api(libs.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
