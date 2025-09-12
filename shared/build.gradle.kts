import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.ktlint)
  id("com.google.osdetector") version "1.7.3"
}

kotlin {
  jvmToolchain(17)
  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  jvm()

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser {
      val rootDirPath = project.rootDir.path
      val projectDirPath = project.projectDir.path
      commonWebpackConfig {
        devServer =
          (devServer ?: KotlinWebpackConfig.DevServer()).apply {
            static =
              (static ?: mutableListOf()).apply {
                // Serve sources to debug inside browser
                add(rootDirPath)
                add(projectDirPath)
              }
          }
      }
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.components.resources)
    }
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
      implementation(libs.android.youtubeplayer.core)
      implementation("io.github.kevinnzou:compose-webview:0.33.6")
    }
    jvmMain.dependencies {
      val fxSuffix = when (osdetector.classifier) {
        "linux-x86_64" -> "linux"
        "linux-aarch_64" -> "linux-aarch64"
        "windows-x86_64" -> "win"
        "osx-x86_64" -> "mac"
        "osx-aarch_64" -> "mac-aarch64"
        else -> throw IllegalStateException("Unknown OS: ${osdetector.classifier}")
      }
      implementation("org.openjfx:javafx-base:19:${fxSuffix}")
      implementation("org.openjfx:javafx-graphics:19:${fxSuffix}")
      implementation("org.openjfx:javafx-controls:19:${fxSuffix}")
      implementation("org.openjfx:javafx-swing:19:${fxSuffix}")
      implementation("org.openjfx:javafx-web:19:${fxSuffix}")
      implementation("org.openjfx:javafx-media:19:${fxSuffix}")
      implementation(libs.kotlinx.coroutines.swing)
    }
    wasmJsMain.dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
    }
  }
}

android {
  namespace = "com.aryamahasangh.shared"
  compileSdk = libs.versions.android.compileSdk.get().toInt()
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }
}

// ============================================================================
// KTLINT CONFIGURATION
// ============================================================================

ktlint {
  // Enable verbose output
  verbose.set(true)

  // Output to console
  outputToConsole.set(true)

  // Treat all Kotlin warnings as errors
  ignoreFailures.set(false)

  // Enable experimental rules
  enableExperimentalRules.set(false)

  // Trailing comma rules are now handled by .editorconfig
  // disabledRules.set(setOf("trailing-comma-on-call-site", "trailing-comma-on-declaration-site"))

  filter {
    exclude("**/generated/**")
    exclude("**/build/**")
    exclude("**/*.Generated.kt")
    exclude("**/ResourceCollectors/**")
    exclude("**/ActualResourceCollectors.kt")
    exclude("**/ExpectResourceCollectors.kt")
    exclude("**/Res.kt")
  }
}
