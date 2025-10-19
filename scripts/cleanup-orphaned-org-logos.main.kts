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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

// Configuration
object CleanupConfig {
  val prodUrl = System.getenv("PROD_URL") ?: "https://jusbsyslwvrdmdwdsvfk.supabase.co"
  val prodServiceKey = System.getenv("PROD_SERVICE_KEY") ?: run {
    println("⚠️ PROD_SERVICE_KEY not provided - you'll need to set it as environment variable")
    println("Example: export PROD_SERVICE_KEY='your-prod-service-key'")
    throw IllegalArgumentException("PROD_SERVICE_KEY environment variable required")
  }
}

val prodClient = createSupabaseClient(
  supabaseUrl = CleanupConfig.prodUrl,
  supabaseKey = CleanupConfig.prodServiceKey
) {
  install(Storage)
  install(Postgrest)
}

data class OrganizationLogo(
  val id: String,
  val name: String,
  val logo: String?
)

suspend fun cleanupOrphanedOrgLogos(): Pair<Int, Int> = runBlocking {
  println("🧹 SUPABASE STORAGE CLEANUP: Orphaned org_logo_*.jpg Files")
  println("===========================================================")
  println("🎯 Target: ${CleanupConfig.prodUrl}")
  println("📁 Bucket: documents")
  println("🔍 Pattern: org_logo_*.jpg")
  println()

  try {
    // Step 1: Get all org_logo_*.jpg files from production storage
    println("📋 Step 1: Listing all org_logo_*.jpg files in storage...")
    val allOrgLogos = getAllOrgLogoFiles()
    println("   Found ${allOrgLogos.size} org_logo_*.jpg files")

    if (allOrgLogos.isEmpty()) {
      println("✅ No org_logo_*.jpg files found in storage")
      return@runBlocking Pair(0, 0)
    }

    // Step 2: Get legitimate files from database
    println("📋 Step 2: Getting legitimate org logo files from database...")
    val legitimateLogos = getLegitimateOrgLogos()
    println("   Found ${legitimateLogos.size} legitimate logo files in database")

    // Step 3: Extract filename from legitimate URLs for comparison
    val legitimateFilenames = legitimateLogos.mapNotNull { logo ->
      extractFilenameFromUrl(logo.logo)
    }.toSet()

    println("📋 Step 3: Legitimate org_logo_*.jpg filenames:")
    legitimateFilenames.forEach { filename ->
      println("   ✅ $filename")
    }

    // Step 4: Identify orphaned files
    val orphanedFiles = allOrgLogos.filter { filename ->
      !legitimateFilenames.contains(filename)
    }

    println()
    println("🔍 Analysis Results:")
    println("   📂 Total org_logo_*.jpg files in storage: ${allOrgLogos.size}")
    println("   ✅ Legitimate files (referenced in database): ${legitimateFilenames.size}")
    println("   🗑️ Orphaned files (to be deleted): ${orphanedFiles.size}")

    if (orphanedFiles.isEmpty()) {
      println()
      println("🎉 NO ORPHANED FILES FOUND!")
      println("✅ All org_logo_*.jpg files are properly referenced in the database")
      return@runBlocking Pair(0, 0)
    }

    println()
    println("🗑️ Orphaned files to be deleted:")
    orphanedFiles.forEachIndexed { index, filename ->
      println("   ${index + 1}. $filename")
    }

    // Step 5: Confirm deletion
    println()
    println("⚠️ WARNING: This will permanently delete ${orphanedFiles.size} files!")
    print("Are you sure you want to proceed? (yes/no): ")
    val confirmation = readLine()?.lowercase()

    if (confirmation != "yes") {
      println("❌ Operation cancelled by user")
      return@runBlocking Pair(0, 0)
    }

    // Step 6: Delete orphaned files
    println()
    println("🗑️ Step 6: Deleting orphaned files...")
    var successCount = 0
    var failCount = 0

    for ((index, filename) in orphanedFiles.withIndex()) {
      try {
        print("   Deleting ${index + 1}/${orphanedFiles.size}: $filename...")
        deleteOrgLogoFile(filename)
        successCount++
        println(" ✅")
      } catch (error: Exception) {
        failCount++
        println(" ❌ (${error.message})")
      }
    }

    println()
    println("🎉 CLEANUP SUMMARY:")
    println("   ✅ Successfully deleted: $successCount files")
    println("   ⚠️ Failed to delete: $failCount files")
    println("   📂 Remaining org_logo_*.jpg files: ${legitimateFilenames.size}")

    // Final verification
    println()
    println("🔍 Final verification...")
    val remainingFiles = getAllOrgLogoFiles()
    println("   📂 Current org_logo_*.jpg files in storage: ${remainingFiles.size}")

    if (remainingFiles.size == legitimateFilenames.size) {
      println("✅ CLEANUP SUCCESSFUL!")
      println("🎯 Exactly ${legitimateFilenames.size} org_logo_*.jpg files remain (as expected)")
    } else {
      println("⚠️ Unexpected file count after cleanup")
      println("   Expected: ${legitimateFilenames.size}")
      println("   Actual: ${remainingFiles.size}")
    }

    Pair(successCount, failCount)

  } catch (error: Exception) {
    println("❌ Cleanup failed: ${error.message}")
    error.printStackTrace()
    Pair(0, 0)
  }
}

suspend fun getAllOrgLogoFiles(): List<String> {
  try {
    val files = prodClient.storage
      .from("documents")
      .list()

    return files.filter { file ->
      file.name.startsWith("org_logo_") && file.name.endsWith(".jpg")
    }.map { it.name }
  } catch (error: Exception) {
    throw Exception("Failed to list files from storage: ${error.message}")
  }
}

suspend fun getLegitimateOrgLogos(): List<OrganizationLogo> {
  try {
    val data = prodClient.from("organisation")
      .select(columns = arrayOf("id", "name", "logo"))
      .decodeList<JsonObject>()
      .map { record ->
        OrganizationLogo(
          id = record["id"]?.jsonPrimitive?.content ?: "",
          name = record["name"]?.jsonPrimitive?.content ?: "",
          logo = record["logo"]?.jsonPrimitive?.contentOrNull
        )
      }
      .filter { it.logo != null && it.logo.contains("org_logo_") && it.logo.contains(".jpg") }

    return data
  } catch (error: Exception) {
    throw Exception("Failed to query organization logos: ${error.message}")
  }
}

fun extractFilenameFromUrl(url: String?): String? {
  if (url == null) return null

  // Extract filename from Supabase storage URL
  // Example: https://jusbsyslwvrdmdwdsvfk.supabase.co/storage/v1/object/public/documents/org_logo_1748535599.jpg
  val regex = Regex("""/documents/(org_logo_\d+\.jpg)""")
  return regex.find(url)?.groupValues?.get(1)
}

suspend fun deleteOrgLogoFile(filename: String) {
  try {
    prodClient.storage
      .from("documents")
      .delete(filename)
  } catch (error: Exception) {
    throw Exception("Failed to delete $filename: ${error.message}")
  }
}

// Main execution
println("🏗️ Initializing Supabase Storage Cleanup...")
try {
  val (successCount, failCount) = runBlocking { cleanupOrphanedOrgLogos() }

  println()
  if (failCount == 0 && successCount > 0) {
    println("🎉 CLEANUP COMPLETED SUCCESSFULLY!")
    println("📋 All orphaned org_logo_*.jpg files have been removed")
  } else if (successCount == 0 && failCount == 0) {
    println("✅ NO ACTION NEEDED")
    println("📋 All org_logo_*.jpg files are properly referenced")
  } else {
    println("⚠️ CLEANUP COMPLETED WITH SOME FAILURES")
    println("📋 Check failed files and retry if needed")
  }

} catch (error: Exception) {
  println("💥 Cleanup failed with error: ${error.message}")
  error.printStackTrace()
}
