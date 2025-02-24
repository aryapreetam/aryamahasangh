import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeHotReload)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.apollo)
}

kotlin {
  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  jvm("desktop")

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    moduleName = "composeApp"
    browser {
      val rootDirPath = project.rootDir.path
      val projectDirPath = project.projectDir.path
      commonWebpackConfig {
        outputFileName = "composeApp.js"
        devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
          static = (static ?: mutableListOf()).apply {
            // Serve sources to debug inside browser
            add(rootDirPath)
            add(projectDirPath)
          }
        }
      }
    }
    binaries.executable()
  }

  sourceSets {
    val desktopMain by getting
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.materialIconsExtended)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodel)
      implementation(libs.androidx.lifecycle.runtime.compose)
      implementation(projects.shared)
      implementation(libs.coil.compose)
      implementation(libs.coil.network.ktor3)
      implementation(libs.navigation.compose)
      implementation(libs.kotlinx.serialization.json)
      implementation(compose.components.uiToolingPreview)

//      implementation("com.apollographql.apollo:apollo-runtime-kotlin:2.5.14")
      implementation("com.apollographql.apollo:apollo-runtime:4.1.1")
      implementation("com.apollographql.apollo:apollo-normalized-cache:4.1.1")
      implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

      implementation(libs.ktor.client.core)
    }
    androidMain.dependencies {
      implementation(compose.preview)
      implementation(libs.androidx.activity.compose)
//      implementation(libs.compose.ui.tooling.preview)
      implementation("androidx.compose.ui:ui:1.7.6")
      implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")

      implementation(libs.ktor.client.android)
    }
    iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
    }
    desktopMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutines.swing)
      implementation(libs.ui.tooling.preview.desktop)
      implementation(libs.ktor.client.java)
    }
    wasmJsMain.dependencies {
    }
  }
}

android {
  namespace = "org.aryamahasangh"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "org.aryamahasangh"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
   implementation(libs.androidx.ui.tooling.preview.android)
  debugImplementation(compose.uiTooling)
//  debugImplementation(libs.compose.ui.tooling)
  debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}

compose.desktop {
  application {
    mainClass = "org.aryamahasangh.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "org.aryamahasangh"
      packageVersion = "1.0.0"
    }
  }
}

tasks.register<ComposeHotRun>("runHot"){
  mainClass.set("org.aryamahasangh.MainKt")
}

apollo {
  service("service") {
    packageName.set("org.aryamahasangh")
    introspection {
      endpointUrl.set("http://localhost:4000/graphql")
      schemaFile.set(file("src/commonMain/graphql/schema.json"))
    }
  }
}