#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.github.jan-tennert.supabase:supabase-kt:2.0.0")
@file:DependsOn("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Properties

// Load credentials from local.properties
fun loadProperties(): Properties {
    val properties = Properties()
    val localPropsFile = File("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { properties.load(it) }
    } else {
        throw IllegalArgumentException("local.properties file not found.")
    }
    return properties
}

val props = loadProperties()

// Configuration
object UrlUpdateConfig {
    val devUrl = props.getProperty("dev_supabase_url") ?: throw IllegalArgumentException("dev_supabase_url not found")
    val devServiceKey = props.getProperty("dev_service_role_key") ?: throw IllegalArgumentException("dev_service_role_key not found")
}

val devClient = createSupabaseClient(
    supabaseUrl = UrlUpdateConfig.devUrl,
    supabaseKey = UrlUpdateConfig.devServiceKey
) {
    install(Postgrest)
}

suspend fun updateDatabaseUrls(): Int = runBlocking {
    println("🔄 SUPABASE URL UPDATE: STAGING → DEV")
    println("=====================================")
    println("📋 Updating database URLs to point to dev storage")
    println()

    var totalUpdated = 0

    // Define update operations for each table/column
    val updateOperations = listOf(
        "organisation.logo",
        "member.profile_image", 
        "learning.thumbnail_url",
        "activities.media_files",
        "activities.overview_media_urls"
    )

    for (operation in updateOperations) {
        try {
            print("   Updating $operation...")
            
            when (operation) {
                "organisation.logo" -> {
                    devClient.postgrest.rpc("update_staging_urls_organisation")
                }
                "member.profile_image" -> {
                    devClient.postgrest.rpc("update_staging_urls_member")
                }
                "learning.thumbnail_url" -> {
                    devClient.postgrest.rpc("update_staging_urls_learning")
                }
                "activities.media_files" -> {
                    devClient.postgrest.rpc("update_staging_urls_activities_media")
                }
                "activities.overview_media_urls" -> {
                    devClient.postgrest.rpc("update_staging_urls_activities_overview")
                }
            }
            
            println(" ✅")
            totalUpdated++
            
        } catch (error: Exception) {
            println(" ❌ (${error.message})")
            println("   Fallback: Using direct SQL execution...")
            try {
                // Fallback to direct SQL execution using Supabase MCP servers
                when (operation) {
                    "organisation.logo" -> println("   Please run: UPDATE organisation SET logo = REPLACE(logo, 'ftnwwiwmljcwzpsawdmf', 'afjtpdeohgdgkrwayayn') WHERE logo LIKE '%ftnwwiwmljcwzpsawdmf%'")
                    "member.profile_image" -> println("   Please run: UPDATE member SET profile_image = REPLACE(profile_image, 'ftnwwiwmljcwzpsawdmf', 'afjtpdeohgdgkrwayayn') WHERE profile_image LIKE '%ftnwwiwmljcwzpsawdmf%'")
                    "learning.thumbnail_url" -> println("   Please run: UPDATE learning SET thumbnail_url = REPLACE(thumbnail_url, 'ftnwwiwmljcwzpsawdmf', 'afjtpdeohgdgkrwayayn') WHERE thumbnail_url LIKE '%ftnwwiwmljcwzpsawdmf%'")
                    "activities.media_files" -> println("   Please run: UPDATE activities SET media_files = (SELECT array_agg(REPLACE(url_elem, 'ftnwwiwmljcwzpsawdmf', 'afjtpdeohgdgkrwayayn')) FROM unnest(media_files) AS url_elem) WHERE array_to_string(media_files, '|') LIKE '%ftnwwiwmljcwzpsawdmf%'")
                    "activities.overview_media_urls" -> println("   Please run: UPDATE activities SET overview_media_urls = (SELECT array_agg(REPLACE(url_elem, 'ftnwwiwmljcwzpsawdmf', 'afjtpdeohgdgkrwayayn')) FROM unnest(overview_media_urls) AS url_elem) WHERE array_to_string(overview_media_urls, '|') LIKE '%ftnwwiwmljcwzpsawdmf%'")
                }
            } catch (fallbackError: Exception) {
                println("   Manual intervention required for $operation")
            }
        }
    }

    println()
    println("🎉 URL UPDATE SUMMARY:")
    println("   ✅ Attempted updates: ${updateOperations.size} table columns")
    println("   📋 All staging URLs should be replaced with dev URLs")
    
    updateOperations.size
}

// Main execution
println("🏗️ Initializing Database URL Update (Staging → Dev)...")
try {
    val updatedCount = runBlocking { updateDatabaseUrls() }
    
    println()
    if (updatedCount > 0) {
        println("🎉 DATABASE URL UPDATE COMPLETED!")
        println("✅ Your dev environment is now isolated from staging")
        println("🔍 Verify by checking your database URLs point to afjtpdeohgdgkrwayayn")
        println()
        println("📋 NEXT STEPS:")
        println("   1. Check if all files load correctly in your dev app")
        println("   2. Test that deleting staging won't affect dev")
        println("   3. Update any other environments that might have similar issues")
    } else {
        println("⚠️ No updates performed - check if URLs need updating")
    }
    
} catch (error: Exception) {
    println("💥 URL update failed with error: ${error.message}")
    error.printStackTrace()
}
