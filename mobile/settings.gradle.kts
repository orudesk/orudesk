// The mobile apps (Week 15): Kotlin Multiplatform logic shared by Android and iOS, with a native UI on each.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "orudesk-mobile"

include(":shared", ":androidApp")
