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
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
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
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
    // Desktop target has to add this repo. for compose-webview-multiplatform
    maven("https://jogamp.org/deployment/maven")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":composeApp")
include(":server")
include(":shared")