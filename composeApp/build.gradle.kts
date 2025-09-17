@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
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
  alias(libs.plugins.kmp.secrets.plugin)
}

// Load secrets from local.properties for build-time operations
fun loadLocalProperties(): Properties {
  val localPropsFile = rootProject.file("local.properties")
  val props = Properties()
  
  if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { props.load(it) }
  }
  
  return props
}

val localProps = loadLocalProperties()

// Get version from local.properties and derive both name and code
val appVersion: String = localProps.getProperty("app_version", "1.0.0")
val appVersionCode: Int = calculateVersionCode(appVersion)

// Function to calculate version code from semantic version
fun calculateVersionCode(version: String): Int {
  val parts = version.split(".")
  return when (parts.size) {
    3 -> parts[0].toInt() * 10000 + parts[1].toInt() * 100 + parts[2].toInt()
    2 -> parts[0].toInt() * 100 + parts[1].toInt()
    1 -> parts[0].toInt()
    else -> 1
  }
}

// Get environment from project properties (can be passed via -Penv=dev/staging/prod)
val environment = project.findProperty("env")?.toString() ?: localProps.getProperty("environment", "dev")

// Apply the test reporting configuration
apply(from = "$rootDir/composeApp/testreporting.gradle.kts")

secretsConfig {
  outputDir = layout.buildDirectory
    .dir("generated/kmp-secrets/commonMain/kotlin")
    .get()
    .asFile
    .absolutePath
}

// NOTE: Do not add name-based matching here (e.g., tasks.matching { it.name.startsWith("compileKotlin") })
// because it is hard to keep precise and can trigger IDE generic inference warnings.
// A single, type-safe wiring is provided below in the afterEvaluate block for all backends
// (JVM/Android, Native, and Wasm/JS) using their concrete compile task types.

// Disable secrets generation for all *Test compilations to avoid shared-output conflicts.
// Why this is necessary:
// - The secrets plugin is configured to write into a single shared outputDir under commonMain.
// - If test variants also generate into the same directory, Gradle may run multiple
//   generators concurrently and race/overwrite files, causing intermittent failures.
// - Tests can still use the generated secrets from commonMain (read-only).
// If you later need test-only secrets, configure a distinct outputDir for tests to avoid clashes.
tasks.configureEach {
  if (name.startsWith("generateSecrets") && name.contains("Test")) {
    enabled = false
  }
}

kotlin {

  jvmToolchain(17)

  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
    // to run instrumented (emulator) tests for Android
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
  }

  val iosX64 = iosX64()
  val iosArm64 = iosArm64()
  val iosSimArm64 = iosSimulatorArm64()

  listOf(iosX64, iosArm64, iosSimArm64).forEach { iosTarget ->
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
          useChromeHeadless()
          useConfigDirectory(
            project.projectDir.resolve("karma.config.d").apply {
              mkdirs()
            }
          )
        }
        // Always execute tests to surface println/console output
        outputs.upToDateWhen { false }
      }
    }
    binaries.executable()
  }

  sourceSets {
    val commonMain by getting {
      kotlin.srcDir(
        layout.buildDirectory
          .dir("generated/kmp-secrets/commonMain/kotlin")
          .get()
          .asFile
          .absolutePath
      )
    }
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
      implementation(projects.uiComponents)
      implementation(compose.animation)
      implementation(libs.coil.compose)
      implementation(libs.coil.network.ktor3)
      implementation(libs.navigation.compose)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.material.adaptive)
      // runtimeOnly("org.jetbrains.compose.material:material:1.9.0-alpha01")
      // Koin for Dependency Injection
      implementation(libs.koin.core)
      implementation(libs.koin.compose)

      implementation(libs.kotlinx.datetime)

      implementation(libs.ktor.client.core)

      implementation(libs.compose.remember.setting)

      // filekit
      // Enables FileKit without Compose dependencies
      implementation(libs.filekit.core)
      // Enables FileKit with Composable utilities
      implementation(libs.filekit.compose)

      // supabase
      implementation(project.dependencies.platform(libs.supabase.bom)) // 
      implementation(libs.supabase.postgrest)
      implementation(libs.supabase.auth)
      implementation(libs.supabase.realtime)
      implementation(libs.supabase.storage)
      implementation(libs.supabase.apollographql)
      implementation(libs.supabase.coil)

      // apollo adapters
      implementation(libs.apollo.adapters.core)
      implementation(libs.apollo.adapters.kotlinx.datetime)

      // apollo normalized cache
      implementation(libs.apollo.normalized.cache)

      // logging
      api("com.diamondedge:logging:2.0.3")
      // list reorder
      implementation(libs.reorderable)

      // Image compression module
      implementation(projects.imgCompressCmp)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
      implementation(compose.uiTest)

      // Koin test dependencies
      implementation(libs.koin.test)
    }
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
      implementation(libs.compose.ui.tooling.preview)
      implementation(libs.ktor.client.android)
      implementation(libs.koin.androidx.compose)
    }

    androidInstrumentedTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.koin.test)
      implementation(libs.koin.test.junit4)
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
      implementation(libs.kotlin.test.junit)
    }
    wasmJsMain.dependencies {
      implementation(libs.ktor.client.wasm)
    }
  }
}

android {
  namespace = "com.aryamahasangh"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.aryamahasangh"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()

    // Use appVersion and appVersionCode from local.properties, with CI override
    versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: appVersionCode
    versionName = System.getenv("VERSION_NAME") ?: appVersion

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Expose version information to the app via BuildConfig
    buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
    buildConfigField("int", "VERSION_CODE", "$versionCode")
    buildConfigField("String", "ENVIRONMENT", "\"$environment\"")

    // Debug output removed to reduce configuration noise
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

  androidTestImplementation(libs.ui.test.junit4)
  debugImplementation(libs.ui.test.manifest)
}

compose.desktop {
  application {
    mainClass = "com.aryamahasangh.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "com.aryamahasangh"
      packageVersion = appVersion

      linux {
        modules("jdk.security.auth")
      }
    }

    // Set JVM arguments to pass version information
    jvmArgs += listOf(
      "-Dapp.version.name=$appVersion",
      "-Dapp.version.code=$appVersionCode",
      "-Dapp.environment=$environment"
    )
  }
}

// Ensure ALL Kotlin compile tasks that read the shared secrets output run AFTER the secrets generators.
// Why this is necessary (Gradle 8 validation):
// - compile tasks (JVM/Android, Native, Wasm/JS) consume the generated sources under
//   build/generated/kmp-secrets/commonMain/kotlin.
// - Gradle 8 enforces explicit dependencies between producers and consumers.
// - Without dependsOn, you will see errors like:
//   "compileKotlinWasmJs uses output of generateSecrets... without declared dependency".
// This single, type-safe wiring targets the common superclass for all Kotlin compile tasks
// so any future backend is covered automatically.
tasks.withType(AbstractKotlinCompileTool::class.java).configureEach {
  dependsOn(tasks.matching { it.name.startsWith("generateSecrets") && !it.name.contains("Test") })
}
// by default composeApp:allTests only works for wasm & ios
tasks.register("allKmpTests") {
  dependsOn(
    "testDebugUnitTest",        // Android unit (JVM)
    "desktopTest",              // Desktop JVM
    "iosX64Test",               // iOS simulator (or iosSimulatorArm64Test)
    "wasmJsBrowserTest"         // Wasm/browser
  )
}

apollo {
  service("service") {
    packageName.set("com.aryamahasangh")
    mapScalar("Datetime", "kotlinx.datetime.Instant")
    mapScalar("Date", "kotlinx.datetime.LocalDate")
    // If you're using adapters, you can also set this
    generateKotlinModels.set(true)
    generateInputBuilders.set(true)

    // Make IDEA aware of codegen and will run it during your Gradle Sync, default: false
    // generateSourcesDuringGradleSync.set(true)

    // Read Supabase credentials directly from local.properties based on environment
    val supabaseUrl = localProps.getProperty("${environment}_supabase_url", "")
    val supabaseKey = localProps.getProperty("${environment}_supabase_key", "")

    introspection {
      endpointUrl.set("$supabaseUrl/graphql/v1")
      schemaFile.set(file("src/commonMain/graphql/schema.graphqls"))
      headers.put("Authorization", "Bearer $supabaseKey")
      headers.put("apikey", supabaseKey)
    }
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
  }
}

// -----------------------------------------------------------------------------
// Desktop test configuration: ensure JUnit runner for kotlin-test-junit
// -----------------------------------------------------------------------------
tasks.named("desktopTest", org.gradle.api.tasks.testing.Test::class).configure {
  useJUnit()
  // Force execution to ensure discovery runs and console output is visible
  outputs.upToDateWhen { false }
  // Include all tests in our package hierarchy (works with Kotlin-compiled classes)
  filter {
    includeTestsMatching("com.aryamahasangh.*")
  }
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showStandardStreams = true
  }
}
