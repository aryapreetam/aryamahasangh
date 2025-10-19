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

// Configuration
object StorageMigrationConfig {
  val stagingUrl = System.getenv("STAGING_URL") ?: "https://ftnwwiwmljcwzpsawdmf.supabase.co"
  val stagingServiceKey = System.getenv("STAGING_SERVICE_KEY") ?: run {
    println("⚠️ STAGING_SERVICE_KEY not provided - you'll need to set it as environment variable")
    println("Example: export STAGING_SERVICE_KEY='your-staging-service-key'")
    throw IllegalArgumentException("STAGING_SERVICE_KEY environment variable required")
  }
  val prodUrl = System.getenv("PROD_URL") ?: "https://jusbsyslwvrdmdwdsvfk.supabase.co"
  val prodServiceKey = System.getenv("PROD_SERVICE_KEY") ?: run {
    println("⚠️ PROD_SERVICE_KEY not provided - you'll need to set it as environment variable")
    println("Example: export PROD_SERVICE_KEY='your-prod-service-key'")
    throw IllegalArgumentException("PROD_SERVICE_KEY environment variable required")
  }
}

val stagingClient = createSupabaseClient(
  supabaseUrl = StorageMigrationConfig.stagingUrl,
  supabaseKey = StorageMigrationConfig.stagingServiceKey
) {
  install(Storage)
  install(Postgrest)
}

val prodClient = createSupabaseClient(
  supabaseUrl = StorageMigrationConfig.prodUrl,
  supabaseKey = StorageMigrationConfig.prodServiceKey
) {
  install(Storage)
  install(Postgrest)
}

data class FileReference(
  val path: String,
  val originalUrl: String,
  val table: String,
  val column: String
)

suspend fun migrateStorageFiles(): Pair<Int, Int> = runBlocking {
  println("🚀 SUPABASE STORAGE MIGRATION: STAGING → PRODUCTION")
  println("====================================================")
  println("📁 Source:  ${StorageMigrationConfig.stagingUrl}")
  println("📁 Target:  ${StorageMigrationConfig.prodUrl}")
  println("📋 Note: Only migrating files actually referenced in production database")
  println()

  try {
    // Get all referenced files from production database
    println("🔍 Scanning production database for file references...")
    val referencedFiles = getReferencedFiles()
    println("📄 Found ${referencedFiles.size} referenced files")

    if (referencedFiles.isEmpty()) {
      println("⏭️ No files to migrate")
      return@runBlocking Pair(0, 0)
    }

    println("📋 Files to migrate:")
    referencedFiles.forEachIndexed { index, file ->
      println("   ${index + 1}. ${file.path} (from ${file.table}.${file.column})")
    }
    println()

    var successCount = 0
    var failCount = 0

    println("📦 Starting file migration...")
    for ((index, fileInfo) in referencedFiles.withIndex()) {
      try {
        print("   Migrating ${index + 1}/${referencedFiles.size}: ${fileInfo.path}...")
        migrateFile(fileInfo)
        successCount++
        println(" ✅")
      } catch (error: Exception) {
        failCount++
        println(" ❌ (${error.message})")
      }
    }

    println()
    println("🎉 STORAGE MIGRATION SUMMARY:")
    println("   ✅ Successfully migrated: $successCount files")
    println("   ⚠️ Failed to migrate: $failCount files")

    if (failCount > 0) {
      println()
      println("⚠️ Some files failed to migrate. Common reasons:")
      println("   • File doesn't exist in staging storage")
      println("   • Network connectivity issues")
      println("   • Insufficient storage permissions")
      println("   • File size limitations")
    }

    Pair(successCount, failCount)

  } catch (error: Exception) {
    println("❌ Storage migration failed: ${error.message}")
    error.printStackTrace()
    Pair(0, 0)
  }
}

suspend fun getReferencedFiles(): List<FileReference> {
  val fileReferences = mutableListOf<FileReference>()

  println("   🔍 Scanning database tables for file references...")

  // Query all tables that contain file references in production
  val queries = listOf(
    Triple("organisation", "logo", "single"),
    Triple("member", "profile_image", "single"),
    Triple("activities", "media_files", "array"),
    Triple("activities", "overview_media_urls", "array"),
    Triple("learning", "thumbnail_url", "single")
  )

  for ((table, column, type) in queries) {
    try {
      print("   📊 Checking $table.$column...")

      val data = prodClient.from(table)
        .select(columns = arrayOf(column))
        .decodeList<JsonObject>()
        .filter { it[column] != null && it[column] != JsonNull }

      var fileCount = 0
      for (record in data) {
        val value = record[column]

        when (type) {
          "single" -> {
            if (value is JsonPrimitive && value.isString) {
              val path = extractFilePathFromUrl(value.content)
              if (path != null) {
                fileReferences.add(
                  FileReference(path, value.content, table, column)
                )
                fileCount++
              }
            }
          }

          "array" -> {
            if (value is JsonArray) {
              for (url in value) {
                if (url is JsonPrimitive && url.isString) {
                  val path = extractFilePathFromUrl(url.content)
                  if (path != null) {
                    fileReferences.add(
                      FileReference(path, url.content, table, column)
                    )
                    fileCount++
                  }
                }
              }
            }
          }
        }
      }
      println(" found $fileCount files")

    } catch (error: Exception) {
      println(" ❌ (${error.message})")
    }
  }

  // Remove duplicates
  return fileReferences.distinctBy { it.path }
}

fun extractFilePathFromUrl(url: String): String? {
  // Extract path from Supabase storage URL
  val regex = Regex("""/storage/v1/object/public/([^?]+)""")
  return regex.find(url)?.groupValues?.get(1)
}

suspend fun migrateFile(fileInfo: FileReference) {
  val httpClient = HttpClient(CIO)

  try {
    // Download file from staging
    val stagingUrl = "${StorageMigrationConfig.stagingUrl}/storage/v1/object/public/${fileInfo.path}"
    val fileBytes = httpClient.get(stagingUrl).readBytes()

    // Extract bucket and file path
    val pathParts = fileInfo.path.split('/', limit = 2)
    if (pathParts.size != 2) {
      throw Exception("Invalid file path format: ${fileInfo.path}")
    }

    val bucket = pathParts[0]
    val filePath = pathParts[1]

    // Upload file to production
    prodClient.storage
      .from(bucket)
      .upload(filePath, fileBytes) {
        upsert = true
      }

  } catch (error: Exception) {
    throw Exception("Failed to migrate ${fileInfo.path}: ${error.message}")
  } finally {
    httpClient.close()
  }
}

// Main execution
println("🏗️ Initializing Supabase Storage Migration...")
try {
  val (successCount, failCount) = runBlocking { migrateStorageFiles() }

  println()
  if (failCount == 0) {
    println("🎉 ALL FILES MIGRATED SUCCESSFULLY!")
    println("📋 Next step: Run URL update script to fix file references")
  } else {
    println("⚠️ MIGRATION COMPLETED WITH SOME FAILURES")
    println("📋 Next step: Check failed files and run URL update script")
  }

} catch (error: Exception) {
  println("💥 Migration failed with error: ${error.message}")
  error.printStackTrace()
}
