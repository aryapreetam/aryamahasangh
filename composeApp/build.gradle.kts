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
  id("io.github.tarasovvp.kmp-secrets-plugin") version "1.2.0"
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
          useConfigDirectory(
            project.projectDir.resolve("karma.config.d").apply {
              mkdirs()
            }
          )
        }
      }
    }
    binaries.executable()
  }

// ============================================================================
// WASM/WEB: Generate version info JS for browser builds
// ============================================================================

  val generateWebVersionInfo by tasks.registering {
    group = "build"
    description = "Generate version info JS file for web (Wasm) build"

  val buildDir = layout.buildDirectory
  val outputDir = buildDir.dir("processedResources/wasmJs/main")
  // Always run when building a web distribution
  outputs.file(outputDir.map { it.file("web-version-info.js") })

  doLast {
    val versionCodeCalculated = appVersion.split(".").let { parts ->
      when (parts.size) {
        3 -> parts[0].toInt() * 10000 + parts[1].toInt() * 100 + parts[2].toInt()
        2 -> parts[0].toInt() * 100 + parts[1].toInt()
        1 -> parts[0].toInt()
        else -> 1
      }
    }
    val js = """
      |window.APP_VERSION_NAME = "${appVersion}";
      |window.APP_VERSION_CODE = ${versionCodeCalculated};
      |window.APP_ENVIRONMENT = "${environment}";
      |console.log("Web version info loaded:", window.APP_VERSION_NAME, window.APP_VERSION_CODE, window.APP_ENVIRONMENT);
    """.trimMargin()
    val dir = outputDir.get().asFile
    if (!dir.exists()) {
      dir.mkdirs()
    }
    val outFile = dir.resolve("web-version-info.js")
    outFile.writeText(js)
    println("✅ Generated web-version-info.js at ${outFile.absolutePath}")
  }
}

// Ensure version info is generated before processing resources and build
  tasks.matching { it.name.startsWith("processWasmJs") }.configureEach {
    dependsOn(generateWebVersionInfo)
  }
  tasks.matching { it.name == "wasmJsBrowserProductionWebpack" || it.name == "wasmJsBrowserDevelopmentWebpack" }
    .configureEach {
    dependsOn(generateWebVersionInfo)
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
      implementation("io.github.vinceglb:filekit-core:0.8.8")
      // Enables FileKit with Composable utilities
      implementation("io.github.vinceglb:filekit-compose:0.8.8")

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
      implementation("com.apollographql.apollo:apollo-normalized-cache:4.1.1")

      // logging
      api("com.diamondedge:logging:2.0.3")
      // list reorder
      implementation(libs.reorderable)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
      implementation(compose.uiTest)

      // Koin test dependencies
      implementation(libs.koin.test)
    }
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
      implementation(libs.compose.ui.tooling.preview)
      implementation("androidx.compose.ui:ui:${libs.versions.compose.android.get()}")
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
    
    // Debug output for development
    println("📱 Android version: $versionName ($versionCode)")
    println("🌍 Environment: $environment")
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

tasks.withType<ComposeHotRun>().configureEach {
  mainClass.set("com.aryamahasangh.MainKt")
}

apollo {
  service("service") {
    packageName.set("com.aryamahasangh")
    mapScalar("Datetime", "kotlinx.datetime.Instant")
    mapScalar("Date", "kotlinx.datetime.LocalDate")
    // If you're using adapters, you can also set this
    generateKotlinModels.set(true)

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