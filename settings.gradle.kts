rootProject.name = "AryaMahasangh"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    google {
      mavenContent {
        includeGroupAndSubgroups("androidx")
        includeGroupAndSubgroups("com.android")
        includeGroupAndSubgroups("com.google")
      }
    }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
    google()
    gradlePluginPortal()
    mavenCentral()
    // Desktop target has to add this repo. for compose-webview-multiplatform
    maven("https://jogamp.org/deployment/maven")
  }
}

dependencyResolutionManagement {
  repositories {
    google {
      mavenContent {
        includeGroupAndSubgroups("androidx")
        includeGroupAndSubgroups("com.android")
        includeGroupAndSubgroups("com.google")
      }
    }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
    mavenCentral()
    // Desktop target has to add this repo. for compose-webview-multiplatform
    maven("https://jogamp.org/deployment/maven")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

include(":composeApp")
// include(":server")
include(":shared")
include(":ui-components")
include(":ui-components-gallery")
