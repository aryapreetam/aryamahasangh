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

// Load credentials from local.properties
fun loadProperties(): Properties {
    val properties = Properties()
    val localPropsFile = File("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { properties.load(it) }
    } else {
        throw IllegalArgumentException("local.properties file not found. Please ensure it exists in the project root.")
    }
    return properties
}

val props = loadProperties()

// Configuration from local.properties
object StorageMigrationConfig {
    val stagingUrl = props.getProperty("staging_supabase_url") ?: throw IllegalArgumentException("staging_supabase_url not found in local.properties")
    val stagingServiceKey = props.getProperty("staging_service_role_key") ?: throw IllegalArgumentException("staging_service_role_key not found in local.properties")
    val devUrl = props.getProperty("dev_supabase_url") ?: throw IllegalArgumentException("dev_supabase_url not found in local.properties") 
    val devServiceKey = props.getProperty("dev_service_role_key") ?: throw IllegalArgumentException("dev_service_role_key not found in local.properties")
}

val stagingClient = createSupabaseClient(
    supabaseUrl = StorageMigrationConfig.stagingUrl,
    supabaseKey = StorageMigrationConfig.stagingServiceKey
) {
    install(Storage)
    install(Postgrest)
}

val devClient = createSupabaseClient(
    supabaseUrl = StorageMigrationConfig.devUrl,
    supabaseKey = StorageMigrationConfig.devServiceKey
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
    println("🚀 SUPABASE STORAGE MIGRATION: STAGING → DEV")
    println("===========================================")
    println("📁 Source:  ${StorageMigrationConfig.stagingUrl}")
    println("📁 Target:  ${StorageMigrationConfig.devUrl}")
    println("📋 Purpose: Fix cross-environment dependencies (staging files in dev database)")
    println()

    try {
        // Get all referenced files from dev database that point to staging
        println("🔍 Scanning dev database for staging file references...")
        val referencedFiles = getReferencedFiles()
        println("📄 Found ${referencedFiles.size} files that need migration")

        if (referencedFiles.isEmpty()) {
            println("✅ No staging file references found in dev database!")
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
            println("   • File already exists in dev storage")
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

    println("   🔍 Scanning database tables for staging file references...")

    // Query dev database for files that reference staging environment
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

            val data = devClient.from(table)
                .select(columns = arrayOf(column))
                .decodeList<JsonObject>()
                .filter { 
                    val value = it[column]
                    when (type) {
                        "single" -> value is JsonPrimitive && value.isString && value.content.contains("ftnwwiwmljcwzpsawdmf")
                        "array" -> value is JsonArray && value.any { url -> 
                            url is JsonPrimitive && url.isString && url.content.contains("ftnwwiwmljcwzpsawdmf")
                        }
                        else -> false
                    }
                }

            var fileCount = 0
            for (record in data) {
                val value = record[column]

                when (type) {
                    "single" -> {
                        if (value is JsonPrimitive && value.isString && value.content.contains("ftnwwiwmljcwzpsawdmf")) {
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
                                if (url is JsonPrimitive && url.isString && url.content.contains("ftnwwiwmljcwzpsawdmf")) {
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
            println(" found $fileCount staging references")

        } catch (error: Exception) {
            println(" ❌ (${error.message})")
        }
    }

    // Remove duplicates based on file path
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

        // Upload file to dev environment
        devClient.storage
            .from(bucket)
            .upload(filePath, fileBytes) {
                upsert = true // Allow overwrite if file exists
            }

    } catch (error: Exception) {
        throw Exception("Failed to migrate ${fileInfo.path}: ${error.message}")
    } finally {
        httpClient.close()
    }
}

// Main execution
println("🏗️ Initializing Supabase Storage Migration (Staging → Dev)...")
try {
    val (successCount, failCount) = runBlocking { migrateStorageFiles() }

    println()
    if (failCount == 0) {
        println("🎉 ALL FILES MIGRATED SUCCESSFULLY!")
        println("📋 Next step: Run URL update script to fix database references")
        println("💡 Command: kotlin scripts/update-urls-staging-to-dev.main.kts")
    } else {
        println("⚠️ MIGRATION COMPLETED WITH SOME FAILURES")
        println("📋 You may still want to run the URL update script for successfully migrated files")
    }

} catch (error: Exception) {
    println("💥 Migration failed with error: ${error.message}")
    error.printStackTrace()
}
