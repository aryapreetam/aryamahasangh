plugins {
  // this is necessary to avoid the plugins to be loaded multiple times
  // in each subproject's classloader
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.composeHotReload) apply false
  alias(libs.plugins.kotlinx.serialization).apply(false)
  alias(libs.plugins.apollo).apply(false)
}

// Global task to fix $ characters in generated Secrets.kt files
// This applies to all subprojects that use kmp-secrets-plugin
allprojects {
  tasks.register("fixGeneratedSecrets") {
    // Capture values at configuration time for configuration cache compatibility
    val projectName = project.name
    val secretsFileProvider = layout.buildDirectory
      .dir("generated/kmp-secrets/commonMain/kotlin/secrets/Secrets.kt")
      .map { it.asFile }

    doLast {
      val secretsFile = secretsFileProvider.get()

      if (secretsFile.exists()) {
        println("[$projectName] Fixing dollar signs in ${secretsFile.absolutePath}")
        val content = secretsFile.readText()

        // Replace unescaped $ with ${'$'} in all const val string assignments
        val fixedContent = content.lines().joinToString("\n") { line ->
          if (line.trim().startsWith("const val") && line.contains("= \"") && line.contains("$") && !line.contains("\${'$'}")) {
            // Match const val declarations and escape $ in the string value
            line.replace(Regex("""(const val \w+ = ")([^"]*?)"""")) { matchResult ->
              val prefix = matchResult.groupValues[1]
              val stringContent = matchResult.groupValues[2]
              // Only replace $ that aren't already escaped
              val escapedContent = stringContent.replace("$", "\${'$'}")
              prefix + escapedContent + "\""
            }
          } else {
            line
          }
        }

        if (content != fixedContent) {
          secretsFile.writeText(fixedContent)
          println("[$projectName] ✓ Fixed dollar signs in Secrets.kt")
        } else {
          println("[$projectName] No dollar signs to fix in Secrets.kt")
        }
      } else {
        println("[$projectName] Secrets.kt not found - skipping")
      }
    }
  }
}

// Hook the fix task after generateSecrets in subprojects
subprojects {
  afterEvaluate {
    tasks.matching {
      it.name.startsWith("generateSecrets") && !it.name.contains("Test")
    }.configureEach {
      finalizedBy("fixGeneratedSecrets")
    }
  }
}

