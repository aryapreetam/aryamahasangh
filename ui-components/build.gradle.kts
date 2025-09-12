@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  jvmToolchain(17)

  androidTarget()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  jvm()

  wasmJs {
    browser()
    binaries.executable()
  }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.materialIconsExtended)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(compose.animation)

      // For coroutines and state management
    }

//    val jvmMain by getting {
//      dependencies {
//        implementation(compose.desktop.currentOs)
//      }
//    }
//
//    val wasmJsMain by getting {
//      dependencies {
//        // WASM specific dependencies if needed
//      }
//    }
  }
}

android {
  namespace = "com.aryamahasangh.uicomponents"
  compileSdk = libs.versions.android.compileSdk.get().toInt()
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }
}
