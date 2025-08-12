
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.composeCompiler)
  kotlin("native.cocoapods")
}

kotlin {
  jvmToolchain(11)

  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  // Desktop JVM target
  jvm("desktop")

  // iOS targets
  iosX64()
  iosArm64()
  iosSimulatorArm64()

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser {
      testTask {
        // Use default browser test runner from root project configuration
      }
    }
    binaries.executable()
  }

  cocoapods {
    // Keep defaults consistent with the project
    summary = "KMP image compression to WebP"
    homepage = "https://github.com/aryapreetam/img-compress-cmp"
    version = "1.0.0"
    ios.deploymentTarget = "14.0"
    framework {
      baseName = "ImgCompressCmp"
      isStatic = true
    }
    pod("libwebp") {
      version = "1.3.2"
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")
        implementation(compose.runtime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")
      }
    }

    val desktopMain by getting {
      dependencies {
        // Pull in Skia via Compose Desktop runtime for WebP encoding
        implementation(compose.desktop.currentOs)
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")
      }
    }

    val wasmJsMain by getting {
      dependencies {
        // No heavy deps; use DOM/Web APIs via Kotlin/Wasm interop
      }
    }
  }
}

android {
  namespace = "com.aryamahasangh.imgcompress"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
    testOptions {
      targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
  }

  buildFeatures {
    buildConfig = false
  }

  packaging {
    resources {
      // Avoid duplicate licenses/resources
      excludes += setOf(
        "META-INF/LICENSE*",
        "META-INF/AL2.0",
        "META-INF/LGPL2.1"
      )
    }
  }
}
