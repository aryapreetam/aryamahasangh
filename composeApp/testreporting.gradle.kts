import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Extension to configure test reporting
open class TestReportingExtension {
  var includeAndroid: Boolean = true
}

// Register extension
val testReporting = extensions.create<TestReportingExtension>("testReporting")

// Data classes for test results
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

// Main UI test task
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
    "connectedAndroidTest" to "Android",
    "iosSimulatorArm64Test" to "iOS",
    "desktopTest" to "Desktop",
    "wasmJsBrowserTest" to "Web (WASM)"
  )

  // Configure dependencies and execution
  doFirst {
    logger.lifecycle("\n=== Running UI Tests (@UiTest annotated) ===")
    logger.lifecycle("Target: Tests in org.aryamahasangh.screens package")
    logger.lifecycle("Platforms: Android (instrumented), iOS, Desktop, Web")

    // Check Android device status at execution time
    val hasAndroidDevice = checkAndroidDevice()

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

  // Set up platform test dependencies
  platformTestTasks.forEach { (taskName, platform) ->
    tasks.findByName(taskName)?.let { task ->
      // Only check Android device at execution time
      if (platform == "Android") {
        task.onlyIf {
          checkAndroidDevice().also { hasDevice ->
            if (!hasDevice) {
              logger.warn("Skipping Android tests - no device/emulator connected")
            }
          }
        }
      }

      // Configure test filtering for Gradle Test tasks
      if (task is org.gradle.api.tasks.testing.Test) {
        task.filter {
          includeTestsMatching("org.aryamahasangh.screens.*")
          excludeTestsMatching("org.aryamahasangh.*SimpleTest")
          excludeTestsMatching("org.aryamahasangh.*UnitTest")
          excludeTestsMatching("org.aryamahasangh.*ViewModelTest")
          // Do not fail the whole build when no tests match on a platform (e.g., Desktop UI tests absent)
          isFailOnNoMatchingTests = false
        }
        logger.lifecycle("Configured $taskName for $platform UI tests")
      }

      dependsOn(taskName)
    } ?: logger.warn("Test task $taskName not found for $platform")
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
    val hasAndroidDevice = checkAndroidDevice()
    if (!hasAndroidDevice) {
      println("❌ Android: No devices/emulators connected")
      println("   To run Android tests:")
      println("   1. Connect a device: Enable USB debugging on your Android device")
      println("   2. OR start an emulator: emulator -avd <avd_name>")
      println("   Then run: ./gradlew :composeApp:connectedDebugAndroidTest")
    } else {
      println("✅ Android: Device(s) connected")
    }

    // Check iOS
    val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    if (isMacOS) {
      val hasXcode = try {
        val process = ProcessBuilder("xcodebuild", "-version").start()
        process.waitFor() == 0
      } catch (e: Exception) {
        false
      }

      if (hasXcode) {
        println("\n✅ iOS: macOS with Xcode detected")
      } else {
        println("\n❌ iOS: Xcode not found")
      }
    } else {
      println("\n❌ iOS: Not running on macOS")
    }

    println("\n✅ Desktop: Ready (JVM-based tests)")
    println("\n✅ Web (WASM): Ready (browser-based tests)")

    println("\n=== Summary ===")
    println("To run all UI tests: ./gradlew :composeApp:allUiTests")
  }
}

// Helper function to check Android device (execution time only)
fun checkAndroidDevice(): Boolean {
  return try {
    val process = ProcessBuilder("adb", "devices").start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    output.lines().count { it.contains("\tdevice") || it.contains("\temulator") } > 0
  } catch (e: Exception) {
    false
  }
}

// Test report generation function
fun generateConsolidatedTestReport() {
  val buildDir = project.layout.buildDirectory.get().asFile
  val reportDir = buildDir.resolve("reports/allUiTests")
  reportDir.mkdirs()
  val htmlReport = reportDir.resolve("index.html")
  val debugReport = reportDir.resolve("debug.txt")

  val testResults = mutableMapOf<String, TestPlatformResult>()
  val debugInfo = StringBuilder()

  debugInfo.appendLine("=== UI Test Report Generation ===")
  debugInfo.appendLine("Build directory: ${buildDir.absolutePath}")

  // UI test platforms and their result locations
  val uiTestPlatforms = mapOf(
    "Android" to listOf(
      "reports/androidTests/connected",
      "outputs/androidTest-results/connected",
      "reports/androidTests/connected/debug",
      "outputs/androidTest-results/connected/debug"
    ),
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

  // Parse results for each platform
  uiTestPlatforms.forEach { (platform, paths) ->
    debugInfo.appendLine("\n=== Searching for $platform UI test results ===")

    val platformResults = paths.mapNotNull { relativePath ->
      val dir = file("${buildDir}/$relativePath")
      if (dir.exists()) {
        parseTestResults(dir, platform).takeIf { it.totalTests > 0 }
      } else null
    }

    val finalResult = platformResults.maxByOrNull { it.totalTests } ?: TestPlatformResult(
      platform = platform,
      totalTests = 0,
      passedTests = 0,
      failedTests = 0,
      skippedTests = 0,
      duration = "0.0s",
      testCases = emptyList()
    )

    testResults[platform] = finalResult
  }

  // Write reports
  debugReport.writeText(debugInfo.toString())
  htmlReport.writeText(generateHtmlReport(testResults))

  // Print summary
  println("\n" + "=".repeat(80))
  println("✅ UI Test Report Generated!")
  println("📊 Report location: file://${htmlReport.absolutePath}")
  println("\n📝 UI Test Summary:")
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

// Parse test results function
fun parseTestResults(testDir: File, platform: String): TestPlatformResult {
  val xmlFiles = testDir.walkTopDown()
    .filter {
      it.isFile && it.name.endsWith(".xml") && (
        it.name.startsWith("TEST-") ||
          it.name.contains("test", ignoreCase = true) ||
          it.parent.contains("connected", ignoreCase = true)
        )
    }
    .toList()

  var totalTests = 0
  var passedTests = 0
  var failedTests = 0
  var skippedTests = 0
  var totalDuration = 0.0
  val testCases = mutableListOf<TestCase>()

  xmlFiles.forEach { xmlFile ->
    try {
      val content = xmlFile.readText()

      if (!content.contains("<testsuite") && !content.contains("<testsuites")) {
        return@forEach
      }

      // Parse XML content for test cases
      val testCasePatterns = listOf(
        Regex("""<testcase[^>]+>.*?</testcase>""", RegexOption.DOT_MATCHES_ALL),
        Regex("""<testcase[^>]+/>""")
      )

      testCasePatterns.forEach { pattern ->
        pattern.findAll(content).forEach { matchResult ->
          val testCaseXml = matchResult.value

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
            else -> "PASSED"
          }

          // Only include UI tests from screens package
          if (className.contains("screens") || className.contains("AboutUsScreenTest")) {
            if (!testCases.any { it.name == name && it.className == className }) {
              testCases.add(TestCase(name, className, status, "%.3fs".format(testTime)))
              totalTests++
              when (status) {
                "PASSED" -> passedTests++
                "FAILED" -> failedTests++
                "SKIPPED" -> skippedTests++
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      logger.warn("Failed to parse test results from $xmlFile: ${e.message}")
    }
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

// HTML report generation function
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
