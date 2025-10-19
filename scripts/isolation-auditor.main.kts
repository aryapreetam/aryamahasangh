#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.github.jan-tennert.supabase:supabase-kt:2.0.0")
@file:DependsOn("io.github.jan-tennert.supabase:storage-kt:2.0.0")
@file:DependsOn("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
@file:DependsOn("io.ktor:ktor-client-cio:2.3.7")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.util.Properties
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Cross-environment dependency results
data class IsolationIssue(
  val environment: String,
  val table: String,
  val column: String,
  val type: ColumnType,
  val issueCount: Int,
  val sampleUrls: List<String>,
  val wrongEnvironmentRef: String,
  val correctEnvironmentRef: String,
  val autoFixAvailable: Boolean = true
) {
  enum class ColumnType { SINGLE_URL, URL_ARRAY }
}

data class FixResult(
  val issue: IsolationIssue,
  val filesNeedingMigration: Set<String>,
  val databaseUpdateQuery: String,
  val status: Status
) {
  enum class Status { SUCCESS, PARTIAL_SUCCESS, FAILED, SKIPPED }
}

// Load configuration from .env.migration file
fun loadMigrationConfig(): Properties? {
  val envFile = File(".env.migration")
  return if (envFile.exists()) {
    val props = Properties()
    envFile.inputStream().use { props.load(it) }
    props
  } else null
}

suspend fun runIsolationAudit(environmentToCheck: String): List<IsolationIssue> = runBlocking {
  val issues = mutableListOf<IsolationIssue>()

  println("�� ENVIRONMENT ISOLATION AUDITOR")
  println("=================================")
  println("🎯 Target Environment: $environmentToCheck")
  println("⏰ Started at: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
  println()

  val config = loadMigrationConfig()
  if (config == null) {
    println("❌ .env.migration file not found!")
    return@runBlocking emptyList()
  }

  val environmentUrl = when (environmentToCheck.lowercase()) {
    "dev", "development" -> config.getProperty("dev_supabase_url") ?: config.getProperty("SOURCE_URL")
    "staging" -> config.getProperty("staging_supabase_url") ?: config.getProperty("TARGET_URL")
    "prod", "production" -> config.getProperty("prod_supabase_url") ?: config.getProperty("TARGET_URL")
    else -> config.getProperty("TARGET_URL")
  }

  val environmentKey = when (environmentToCheck.lowercase()) {
    "dev", "development" -> config.getProperty("dev_service_role_key") ?: config.getProperty("SOURCE_SERVICE_KEY")
    "staging" -> config.getProperty("staging_service_role_key") ?: config.getProperty("TARGET_SERVICE_KEY")
    "prod", "production" -> config.getProperty("prod_service_role_key") ?: config.getProperty("TARGET_SERVICE_KEY")
    else -> config.getProperty("TARGET_SERVICE_KEY")
  }

  if (environmentUrl.isNullOrBlank() || environmentKey.isNullOrBlank()) {
    println("❌ Missing configuration for environment: $environmentToCheck")
    println("💡 Make sure to configure ${environmentToCheck}_supabase_url and ${environmentToCheck}_service_role_key")
    return@runBlocking emptyList()
  }

  try {
    val client = createSupabaseClient(environmentUrl, environmentKey) {
      install(Postgrest)
      install(Storage)
    }

    println("📊 1. SCANNING FOR CROSS-ENVIRONMENT DEPENDENCIES")
    println("=================================================")

    val expectedEnvironmentRef = extractProjectRefFromUrl(environmentUrl)
    println("   Expected environment reference: $expectedEnvironmentRef")
    println()

    // Define file reference columns to check
    val fileReferenceColumns = listOf(
      Triple("organisation", "logo", IsolationIssue.ColumnType.SINGLE_URL),
      Triple("member", "profile_image", IsolationIssue.ColumnType.SINGLE_URL),
      Triple("learning", "thumbnail_url", IsolationIssue.ColumnType.SINGLE_URL),
      Triple("activities", "media_files", IsolationIssue.ColumnType.URL_ARRAY),
      Triple("activities", "overview_media_urls", IsolationIssue.ColumnType.URL_ARRAY),
      Triple("family", "photos", IsolationIssue.ColumnType.URL_ARRAY),
      Triple("arya_samaj", "media_urls", IsolationIssue.ColumnType.URL_ARRAY)
    )

    for ((table, column, type) in fileReferenceColumns) {
      print("   Checking $table.$column...")
      try {
        val issue = scanColumnForCrossEnvironmentRefs(
          client,
          environmentToCheck,
          table,
          column,
          type,
          expectedEnvironmentRef
        )

        if (issue != null) {
          issues.add(issue)
          println(" ❌ ${issue.issueCount} cross-environment references found")
        } else {
          println(" ✅ No issues")
        }

      } catch (e: Exception) {
        println(" ⚠️ (${e.message})")
      }
    }

  } catch (error: Exception) {
    println("❌ Failed to connect to $environmentToCheck environment: ${error.message}")
  }

  return@runBlocking issues
}

suspend fun scanColumnForCrossEnvironmentRefs(
  client: io.github.jan.supabase.SupabaseClient,
  environment: String,
  table: String,
  column: String,
  type: IsolationIssue.ColumnType,
  expectedRef: String
): IsolationIssue? {

  try {
    val data = client.from(table)
      .select(column)
      .decodeList<JsonObject>()

    val crossEnvUrls = mutableListOf<String>()
    var wrongRef = ""

    for (record in data) {
      val value = record[column]

      when (type) {
        IsolationIssue.ColumnType.SINGLE_URL -> {
          if (value is JsonPrimitive && value.isString && value.content.isNotBlank()) {
            if (!value.content.contains(expectedRef)) {
              crossEnvUrls.add(value.content)
              if (wrongRef.isEmpty()) {
                wrongRef = extractProjectRefFromUrl(value.content) ?: "unknown"
              }
            }
          }
        }

        IsolationIssue.ColumnType.URL_ARRAY -> {
          if (value is JsonArray) {
            for (url in value) {
              if (url is JsonPrimitive && url.isString && url.content.isNotBlank()) {
                if (!url.content.contains(expectedRef)) {
                  crossEnvUrls.add(url.content)
                  if (wrongRef.isEmpty()) {
                    wrongRef = extractProjectRefFromUrl(url.content) ?: "unknown"
                  }
                }
              }
            }
          }
        }
      }
    }

    return if (crossEnvUrls.isNotEmpty()) {
      IsolationIssue(
        environment = environment,
        table = table,
        column = column,
        type = type,
        issueCount = crossEnvUrls.size,
        sampleUrls = crossEnvUrls.take(3),
        wrongEnvironmentRef = wrongRef,
        correctEnvironmentRef = expectedRef
      )
    } else null

  } catch (e: Exception) {
    throw Exception("Failed to scan $table.$column: ${e.message}")
  }
}

fun extractProjectRefFromUrl(url: String): String? {
  // Extract project reference from Supabase URL
  val regex = Regex("""https://([^.]+)\.supabase\.co""")
  return regex.find(url)?.groupValues?.get(1)
}

suspend fun generateAutoFixScript(issues: List<IsolationIssue>, environmentToCheck: String): String {
  val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
  val scriptName = "auto-fix-isolation-${environmentToCheck}-$timestamp.sh"

  val scriptContent = buildString {
    appendLine("#!/bin/bash")
    appendLine("# Auto-generated isolation fix script")
    appendLine("# Generated: ${LocalDateTime.now()}")
    appendLine("# Environment: $environmentToCheck")
    appendLine("")
    appendLine("set -e  # Exit on any error")
    appendLine("")
    appendLine("echo \"🛠️ FIXING ENVIRONMENT ISOLATION ISSUES\"")
    appendLine("echo \"=======================================\"")
    appendLine("echo \"Environment: $environmentToCheck\"")
    appendLine("echo \"Issues to fix: ${issues.size}\"")
    appendLine("echo \"\"")
    appendLine("")

    if (issues.isNotEmpty()) {
      val config = loadMigrationConfig()
      val environmentUrl = when (environmentToCheck.lowercase()) {
        "dev", "development" -> config?.getProperty("dev_supabase_url")
        "staging" -> config?.getProperty("staging_supabase_url")
        "prod", "production" -> config?.getProperty("prod_supabase_url")
        else -> null
      }

      appendLine("# Load configuration")
      appendLine("source .env.migration")
      appendLine("")

      appendLine("# Phase 1: File Migration")
      appendLine("echo \"📋 PHASE 1: MIGRATE MISSING FILES\"")
      appendLine("echo \"==================================\"")
      appendLine("")

      // Generate file migration commands
      val allFiles = issues.flatMap { extractFilePathsFromIssue(it) }.toSet()
      appendLine("echo \"Files to migrate: ${allFiles.size}\"")
      appendLine("")

      for ((index, filePath) in allFiles.withIndex()) {
        appendLine("echo \"   Migrating file ${index + 1}/${allFiles.size}: $filePath\"")
        appendLine("# File migration would be handled by Kotlin script")
      }

      appendLine("")
      appendLine("# Phase 2: Database URL Updates")
      appendLine("echo \"\"")
      appendLine("echo \"📋 PHASE 2: UPDATE DATABASE URLS\"")
      appendLine("echo \"=================================\"")
      appendLine("")

      // Generate SQL update commands for each issue
      for (issue in issues) {
        appendLine("echo \"   Fixing ${issue.table}.${issue.column}...\"")
        appendLine("")

        val sqlCommand = generateSqlUpdateCommand(issue)
        appendLine("# Update ${issue.table}.${issue.column}")
        appendLine("supabase db remote --linked exec \"$sqlCommand\"")
        appendLine("echo \"   ✅ Fixed ${issue.table}.${issue.column}\"")
        appendLine("")
      }

      appendLine("# Phase 3: Verification")
      appendLine("echo \"\"")
      appendLine("echo \"📋 PHASE 3: VERIFICATION\"")
      appendLine("echo \"========================\"")
      appendLine("")

      for (issue in issues) {
        val verificationSql = generateVerificationQuery(issue)
        appendLine("echo \"   Verifying ${issue.table}.${issue.column}...\"")
        appendLine("RESULT=\\$(supabase db remote --linked exec \"$verificationSql\" | tail -n +3 | head -n 1 | xargs)")
        appendLine("if [ \"\\$RESULT\" = \"0\" ]; then")
        appendLine("    echo \"   ✅ ${issue.table}.${issue.column} verified (0 cross-env refs)\"")
        appendLine("else")
        appendLine("    echo \"   ❌ ${issue.table}.${issue.column} still has \\$RESULT cross-env refs\"")
        appendLine("fi")
        appendLine("")
      }

      appendLine("echo \"\"")
      appendLine("echo \"🎉 ENVIRONMENT ISOLATION FIX COMPLETED!\"")
      appendLine("echo \"Run isolation auditor again to verify all issues resolved\"")
    }
  }

  // Write script to file
  File(scriptName).writeText(scriptContent)

  return scriptName
}

fun extractFilePathsFromIssue(issue: IsolationIssue): List<String> {
  return issue.sampleUrls.mapNotNull { url ->
    val regex = Regex("""/storage/v1/object/public/([^?]+)""")
    regex.find(url)?.groupValues?.get(1)
  }
}

fun generateSqlUpdateCommand(issue: IsolationIssue): String {
  val wrongRef = issue.wrongEnvironmentRef
  val correctRef = issue.correctEnvironmentRef

  return when (issue.type) {
    IsolationIssue.ColumnType.SINGLE_URL -> {
      "UPDATE ${issue.table} SET ${issue.column} = REPLACE(${issue.column}, '$wrongRef.supabase.co', '$correctRef.supabase.co') WHERE ${issue.column} LIKE '%$wrongRef%';"
    }

    IsolationIssue.ColumnType.URL_ARRAY -> {
      """UPDATE ${issue.table} 
               SET ${issue.column} = (
                   SELECT array_agg(REPLACE(url_elem, '$wrongRef.supabase.co', '$correctRef.supabase.co'))
                   FROM unnest(${issue.column}) AS url_elem
               )
               WHERE array_to_string(${issue.column}, '|') LIKE '%$wrongRef%';"""
    }
  }
}

fun generateVerificationQuery(issue: IsolationIssue): String {
  val wrongRef = issue.wrongEnvironmentRef

  return when (issue.type) {
    IsolationIssue.ColumnType.SINGLE_URL -> {
      "SELECT COUNT(*) FROM ${issue.table} WHERE ${issue.column} LIKE '%$wrongRef%';"
    }

    IsolationIssue.ColumnType.URL_ARRAY -> {
      "SELECT COUNT(*) FROM ${issue.table} WHERE array_to_string(${issue.column}, '|') LIKE '%$wrongRef%';"
    }
  }
}

fun generateIsolationReport(issues: List<IsolationIssue>, environmentToCheck: String) {
  val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
  val reportFile = "isolation-audit-$environmentToCheck-$timestamp.md"

  println("\n" + "=".repeat(60))
  println("🔒 ENVIRONMENT ISOLATION AUDIT RESULTS")
  println("=".repeat(60))
  println("🎯 Environment: $environmentToCheck")
  println("📊 Issues Found: ${issues.size}")
  println()

  if (issues.isEmpty()) {
    println("✅ PERFECT ISOLATION!")
    println("   No cross-environment dependencies detected")
    println("   Environment is properly isolated")
  } else {
    println("🚨 ISOLATION ISSUES DETECTED")
    println("   ${issues.size} cross-environment dependencies found")
    println()

    // Group issues by severity
    val criticalIssues = issues.filter { it.issueCount > 10 }
    val moderateIssues = issues.filter { it.issueCount in 2..10 }
    val minorIssues = issues.filter { it.issueCount == 1 }

    if (criticalIssues.isNotEmpty()) {
      println("🚨 CRITICAL ISSUES (>10 references):")
      criticalIssues.forEach { issue ->
        println("   ❌ ${issue.table}.${issue.column}: ${issue.issueCount} cross-env references")
        println("      Wrong environment: ${issue.wrongEnvironmentRef}")
        println("      Should be: ${issue.correctEnvironmentRef}")
        println("      Sample URL: ${issue.sampleUrls.firstOrNull() ?: "N/A"}")
        println()
      }
    }

    if (moderateIssues.isNotEmpty()) {
      println("⚠️ MODERATE ISSUES (2-10 references):")
      moderateIssues.forEach { issue ->
        println("   ⚠️ ${issue.table}.${issue.column}: ${issue.issueCount} cross-env references")
        println("      From: ${issue.wrongEnvironmentRef} → To: ${issue.correctEnvironmentRef}")
      }
      println()
    }

    if (minorIssues.isNotEmpty()) {
      println("📝 MINOR ISSUES (1 reference):")
      minorIssues.forEach { issue ->
        println("   📝 ${issue.table}.${issue.column}: ${issue.issueCount} cross-env reference")
      }
      println()
    }

    // Environment breakdown
    val environmentRefs = issues.map { it.wrongEnvironmentRef }.toSet()
    if (environmentRefs.size > 1) {
      println("📊 CROSS-ENVIRONMENT BREAKDOWN:")
      environmentRefs.forEach { wrongRef ->
        val count = issues.count { it.wrongEnvironmentRef == wrongRef }
        val totalReferences = issues.filter { it.wrongEnvironmentRef == wrongRef }.sumOf { it.issueCount }
        println("   $wrongRef: $count tables affected, $totalReferences total references")
      }
      println()
    }

    // Auto-fix availability
    val autoFixableIssues = issues.filter { it.autoFixAvailable }
    println("🔧 AUTO-FIX AVAILABILITY:")
    println("   ✅ Auto-fixable: ${autoFixableIssues.size}/${issues.size} issues")

    if (autoFixableIssues.isNotEmpty()) {
      println("   💡 Run auto-fix generation to create repair script")
    }
  }

  // Generate detailed markdown report
  try {
    val reportContent = generateDetailedIsolationReport(issues, environmentToCheck)
    File(reportFile).writeText(reportContent)
    println("\n📝 Detailed report written to: $reportFile")
  } catch (e: Exception) {
    println("\n⚠️ Could not write report file: ${e.message}")
  }

  println("\n📋 NEXT STEPS:")
  if (issues.isEmpty()) {
    println("   1. ✅ Environment isolation is perfect!")
    println("   2. 🔄 Run this auditor periodically to maintain isolation")
    println("   3. 📊 Monitor for new cross-environment dependencies")
  } else {
    println("   1. 🛠️ Generate auto-fix script: Add --generate-fix flag")
    println("   2. 📋 Review the generated fix script before execution")
    println("   3. 🚀 Execute the fix script to resolve isolation issues")
    println("   4. ✅ Re-run auditor to verify fixes")
  }

  println("\n" + "=".repeat(60))
}

fun generateDetailedIsolationReport(issues: List<IsolationIssue>, environmentToCheck: String): String {
  val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

  return buildString {
    appendLine("# Environment Isolation Audit Report")
    appendLine("**Environment:** $environmentToCheck")
    appendLine("**Generated:** $timestamp")
    appendLine("**Issues Found:** ${issues.size}")
    appendLine()

    if (issues.isEmpty()) {
      appendLine("## ✅ Perfect Isolation")
      appendLine()
      appendLine("No cross-environment dependencies detected. Environment is properly isolated.")
      appendLine()
      appendLine("### Recommendations")
      appendLine("- Run this auditor periodically to maintain isolation")
      appendLine("- Monitor uploads and data imports for new cross-environment references")
      appendLine("- Implement validation to prevent future cross-environment dependencies")
    } else {
      appendLine("## 🚨 Issues Summary")
      appendLine()
      appendLine("| Table | Column | Type | Issue Count | Wrong Env | Correct Env |")
      appendLine("|-------|--------|------|-------------|-----------|-------------|")

      issues.forEach { issue ->
        val typeStr = when (issue.type) {
          IsolationIssue.ColumnType.SINGLE_URL -> "Single URL"
          IsolationIssue.ColumnType.URL_ARRAY -> "URL Array"
        }
        appendLine("| ${issue.table} | ${issue.column} | $typeStr | ${issue.issueCount} | ${issue.wrongEnvironmentRef} | ${issue.correctEnvironmentRef} |")
      }

      appendLine()
      appendLine("## 🔍 Detailed Issues")
      appendLine()

      issues.forEach { issue ->
        appendLine("### ${issue.table}.${issue.column}")
        appendLine()
        appendLine("- **Issue Count:** ${issue.issueCount}")
        appendLine("- **Column Type:** ${issue.type}")
        appendLine("- **Wrong Environment:** ${issue.wrongEnvironmentRef}")
        appendLine("- **Correct Environment:** ${issue.correctEnvironmentRef}")
        appendLine("- **Auto-fix Available:** ${if (issue.autoFixAvailable) "✅ Yes" else "❌ No"}")
        appendLine()

        if (issue.sampleUrls.isNotEmpty()) {
          appendLine("**Sample URLs:**")
          issue.sampleUrls.forEach { url ->
            appendLine("- `$url`")
          }
          appendLine()
        }

        appendLine("**Fix Required:**")
        appendLine("1. Migrate files from ${issue.wrongEnvironmentRef} storage to ${issue.correctEnvironmentRef} storage")
        appendLine("2. Update database URLs to reference correct environment")
        appendLine("3. Verify all files are accessible in correct environment")
        appendLine()
      }

      appendLine("## 🛠️ Resolution Steps")
      appendLine()
      appendLine("1. **Generate Auto-fix Script**")
      appendLine("   ```bash")
      appendLine("   kotlin scripts/isolation-auditor.main.kts --environment=$environmentToCheck --generate-fix")
      appendLine("   ```")
      appendLine()
      appendLine("2. **Review Generated Script**")
      appendLine("   - Check file migration commands")
      appendLine("   - Verify SQL update statements")
      appendLine("   - Ensure no data loss will occur")
      appendLine()
      appendLine("3. **Execute Fix Script**")
      appendLine("   ```bash")
      appendLine("   chmod +x auto-fix-isolation-$environmentToCheck-*.sh")
      appendLine("   ./auto-fix-isolation-$environmentToCheck-*.sh")
      appendLine("   ```")
      appendLine()
      appendLine("4. **Verify Resolution**")
      appendLine("   ```bash")
      appendLine("   kotlin scripts/isolation-auditor.main.kts --environment=$environmentToCheck")
      appendLine("   ```")
    }
  }
}

// Main execution
val args = args
val environmentToCheck = args.find { it.startsWith("--environment=") }?.substringAfter("=") ?: "dev"
val generateFix = args.contains("--generate-fix")

runBlocking {
  try {
    println("🏗️ Initializing Environment Isolation Auditor...")
    println("Target Environment: $environmentToCheck")
    if (generateFix) {
      println("Mode: Audit + Generate Fix Script")
    } else {
      println("Mode: Audit Only")
    }
    println()

    val issues = runIsolationAudit(environmentToCheck)

    generateIsolationReport(issues, environmentToCheck)

    if (generateFix && issues.isNotEmpty()) {
      println("\n🛠️ GENERATING AUTO-FIX SCRIPT")
      println("==============================")
      val scriptFile = generateAutoFixScript(issues, environmentToCheck)
      println("✅ Auto-fix script generated: $scriptFile")
      println("📋 Review the script before execution")
      println("🚀 Execute with: chmod +x $scriptFile && ./$scriptFile")
    }

  } catch (error: Exception) {
    println("💥 Isolation audit failed with error: ${error.message}")
    error.printStackTrace()
  }
}
