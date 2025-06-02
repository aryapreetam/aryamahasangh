import com.apollographql.apollo.gradle.internal.ApolloGenerateSourcesTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.util.*

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeHotReload)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.apollo)
  alias(libs.plugins.ktlint)
}

// Load secrets from properties file
fun loadSecrets(): Properties {
  val secretsFile = rootProject.file("secrets.properties")
  val secrets = Properties()

  if (secretsFile.exists()) {
    secretsFile.inputStream().use { secrets.load(it) }
  } else {
    println("Warning: secrets.properties file not found. Using fallback values.")
  }

  return secrets
}

val secrets = loadSecrets()
val environment = secrets.getProperty("environment", "dev")
val supabaseUrl = secrets.getProperty("$environment.supabase.url", "")
val supabaseKey = secrets.getProperty("$environment.supabase.key", "")

kotlin {

  jvmToolchain(11)

  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
    // to run instrumented (emulator) tests for Android
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
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
    outputModuleName.set("composeApp")
    browser {
      val rootDirPath = project.rootDir.path
      val projectDirPath = project.projectDir.path
      commonWebpackConfig {
        outputFileName = "composeApp.js"
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
      testTask {
        useKarma {
          useChrome()
          useConfigDirectory(project.projectDir.resolve("karma.config.d").apply {
            mkdirs()
          })
        }
      }
    }
    binaries.executable()
  }

  sourceSets {
    val desktopMain by getting
    val desktopTest by getting
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
      implementation(libs.material.adaptive)

      // Koin for Dependency Injection
      implementation(libs.koin.core)
      implementation(libs.koin.compose)

      implementation(libs.kotlinx.datetime)

      implementation(libs.ktor.client.core)

      implementation(libs.compose.remember.setting)

      // filekit
      // Enables FileKit without Compose dependencies
      implementation("io.github.vinceglb:filekit-core:0.8.8")
      // Enables FileKit with Composable utilities
      implementation("io.github.vinceglb:filekit-compose:0.8.8")

      // supabase
      implementation(project.dependencies.platform(libs.supabase.bom)) // 👈 BOM for consistent versions
      implementation(libs.supabase.postgrest)
      implementation(libs.supabase.auth)
      implementation(libs.supabase.realtime)
      implementation(libs.supabase.storage)
      implementation(libs.supabase.apollographql)
      implementation(libs.supabase.coil)

      // apollo adapters
      implementation(libs.apollo.adapters.core)
      implementation(libs.apollo.adapters.kotlinx.datetime)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
      implementation(compose.uiTest)
    }
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
      implementation(libs.compose.ui.tooling.preview)
      implementation("androidx.compose.ui:ui:${libs.versions.compose.android.get()}")
      implementation(libs.ktor.client.android)
      implementation(libs.koin.androidx.compose)
    }
    iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
    }
    desktopMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutines.swing)
      implementation(libs.ktor.client.java)
    }
    desktopTest.dependencies {
      implementation(compose.desktop.currentOs)
    }
    wasmJsMain.dependencies {
      implementation(libs.ktor.client.wasm)
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

    // Use environment variables for version, fallback to defaults
    // During CI builds, VERSION_NAME and VERSION_CODE are set by the workflow
    // During local development, defaults are used (0.0.1 and 1)
    versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
    versionName = System.getenv("VERSION_NAME") ?: "0.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Expose version information to the app via BuildConfig
    buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
    buildConfigField("int", "VERSION_CODE", "$versionCode")

    // Debug output for development
    println("📱 Android version: $versionName ($versionCode)")
  }

  buildFeatures {
    buildConfig = true
  }

  packaging {
    resources {
      excludes +=
        setOf(
          "META-INF/INDEX.LIST",
          "META-INF/io.netty.versions.properties"
        )
    }
  }
  signingConfigs {
    create("release") {
      storeFile = file("../aryamahasangh.jks")
      storePassword = System.getenv("KEYSTORE_PASSWORD")
      keyAlias = "keystore"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        file("proguard-rules.pro")
      )
      signingConfig = signingConfigs.getByName("release")
    }
  }
}

dependencies {
  implementation(libs.androidx.material3.android)
  // implementation(project(":server"))
  debugImplementation(compose.uiTooling)

  androidTestImplementation("androidx.compose.ui:ui-test-junit4-android:1.6.8")
  debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")
}

compose.desktop {
  application {
    mainClass = "org.aryamahasangh.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "org.aryamahasangh"
      packageVersion = "1.0.0"
      linux {
        modules("jdk.security.auth")
      }
    }
  }
}

tasks.register<ComposeHotRun>("runHot") {
  mainClass.set("org.aryamahasangh.MainKt")
}

apollo {
  service("service") {
    packageName.set("org.aryamahasangh")
    mapScalar("Datetime", "kotlinx.datetime.Instant")
    // If you're using adapters, you can also set this
    generateKotlinModels.set(true)

    introspection {
      endpointUrl.set("$supabaseUrl/graphql/v1")
      schemaFile.set(file("src/commonMain/graphql/schema.graphqls"))
      headers.put("Authorization", "Bearer $supabaseKey")
      headers.put("apikey", supabaseKey)
    }
  }
}

// not generating all models properly. needs refactoring. until that time don't use
//tasks.register<GenerateKto>("generateKtoModels") {
//  dependsOn("generateApolloSources")
//  doLast {
//    println("Generating DTO models...")
//  }
//}

tasks.withType(ApolloGenerateSourcesTask::class.java).configureEach {
  doNotTrackState("i don't know")
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
  }
}

// ============================================================================
// AUTOMATED SECRETS SETUP TASKS
// ============================================================================

/**
 * Task to automatically run the setup-secrets.sh script
 * This ensures secrets are properly configured for all platforms before compilation
 */
tasks.register<Exec>("setupSecrets") {
  group = "secrets"
  description = "Setup secrets for all platforms (Desktop, Android, Web, iOS)"

  // Configuration cache compatible - capture values at configuration time
  val rootDir = rootProject.projectDir
  val secretsFile = File(rootDir, "secrets.properties")
  val isWindows = System.getProperty("os.name").lowercase().contains("windows")

  workingDir = rootDir

  // Use appropriate shell based on OS
  if (isWindows) {
    commandLine("cmd", "/c", "bash", "setup-secrets.sh")
  } else {
    commandLine("bash", "setup-secrets.sh")
  }

  // Only run if secrets.properties exists
  onlyIf {
    secretsFile.exists()
  }

  doFirst {
    println("🔧 Running automated secrets setup...")
  }

  doLast {
    println("✅ Automated secrets setup completed")
  }
}

/**
 * Task to check if secrets.properties exists and warn if not
 */
tasks.register("checkSecrets") {
  group = "secrets"
  description = "Check if secrets.properties file exists"

  // Configuration cache compatible - capture values at configuration time
  val secretsFilePath = File(rootProject.projectDir, "secrets.properties").absolutePath

  doLast {
    val secretsFile = File(secretsFilePath)
    if (!secretsFile.exists()) {
      logger.warn(
        """
        ⚠️  WARNING: secrets.properties file not found!
        
        To set up secrets for all platforms:
        1. Copy the template: cp secrets.properties.template secrets.properties
        2. Fill in your actual values in secrets.properties
        3. Run: ./setup-secrets.sh
        
        Or the setup will run automatically when you build.
        """.trimIndent()
      )
    } else {
      println("✅ secrets.properties file found")
    }
  }
}

// ============================================================================
// PLATFORM-SPECIFIC SETUP TASKS
// ============================================================================

// Hook setupSecrets to run before compilation tasks for all platforms
tasks.matching { task ->
  task.name.startsWith("compile") ||
    task.name.contains("Compile") ||
    task.name == "preBuild" ||
    task.name.startsWith("assemble") ||
    task.name.startsWith("bundle") ||
    task.name == "run" ||
    task.name == "runDebug" ||
    task.name == "runRelease"
}.configureEach {
  dependsOn("setupSecrets")
}

// Android specific tasks
tasks.matching { it.name.startsWith("assemble") }.configureEach {
  dependsOn("setupSecrets")
}

// Desktop specific tasks
tasks.matching { it.name.contains("Desktop") || it.name == "run" }.configureEach {
  dependsOn("setupSecrets")
}

// Web specific tasks
tasks.matching { it.name.contains("Js") || it.name.contains("Wasm") }.configureEach {
  dependsOn("setupSecrets")
}

// iOS specific tasks
tasks.matching { it.name.contains("Ios") || it.name.contains("iOS") }.configureEach {
  dependsOn("setupSecrets")
}

// Make checkSecrets run early in the build process
tasks.named("preBuild") {
  dependsOn("checkSecrets")
}

// Also run checkSecrets before setupSecrets
tasks.named("setupSecrets") {
  dependsOn("checkSecrets")
}
