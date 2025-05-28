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
}

kotlin {
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
    }
    jvmMain.dependencies {
      implementation(libs.compose.webview.multiplatform.desktop)
    }
    wasmJsMain.dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
    }
  }
}

android {
  namespace = "org.aryamahasangh.shared"
  compileSdk = libs.versions.android.compileSdk.get().toInt()
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }
}
