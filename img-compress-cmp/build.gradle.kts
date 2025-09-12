
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  jvmToolchain(17)

  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  // Desktop JVM target
  jvm("desktop")

  // iOS targets
  val iosX64 = iosX64()
  val iosArm64 = iosArm64()
  val iosSimArm64 = iosSimulatorArm64()

  listOf(iosX64, iosArm64, iosSimArm64).forEach { t ->
    t.binaries.framework {
      baseName = "ImgCompressCmp"
      isStatic = true
    }
  }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser {
      testTask {
        // Use default browser test runner from root project configuration
      }
    }
    binaries.executable()
  }

  // Toggle native interop via -PimgCompress.enableNative=true
  val enableNativeInterop = (project.findProperty("imgCompress.enableNative") as String?)?.toBoolean() ?: false

  // Self-contained native interop for iOS: vendor libwebp headers and static libs inside this module
  // Directory structure expected:
  // - src/nativeInterop/cinterop/include/webp/*.h
  // - src/nativeInterop/cinterop/libs/ios/iphoneos/libwebp.a
  // - src/nativeInterop/cinterop/libs/ios/iphonesimulator/libwebp.a
  targets.withType<KotlinNativeTarget>().configureEach {
    if (konanTarget.family == Family.IOS && enableNativeInterop) {
      val isDevice = (konanTarget == KonanTarget.IOS_ARM64)
      val platformDir = if (isDevice) "iphoneos" else "iphonesimulator"
      val libsDir = project.file("src/nativeInterop/cinterop/libs/ios/${platformDir}").absolutePath
      val includeDir = project.file("src/nativeInterop/cinterop/include").absolutePath

      compilations.getByName("main").cinterops.create("libwebp") {
        defFile(project.file("src/nativeInterop/cinterop/libwebp.def"))
        // Use vendored headers under include/webp
        compilerOpts("-I$includeDir")
      }

      // Ensure final test/main binaries link transitively without consumer config
      binaries.all {
        // Link against the static lib for current target
        linkerOpts("-L$libsDir", "-lwebp")
      }
    } else if (konanTarget.family == Family.IOS && !enableNativeInterop) {
      project.logger.lifecycle("img-compress-cmp: Native interop disabled for ${konanTarget}. Set -PimgCompress.enableNative=true to enable.")
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

  // Select iOS source directory based on interop toggle without relying on iosMain availability
  listOf(iosX64, iosArm64, iosSimArm64).forEach { t ->
    t.compilations.getByName("main").defaultSourceSet.kotlin.srcDirs(
      if (enableNativeInterop) listOf("src/iosMainNative/kotlin") else listOf("src/iosMainStub/kotlin")
    )
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
