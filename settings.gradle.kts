pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EdgeGate"

// :core  -> pure Kotlin/JVM: plate parsing, multi-frame voting, YOLO decoding, gate rules.
//           No Android dependency, so it is unit-tested on the JVM in milliseconds.
// :app   -> Android app: CameraX + LiteRT (TFLite) + ML Kit + Room + Jetpack Compose.
include(":core")
include(":app")
