
import com.apollographql.apollo.gradle.internal.ApolloGenerateSourcesTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

      // logging
      api("com.diamondedge:logging:2.0.3")
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

  androidTestImplementation(libs.ui.test.junit4)
  debugImplementation(libs.ui.test.manifest)
}

tasks.register("allUiTests") {
  group = "verification"
  description = "Runs @UiTest annotated tests for all supported platforms & generates a unified report"

  // Configure test tasks to continue on failure
  gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
      if (task.name.contains("Test") && task.hasProperty("ignoreFailures")) {
        task.setProperty("ignoreFailures", true)
      }
    }
  }

  // Define platform test tasks
  val platformTestTasks = mapOf(
    "connectedAndroidTest" to "Android",  // Android instrumented tests
    "iosSimulatorArm64Test" to "iOS",          // iOS simulator tests
    "desktopTest" to "Desktop",                // Desktop JVM tests
    "wasmJsBrowserTest" to "Web (WASM)"       // Web/WASM tests (actual task name)
  )

  // Check if Android device is connected
  val hasAndroidDevice = try {
    val process = ProcessBuilder("adb", "devices").start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    // Check if there are any devices listed (not just the header)
    output.lines().count { it.contains("\tdevice") || it.contains("\temulator") } > 0
  } catch (e: Exception) {
    false
  }

  // Configure each test task to include only @UiTest annotated tests
  platformTestTasks.forEach { (taskName, platform) ->
    // Skip Android if no device is connected
    if (platform == "Android" && !hasAndroidDevice) {
      logger.warn("No Android device/emulator connected - skipping Android tests")
      return@forEach
    }

    tasks.findByName(taskName)?.let { task ->
      // Configure test filtering for Gradle Test tasks
      if (task is org.gradle.api.tasks.testing.Test) {
        // Only include tests from screens package (where UI tests are)
        task.filter {
          includeTestsMatching("org.aryamahasangh.screens.*")
          // Exclude any test that doesn't match our UI test pattern
          excludeTestsMatching("org.aryamahasangh.*SimpleTest")
          excludeTestsMatching("org.aryamahasangh.*UnitTest")
          excludeTestsMatching("org.aryamahasangh.*ViewModelTest")
        }

        logger.lifecycle("Configured $taskName for $platform UI tests")
      }

      // Add as dependency
      dependsOn(taskName)
    } ?: logger.warn("Test task $taskName not found for $platform")
  }

  doFirst {
    logger.lifecycle("\n=== Running UI Tests (@UiTest annotated) ===")
    logger.lifecycle("Target: Tests in org.aryamahasangh.screens package")
    logger.lifecycle("Platforms: Android (instrumented), iOS, Desktop, Web")
    
    // Check Android device status
    if (!hasAndroidDevice) {
      logger.warn("\n⚠️  Android: No device/emulator connected. To run Android tests:")
      logger.warn("   1. Connect an Android device with USB debugging enabled, OR")
      logger.warn("   2. Start an Android emulator")
      logger.warn("   Then run: ./gradlew :composeApp:connectedDebugAndroidTest")
    } else {
      logger.lifecycle("✅ Android: Device/emulator detected")
    }
    
    // Check iOS availability
    val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    if (!isMacOS) {
      logger.warn("\n⚠️  iOS: Tests can only run on macOS with Xcode installed")
    } else {
      logger.lifecycle("✅ iOS: Running on macOS")
    }
  }

  doLast {
    generateConsolidatedTestReport()
  }
}

// Helper task to check test environment
tasks.register("checkTestEnvironment") {
  group = "verification"
  description = "Check if the environment is set up for running UI tests on all platforms"

  doLast {
    println("\n=== UI Test Environment Check ===\n")

    // Check Android
    val hasAndroidDevice = try {
      val process = ProcessBuilder("adb", "devices").start()
      val output = process.inputStream.bufferedReader().readText()
      process.waitFor()
      val devices = output.lines().filter { it.contains("\tdevice") || it.contains("\temulator") }
      if (devices.isNotEmpty()) {
        println("✅ Android: ${devices.size} device(s) connected")
        devices.forEach { println("   - $it") }
        true
      } else {
        false
      }
    } catch (e: Exception) {
      false
    }

    if (!hasAndroidDevice) {
      println("❌ Android: No devices/emulators connected")
      println("   To run Android tests:")
      println("   1. Connect a device: Enable USB debugging on your Android device")
      println("   2. OR start an emulator: emulator -avd <avd_name>")
      println("   Then run: ./gradlew :composeApp:connectedDebugAndroidTest")
    }

    // Check iOS
    val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    if (isMacOS) {
      // Check if Xcode is installed
      val hasXcode = try {
        val process = ProcessBuilder("xcodebuild", "-version").start()
        process.waitFor() == 0
      } catch (e: Exception) {
        false
      }

      if (hasXcode) {
        println("\n✅ iOS: macOS with Xcode detected")
        println("   iOS tests should run successfully")
      } else {
        println("\n❌ iOS: Xcode not found")
        println("   Install Xcode from the App Store to run iOS tests")
      }
    } else {
      println("\n❌ iOS: Not running on macOS")
      println("   iOS tests can only run on macOS with Xcode installed")
    }

    // Desktop is always available
    println("\n✅ Desktop: Ready (JVM-based tests)")

    // Web/WASM is always available
    println("\n✅ Web (WASM): Ready (browser-based tests)")

    println("\n=== Summary ===")
    println("To run all UI tests: ./gradlew :composeApp:allUiTests")
    println("To run specific platform tests:")
    println("  Android: ./gradlew :composeApp:connectedDebugAndroidTest")
    println("  iOS:     ./gradlew :composeApp:iosSimulatorArm64Test")
    println("  Desktop: ./gradlew :composeApp:desktopTest")
    println("  Web:     ./gradlew :composeApp:wasmJsBrowserTest")
  }
}

// Helper task to find test results
tasks.register("findTestResults") {
  group = "verification"
  description = "Finds all test result files in the build directory"

  doLast {
    println("\n=== Searching for test results ===")
    val testFilePatterns = listOf(".xml", ".html", "TEST-", "test-results", "test-report")

    file(buildDir).walkTopDown()
      .filter { file ->
        file.isFile && testFilePatterns.any { pattern ->
          file.name.contains(pattern, ignoreCase = true) ||
            file.parent.contains(pattern, ignoreCase = true)
        }
      }
      .forEach { file ->
        val relativePath = file.relativeTo(buildDir).path
        println("Found: $relativePath (${file.length()} bytes)")
      }
  }
}

/**
 * Generates a consolidated HTML test report from UI test results only
 */
fun generateConsolidatedTestReport(includeAndroid: Boolean = true) {
  val reportDir = file("${buildDir}/reports/allUiTests")
  reportDir.mkdirs()
  val htmlReport = File(reportDir, "index.html")
  val debugReport = File(reportDir, "debug.txt")

  // Collect test results from UI test platforms only
  val testResults = mutableMapOf<String, TestPlatformResult>()
  val debugInfo = StringBuilder()

  debugInfo.appendLine("=== UI Test Report Generation ===")
  debugInfo.appendLine("Build directory: ${buildDir.absolutePath}")
  debugInfo.appendLine("Target UI test tasks: connectedDebugAndroidTest, iosSimulatorArm64Test, desktopTest, wasmJsBrowserTest")

  // UI test platforms and their specific result locations
  val allPlatforms = mutableMapOf(
    "iOS" to listOf(
      "reports/tests/iosSimulatorArm64Test",
      "test-results/iosSimulatorArm64Test",
      "bin/iosSimulatorArm64/debugTest/test-results",
      "kotlin/iosSimulatorArm64Test/test-results"
    ),
    "Desktop" to listOf(
      "reports/tests/desktopTest",
      "test-results/desktopTest"
    ),
    "Web (WASM)" to listOf(
      "reports/tests/wasmJsBrowserTest",
      "test-results/wasmJsBrowserTest",
      "reports/tests/wasmJsTest",
      "test-results/wasmJsTest"
    )
  )

  // Android instrumented test result locations
  allPlatforms["Android"] = listOf(
    "reports/androidTests/connected",
    "outputs/androidTest-results/connected",
    "reports/androidTests/connected/debug",
    "outputs/androidTest-results/connected/debug"
  )

  val uiTestPlatforms = allPlatforms

  // Search for test results for each UI platform
  uiTestPlatforms.forEach { (platform, paths) ->
    debugInfo.appendLine("\n=== Searching for $platform UI test results ===")

    // Find the best result among all paths for this platform
    val platformResults = mutableListOf<TestPlatformResult>()

    paths.forEach { relativePath ->
      val dir = file("${buildDir}/$relativePath")
      debugInfo.appendLine("Checking: ${dir.absolutePath}")
      debugInfo.appendLine("  Exists: ${dir.exists()}")

      if (dir.exists()) {
        val files = dir.listFiles() ?: emptyArray()
        debugInfo.appendLine("  Found directory with ${files.size} files")

        // List all files for debugging
        files.forEach { file ->
          debugInfo.appendLine("    File: ${file.name} (${file.length()} bytes)")
        }

        // Look for XML files specifically
        val xmlFiles = files.filter { it.name.endsWith(".xml") }
        debugInfo.appendLine("  XML files: ${xmlFiles.size}")

        // For Android, also check for HTML files in connected test reports
        if (platform == "Android" && xmlFiles.isEmpty()) {
          val htmlFiles = files.filter { it.name.endsWith(".html") && it.name != "index.html" }
          debugInfo.appendLine("  HTML files: ${htmlFiles.size}")

          // Look for Android instrumented test XML files
          val connectedXmlFiles = dir.walkTopDown()
            .filter { it.isFile && it.name.startsWith("TEST-") && it.name.endsWith(".xml") }
            .toList()

          if (connectedXmlFiles.isNotEmpty()) {
            debugInfo.appendLine("  Found ${connectedXmlFiles.size} Android connected test XML files")
            connectedXmlFiles.forEach { xmlFile ->
              val result = parseTestResults(xmlFile.parentFile, platform)
              if (result.totalTests > 0) {
                platformResults.add(result)
                debugInfo.appendLine("  ✓ Parsed Android XML results: ${result.totalTests} tests")
              } else {
                // Check if this is an installation failure
                try {
                  val xmlContent = xmlFile.readText()
                  if (xmlContent.contains("INSTALL_FAILED")) {
                    debugInfo.appendLine("  ⚠️ Android test installation failed")
                  }
                } catch (e: Exception) {
                  debugInfo.appendLine("  ✗ Error checking XML: ${e.message}")
                }
              }
            }
          }

          // If no XML files, try HTML parsing
          val indexHtml = File(dir, "index.html")
          if (indexHtml.exists() && platformResults.isEmpty()) {
            debugInfo.appendLine("  Found Android HTML report")
            // Look for test results in subdirectories
            val classFiles = dir.walkTopDown()
              .filter { it.isFile && it.name.endsWith(".html") && it.name != "index.html" }
              .toList()

            if (classFiles.isNotEmpty()) {
              debugInfo.appendLine("  Found ${classFiles.size} test class HTML files")
              // Parse each class file for test results
              var totalTests = 0
              var passedTests = 0
              val testCases = mutableListOf<TestCase>()

              classFiles.forEach { classFile ->
                try {
                  val content = classFile.readText()
                  // Extract class name from file
                  val className = classFile.nameWithoutExtension

                  // Look for test method results in HTML
                  val testPattern = Regex("""<h3 class="(passed|failed|skipped)">(\w+)</h3>""")
                  testPattern.findAll(content).forEach { match ->
                    val status = match.groupValues[1].uppercase()
                    val testName = match.groupValues[2]
                    totalTests++
                    if (status == "PASSED") passedTests++

                    testCases.add(TestCase(testName, className, status, "0.0s"))
                  }
                } catch (e: Exception) {
                  debugInfo.appendLine("    Error parsing ${classFile.name}: ${e.message}")
                }
              }

              if (totalTests > 0) {
                val result = TestPlatformResult(
                  platform = platform,
                  totalTests = totalTests,
                  passedTests = passedTests,
                  failedTests = totalTests - passedTests,
                  skippedTests = 0,
                  duration = "0.0s",
                  testCases = testCases
                )
                platformResults.add(result)
                debugInfo.appendLine("  ✓ Parsed Android HTML results: $totalTests tests")
              }
            }
          }
        } else if (xmlFiles.isNotEmpty()) {
          xmlFiles.forEach { xmlFile ->
            debugInfo.appendLine("    XML: ${xmlFile.name}")
            // Preview XML content for debugging
            try {
              val content = xmlFile.readText()
              if (content.contains("<testsuite") || content.contains("<testsuites")) {
                debugInfo.appendLine("      ✓ Valid test suite XML")
                val testsMatch = Regex("""tests="(\d+)"""").find(content)
                val tests = testsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                debugInfo.appendLine("      Tests found in XML: $tests")
              } else {
                debugInfo.appendLine("      ✗ Not a test suite XML")
              }
            } catch (e: Exception) {
              debugInfo.appendLine("      ✗ Error reading XML: ${e.message}")
            }
          }

          val result = parseTestResults(dir, platform)
          debugInfo.appendLine("  Parsed result: ${result.totalTests} tests")

          // Log test cases found
          if (result.testCases.isNotEmpty()) {
            debugInfo.appendLine("  Test cases found:")
            result.testCases.forEach { testCase ->
              debugInfo.appendLine("    - ${testCase.className}.${testCase.name} [${testCase.status}]")
            }
          }

          if (result.totalTests > 0) {
            platformResults.add(result)
            debugInfo.appendLine("  ✓ Added result for $platform")
          }
        } else {
          debugInfo.appendLine("  ✗ No XML files found")
        }
      } else {
        debugInfo.appendLine("  ✗ Directory does not exist")
      }
    }

    // Use the best result (most tests) or create empty/synthetic placeholder
    val finalResult = if (platformResults.isNotEmpty()) {
      val bestResult = platformResults.maxByOrNull { it.totalTests }!!
      debugInfo.appendLine("  ✓ Final result for $platform: ${bestResult.totalTests} tests")
      bestResult
    } else {
      debugInfo.appendLine("  ✗ No test results found for $platform")

      TestPlatformResult(
        platform = platform,
        totalTests = 0,
        passedTests = 0,
        failedTests = 0,
        skippedTests = 0,
        duration = "0.0s",
        testCases = emptyList()
      )
    }
    testResults[platform] = finalResult
  }

  // Additional comprehensive search for iOS tests if none found
  if (testResults["iOS"]?.totalTests == 0) {
    debugInfo.appendLine("\n=== Additional iOS test search ===")
    val iosSearchPaths = listOf(
      "bin/iosSimulatorArm64",
      "kotlin/iosSimulatorArm64",
      "kotlin-build/iosSimulatorArm64",
      "tmp/iosSimulatorArm64Test"
    )

    iosSearchPaths.forEach { searchPath ->
      val searchDir = file("${buildDir}/$searchPath")
      if (searchDir.exists()) {
        debugInfo.appendLine("Found iOS directory: ${searchDir.absolutePath}")
        searchDir.walkTopDown()
          .filter { it.isFile && it.name.endsWith(".xml") && it.name.contains("test", ignoreCase = true) }
          .forEach { xmlFile ->
            debugInfo.appendLine("  Found XML in iOS directory: ${xmlFile.relativeTo(buildDir).path}")
            val result = parseTestResults(xmlFile.parentFile, "iOS")
            if (result.totalTests > 0) {
              testResults["iOS"] = result
              debugInfo.appendLine("  ✓ Using iOS results: ${result.totalTests} tests")
            }
          }
      }
    }
  }

  // Write debug info
  debugReport.writeText(debugInfo.toString())

  // Generate HTML report
  val html = generateHtmlReport(testResults)
  htmlReport.writeText(html)

  // Print report location and summary
  println("\n" + "=".repeat(80))
  println("✅ UI Test Report Generated!")
  println("📊 Report location: file://${htmlReport.absolutePath}")
  println("📊 Debug info: file://${debugReport.absolutePath}")
  println("\n📝 UI Test Summary (connectedDebugAndroidTest, iosSimulatorArm64Test, desktopTest, wasmJsBrowserTest):")
  testResults.forEach { (platform, result) ->
    val status = when {
      result.totalTests == 0 -> "⚠️  No tests found"
      result.failedTests > 0 -> "❌ Failed (${result.failedTests}/${result.totalTests})"
      else -> "✅ All passed (${result.passedTests}/${result.totalTests})"
    }
    println("   $platform: $status")
  }
  println("=".repeat(80) + "\n")
}

data class TestPlatformResult(
  val platform: String,
  val totalTests: Int,
  val passedTests: Int,
  val failedTests: Int,
  val skippedTests: Int,
  val duration: String,
  val testCases: List<TestCase>
)

data class TestCase(
  val name: String,
  val className: String,
  val status: String,
  val duration: String,
  val errorMessage: String? = null
)

/**
 * Parse test results from platform-specific test directories
 */
fun parseTestResults(testDir: File, platform: String): TestPlatformResult {
  logger.lifecycle("Parsing test results for $platform from: ${testDir.absolutePath}")

  val xmlFiles = testDir.walkTopDown()
    .filter {
      it.isFile && it.name.endsWith(".xml") && (
        it.name.startsWith("TEST-") ||
          it.name.contains("test", ignoreCase = true) ||
          // Android connected tests may have different naming
          it.parent.contains("connected", ignoreCase = true)
        )
    }
    .toList()

  logger.lifecycle("Found ${xmlFiles.size} XML files for $platform")

  var totalTests = 0
  var passedTests = 0
  var failedTests = 0
  var skippedTests = 0
  var totalDuration = 0.0
  val testCases = mutableListOf<TestCase>()

  xmlFiles.forEach { xmlFile ->
    try {
      logger.lifecycle("Processing XML file: ${xmlFile.name}")
      val content = xmlFile.readText()

      // Check if this is a valid test suite XML
      if (!content.contains("<testsuite") && !content.contains("<testsuites")) {
        logger.warn("Skipping ${xmlFile.name} - doesn't appear to be a test result XML")
        return@forEach
      }

      // Simple XML parsing (you might want to use a proper XML parser in production)
      val testsMatch = Regex("""tests="(\d+)"""").find(content)
      val failuresMatch = Regex("""failures="(\d+)"""").find(content)
      val skippedMatch = Regex("""skipped="(\d+)"""").find(content)
      val timeMatch = Regex("""time="([\d.]+)"""").find(content)

      val tests = testsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
      val failures = failuresMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
      val skipped = skippedMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
      val time = timeMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

      logger.lifecycle("  Tests: $tests, Failures: $failures, Skipped: $skipped, Time: $time")

      // Log the raw test count before filtering
      if (platform == "iOS" || platform == "Desktop") {
        logger.lifecycle("  Raw test count for $platform: $tests")
      }

      // Check for Android test installation failures
      if (platform == "Android" && tests == 0 && content.contains("INSTALL_FAILED")) {
        logger.warn("  Android tests failed to install - signature mismatch detected")
        // Try to find test results in subdirectories
        val parentDir = xmlFile.parentFile
        parentDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
          logger.lifecycle("  Checking Android subdirectory: ${subDir.name}")
          val subDirResults = parseTestResults(subDir, platform)
          if (subDirResults.totalTests > 0) {
            return subDirResults
          }
        }
      }

      // Don't add to totals yet - we'll count after filtering test cases
      var filteredTests = 0
      var filteredPassed = 0
      var filteredFailed = 0
      var filteredSkipped = 0

      // Parse individual test cases with better regex
      // Handle both self-closing and regular test case tags
      val testCasePatterns = listOf(
        Regex("""<testcase[^>]+>.*?</testcase>""", RegexOption.DOT_MATCHES_ALL),
        Regex("""<testcase[^>]+/>""")
      )

      testCasePatterns.forEach { pattern ->
        pattern.findAll(content).forEach { matchResult ->
          val testCaseXml = matchResult.value

          // Extract attributes
          val nameMatch = Regex("""name="([^"]+)"""").find(testCaseXml)
          val classMatch = Regex("""classname="([^"]+)"""").find(testCaseXml)
          val timeMatch = Regex("""time="([\d.]+)"""").find(testCaseXml)

          val name = nameMatch?.groupValues?.get(1) ?: "Unknown"
          val className = classMatch?.groupValues?.get(1) ?: "Unknown"
          val testTime = timeMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

          val status = when {
            testCaseXml.contains("<failure") -> "FAILED"
            testCaseXml.contains("<error") -> "FAILED"
            testCaseXml.contains("<skipped") -> "SKIPPED"
            testCaseXml.contains("/>") && !testCaseXml.contains("<failure") && !testCaseXml.contains("<error") -> "PASSED"
            else -> "PASSED"
          }

          val errorMessage = when {
            testCaseXml.contains("<failure") -> {
              Regex("""<failure[^>]*>(.+?)</failure>""", RegexOption.DOT_MATCHES_ALL)
                .find(testCaseXml)?.groupValues?.get(1)?.trim()?.take(200) // Limit error message length
            }

            testCaseXml.contains("<error") -> {
              Regex("""<error[^>]*>(.+?)</error>""", RegexOption.DOT_MATCHES_ALL)
                .find(testCaseXml)?.groupValues?.get(1)?.trim()?.take(200)
            }

            else -> null
          }

          // Avoid duplicate test cases and filter out non-UI tests
          if (!testCases.any { it.name == name && it.className == className }) {
            // Only include tests from screens package (UI tests)
            if (className.contains("screens") || className.contains("AboutUsScreenTest")) {
              testCases.add(TestCase(name, className, status, "%.3fs".format(testTime), errorMessage))
              filteredTests++
              when (status) {
                "PASSED" -> filteredPassed++
                "FAILED" -> filteredFailed++
                "SKIPPED" -> filteredSkipped++
              }
              logger.lifecycle("    Including UI test: $className.$name")
            } else {
              logger.lifecycle("    Filtering out non-UI test: $className.$name")
            }
          }
        }
      }

      // Add filtered counts to totals
      totalTests += filteredTests
      passedTests += filteredPassed
      failedTests += filteredFailed
      skippedTests += filteredSkipped
      totalDuration += time
    } catch (e: Exception) {
      logger.warn("Failed to parse test results from $xmlFile: ${e.message}")
      e.printStackTrace()
    }
  }

  // If no XML files found, check for other test indicators
  if (xmlFiles.isEmpty() && testDir.exists()) {
    logger.warn("No XML test results found in ${testDir.absolutePath}")
    // Look for any files that might indicate tests were run
    val anyTestFiles = testDir.walkTopDown()
      .filter { it.isFile && (it.name.contains("test", ignoreCase = true) || it.name.endsWith(".html")) }
      .toList()
    if (anyTestFiles.isNotEmpty()) {
      logger.lifecycle("Found ${anyTestFiles.size} potential test-related files")
    }
  }

  logger.lifecycle("Summary for $platform: Tests=$totalTests, Passed=$passedTests, Failed=$failedTests, Skipped=$skippedTests")
  logger.lifecycle("  Test cases found: ${testCases.size}")
  testCases.forEach { testCase ->
    logger.lifecycle("    - ${testCase.className}.${testCase.name}")
  }

  return TestPlatformResult(
    platform = platform,
    totalTests = totalTests,
    passedTests = passedTests,
    failedTests = failedTests,
    skippedTests = skippedTests,
    duration = "%.3fs".format(totalDuration),
    testCases = testCases
  )
}

/**
 * Generate HTML report from test results
 */
fun generateHtmlReport(testResults: Map<String, TestPlatformResult>): String {
  val totalTests = testResults.values.sumOf { it.totalTests }
  val totalPassed = testResults.values.sumOf { it.passedTests }
  val totalFailed = testResults.values.sumOf { it.failedTests }
  val totalSkipped = testResults.values.sumOf { it.skippedTests }

  return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Arya Mahasangh UI Test Report</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            padding: 30px;
        }
        h1 {
            color: #333;
            margin-bottom: 30px;
            text-align: center;
        }
        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }
        .summary-card {
            background-color: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            border: 1px solid #e9ecef;
        }
        .summary-card h3 {
            margin: 0 0 10px 0;
            color: #666;
            font-size: 14px;
            text-transform: uppercase;
        }
        .summary-card .value {
            font-size: 32px;
            font-weight: bold;
            margin: 0;
        }
        .summary-card.passed .value { color: #28a745; }
        .summary-card.failed .value { color: #dc3545; }
        .summary-card.skipped .value { color: #ffc107; }
        .summary-card.total .value { color: #007bff; }
        
        .platform-section {
            margin-bottom: 30px;
            border: 1px solid #e9ecef;
            border-radius: 8px;
            overflow: hidden;
        }
        .platform-header {
            background-color: #f8f9fa;
            padding: 15px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            cursor: pointer;
        }
        .platform-header h2 {
            margin: 0;
            font-size: 20px;
            color: #333;
        }
        .platform-stats {
            display: flex;
            gap: 20px;
            font-size: 14px;
        }
        .stat {
            display: flex;
            align-items: center;
            gap: 5px;
        }
        .stat.passed { color: #28a745; }
        .stat.failed { color: #dc3545; }
        .stat.skipped { color: #ffc107; }
        
        .test-details {
            padding: 20px;
            display: none;
        }
        .test-details.expanded {
            display: block;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
        }
        th, td {
            text-align: left;
            padding: 12px;
            border-bottom: 1px solid #e9ecef;
        }
        th {
            background-color: #f8f9fa;
            font-weight: 600;
            color: #666;
        }
        .status {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 600;
        }
        .status.passed {
            background-color: #d4edda;
            color: #155724;
        }
        .status.failed {
            background-color: #f8d7da;
            color: #721c24;
        }
        .status.skipped {
            background-color: #fff3cd;
            color: #856404;
        }
        .error-message {
            color: #dc3545;
            font-size: 12px;
            margin-top: 5px;
        }
        .timestamp {
            text-align: center;
            color: #666;
            margin-top: 30px;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🧪 Arya Mahasangh UI Test Report</h1>
        
        <div class="summary">
            <div class="summary-card total">
                <h3>Total Tests</h3>
                <p class="value">${totalTests}</p>
            </div>
            <div class="summary-card passed">
                <h3>Passed</h3>
                <p class="value">${totalPassed}</p>
            </div>
            <div class="summary-card failed">
                <h3>Failed</h3>
                <p class="value">${totalFailed}</p>
            </div>
            <div class="summary-card skipped">
                <h3>Skipped</h3>
                <p class="value">${totalSkipped}</p>
            </div>
        </div>
        
        ${
    testResults.entries.joinToString("\n") { (platform, result) ->
      """
        <div class="platform-section">
            <div class="platform-header" onclick="toggleDetails('${platform}')">
                <h2>📱 ${platform}</h2>
                <div class="platform-stats">
                    <span class="stat passed">✅ ${result.passedTests} passed</span>
                    <span class="stat failed">❌ ${result.failedTests} failed</span>
                    <span class="stat skipped">⏭️ ${result.skippedTests} skipped</span>
                    <span class="stat">⏱️ ${result.duration}</span>
                </div>
            </div>
            <div class="test-details" id="${platform}">
                <table>
                    <thead>
                        <tr>
                            <th>Test Name</th>
                            <th>Class</th>
                            <th>Status</th>
                            <th>Duration</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${
        if (result.testCases.isEmpty()) {
          "<tr><td colspan='4' style='text-align: center; color: #666;'>No test results found</td></tr>"
        } else {
          result.testCases.joinToString("\n") { test ->
            """
                        <tr>
                            <td>${test.name}</td>
                            <td>${test.className}</td>
                            <td><span class="status ${test.status.lowercase()}">${test.status}</span></td>
                            <td>${test.duration}</td>
                        </tr>
                        ${
              if (test.errorMessage != null) {
                "<tr><td colspan='4' class='error-message'>${test.errorMessage}</td></tr>"
              } else ""
            }
                        """
          }
        }
      }
                    </tbody>
                </table>
            </div>
        </div>
        """
    }
  }
        
        <p class="timestamp">Generated on ${
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
  }</p>
    </div>
    
    <script>
        function toggleDetails(platform) {
            const details = document.getElementById(platform);
            details.classList.toggle('expanded');
        }
    </script>
</body>
</html>
  """.trimIndent()
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

//tasks.register<ComposeHotRun>("runHot") {
//  mainClass.set("org.aryamahasangh.MainKt")
//}

tasks.withType<ComposeHotRun>().configureEach {
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
//
//abstract class AddWasmPreloadLinksTask : DefaultTask() {
//    @get:Internal
//    abstract val buildDirectory: DirectoryProperty
//
//    init {
//        description = "Adds preload links for WASM files to index.html"
//        group = "wasm"
//    }
//
//    @TaskAction
//    fun execute() {
//        val distDir = buildDirectory.get().dir("dist/wasmJs/productionExecutable").asFile
//        println("Checking for WASM files in: ${distDir.absolutePath}")
//
//        if (!distDir.exists()) {
//            throw GradleException("Distribution directory not found at ${distDir.absolutePath}")
//        }
//
//        val indexFile = File(distDir, "index.html")
//        if (!indexFile.exists()) {
//            throw GradleException("index.html not found in ${distDir.absolutePath}")
//        }
//
//        // Find all .wasm files in the directory
//        val wasmFiles = distDir.listFiles { file -> file.name.endsWith(".wasm") }
//            ?: throw GradleException("No .wasm files found in ${distDir.absolutePath}")
//
//        if (wasmFiles.isEmpty()) {
//            throw GradleException("No .wasm files found in ${distDir.absolutePath}")
//        }
//
//        // Read the current content of index.html
//        var content = indexFile.readText()
//
//        // Check if preload links already exist to avoid duplicates
//        if (content.contains("""rel="preload" href="/.+\.wasm"""".toRegex())) {
//            println("WASM preload links already exist in index.html, skipping...")
//            return
//        }
//
//        // Create preload links for each .wasm file
//        val preloadLinks = wasmFiles.joinToString("\n") { wasmFile ->
//            """    <link rel="preload" href="/${wasmFile.name}" as="fetch" type="application/wasm" crossorigin="anonymous" />"""
//        }
//
//        // Insert the preload links after the opening <head> tag
//        content = content.replace("<head>", "<head>\n$preloadLinks")
//
//        // Write the modified content back to the file
//        indexFile.writeText(content)
//
//        println("✅ Successfully added preload links for ${wasmFiles.size} WASM files to index.html:")
//        wasmFiles.forEach { file ->
//            println("  - ${file.name}")
//        }
//    }
//}
//
//tasks.register<AddWasmPreloadLinksTask>("addWasmPreloadLinks") {
//    buildDirectory.set(layout.buildDirectory)
//}
//
//// Hook the task to run after wasmJsBrowserDistribution
//tasks.named("wasmJsBrowserDistribution") {
//    finalizedBy("addWasmPreloadLinks")
//}
