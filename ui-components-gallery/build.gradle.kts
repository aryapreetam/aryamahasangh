plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  jvmToolchain(11)

  jvm("desktop")

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
      implementation(project(":ui-components"))
      implementation(libs.navigation.compose)
    }

    val desktopMain by getting {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.kotlinx.coroutines.swing)
      }
    }

    val wasmJsMain by getting {
      dependencies {
      }
    }
  }
}

compose.desktop {
  application {
    mainClass = "org.aryamahasangh.gallery.MainKt"

    nativeDistributions {
      targetFormats(
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
      )
      packageName = "UI Components Gallery"
      packageVersion = "1.0.0"
    }
  }
}
