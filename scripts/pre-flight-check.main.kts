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
import kotlin.system.exitProcess

// Pre-flight validation results
data class ValidationResult(
    val category: String,
    val check: String,
    val status: Status,
    val message: String,
    val recommendation: String = ""
) {
    enum class Status { PASS, WARNING, FAIL }
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

suspend fun runPreFlightChecks(): List<ValidationResult> = runBlocking {
    val results = mutableListOf<ValidationResult>()
    
    println("🚀 SUPABASE MIGRATION PRE-FLIGHT VALIDATION")
    println("============================================")
    println("📋 Validating all prerequisites before migration...")
    println()

    // Category 1: Configuration Validation
    println("📋 1. CONFIGURATION VALIDATION")
    println("==============================")
    results.addAll(validateConfiguration())
    
    // Category 2: Environment Connectivity  
    println("\n📡 2. ENVIRONMENT CONNECTIVITY")
    println("==============================")
    results.addAll(validateConnectivity())
    
    // Category 3: Tool Dependencies
    println("\n🔧 3. TOOL DEPENDENCIES")
    println("=======================")
    results.addAll(validateToolDependencies())
    
    // Category 4: Source Environment Health
    println("\n🏥 4. SOURCE ENVIRONMENT HEALTH")
    println("===============================")
    results.addAll(validateSourceEnvironment())
    
    // Category 5: Target Environment Readiness
    println("\n🎯 5. TARGET ENVIRONMENT READINESS")
    println("===================================")
    results.addAll(validateTargetEnvironment())
    
    // Category 6: Migration Estimation
    println("\n📊 6. MIGRATION ESTIMATION")
    println("==========================")
    results.addAll(generateMigrationEstimation())
    
    return@runBlocking results
}

fun validateConfiguration(): List<ValidationResult> {
    val results = mutableListOf<ValidationResult>()
    
    // Check if .env.migration exists
    val envFile = File(".env.migration")
    if (!envFile.exists()) {
        results.add(ValidationResult(
            "Configuration", 
            ".env.migration file", 
            ValidationResult.Status.FAIL,
            "❌ .env.migration file not found",
            "Create .env.migration with SOURCE_* and TARGET_* variables"
        ))
        return results
    }
    
    results.add(ValidationResult(
        "Configuration",
        ".env.migration file", 
        ValidationResult.Status.PASS,
        "✅ .env.migration file found"
    ))
    
    // Load and validate configuration
    val config = loadMigrationConfig()!!
    
    val requiredVars = listOf(
        "SOURCE_PROJECT_ID", "SOURCE_URL", "SOURCE_SERVICE_KEY",
        "TARGET_PROJECT_ID", "TARGET_URL", "TARGET_SERVICE_KEY"
    )
    
    for (variable in requiredVars) {
        val value = config.getProperty(variable)
        if (value.isNullOrBlank()) {
            results.add(ValidationResult(
                "Configuration",
                variable,
                ValidationResult.Status.FAIL,
                "❌ $variable is missing or empty",
                "Add $variable to .env.migration file"
            ))
        } else {
            results.add(ValidationResult(
                "Configuration",
                variable,
                ValidationResult.Status.PASS,
                "✅ $variable configured"
            ))
        }
    }
    
    return results
}

suspend fun validateConnectivity(): List<ValidationResult> {
    val results = mutableListOf<ValidationResult>()
    val config = loadMigrationConfig() ?: return results
    
    val sourceUrl = config.getProperty("SOURCE_URL")
    val sourceServiceKey = config.getProperty("SOURCE_SERVICE_KEY") 
    val targetUrl = config.getProperty("TARGET_URL")
    val targetServiceKey = config.getProperty("TARGET_SERVICE_KEY")
    
    // Test source environment connectivity
    print("   Testing source environment connectivity...")
    try {
        val sourceClient = createSupabaseClient(sourceUrl, sourceServiceKey) {
            install(Postgrest)
            install(Storage)
        }
        
        // Test database connection
        sourceClient.from("information_schema.tables").select("table_name").limit(1).decodeList<JsonObject>()
        
        results.add(ValidationResult(
            "Connectivity",
            "Source database",
            ValidationResult.Status.PASS,
            "✅ Source database accessible"
        ))
        println(" ✅")
        
        // Test storage connection
        try {
            sourceClient.storage.listBuckets()
            results.add(ValidationResult(
                "Connectivity",
                "Source storage",
                ValidationResult.Status.PASS,
                "✅ Source storage accessible"
            ))
        } catch (e: Exception) {
            results.add(ValidationResult(
                "Connectivity", 
                "Source storage",
                ValidationResult.Status.WARNING,
                "⚠️ Source storage may not be accessible: ${e.message}",
                "Check service role key permissions for storage"
            ))
        }
        
    } catch (e: Exception) {
        results.add(ValidationResult(
            "Connectivity",
            "Source environment", 
            ValidationResult.Status.FAIL,
            "❌ Cannot connect to source: ${e.message}",
            "Check SOURCE_URL, SOURCE_SERVICE_KEY, and network restrictions"
        ))
        println(" ❌")
    }
    
    // Test target environment connectivity
    print("   Testing target environment connectivity...")
    try {
        val targetClient = createSupabaseClient(targetUrl, targetServiceKey) {
            install(Postgrest)
            install(Storage)
        }
        
        // Test database connection
        targetClient.from("information_schema.tables").select("table_name").limit(1).decodeList<JsonObject>()
        
        results.add(ValidationResult(
            "Connectivity",
            "Target database",
            ValidationResult.Status.PASS,
            "✅ Target database accessible"
        ))
        println(" ✅")
        
        // Test storage connection
        try {
            targetClient.storage.listBuckets()
            results.add(ValidationResult(
                "Connectivity",
                "Target storage", 
                ValidationResult.Status.PASS,
                "✅ Target storage accessible"
            ))
        } catch (e: Exception) {
            results.add(ValidationResult(
                "Connectivity",
                "Target storage",
                ValidationResult.Status.WARNING,
                "⚠️ Target storage may not be accessible: ${e.message}",
                "Check service role key permissions for storage"
            ))
        }
        
    } catch (e: Exception) {
        results.add(ValidationResult(
            "Connectivity",
            "Target environment",
            ValidationResult.Status.FAIL,
            "❌ Cannot connect to target: ${e.message}",
            "Check TARGET_URL, TARGET_SERVICE_KEY, and network restrictions"
        ))
        println(" ❌")
    }
    
    return results
}

fun validateToolDependencies(): List<ValidationResult> {
    val results = mutableListOf<ValidationResult>()
    
    val requiredTools = mapOf(
        "supabase" to "Supabase CLI",
        "psql" to "PostgreSQL client", 
        "pg_dump" to "PostgreSQL dump tool",
        "kotlin" to "Kotlin runtime"
    )
    
    for ((command, description) in requiredTools) {
        print("   Checking $description...")
        try {
            val process = ProcessBuilder(command, "--version")
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                results.add(ValidationResult(
                    "Tools",
                    description,
                    ValidationResult.Status.PASS,
                    "✅ $description available"
                ))
                println(" ✅")
            } else {
                results.add(ValidationResult(
                    "Tools",
                    description,
                    ValidationResult.Status.FAIL,
                    "❌ $description not working properly",
                    "Install or fix $description"
                ))
                println(" ❌")
            }
        } catch (e: Exception) {
            results.add(ValidationResult(
                "Tools",
                description,
                ValidationResult.Status.FAIL,
                "❌ $description not found",
                "Install $description: ${getInstallationCommand(command)}"
            ))
            println(" ❌")
        }
    }
    
    return results
}

suspend fun validateSourceEnvironment(): List<ValidationResult> {
    val results = mutableListOf<ValidationResult>()
    val config = loadMigrationConfig() ?: return results
    
    val sourceUrl = config.getProperty("SOURCE_URL")
    val sourceServiceKey = config.getProperty("SOURCE_SERVICE_KEY")
    
    if (sourceUrl.isNullOrBlank() || sourceServiceKey.isNullOrBlank()) {
        return results
    }
    
    try {
        val sourceClient = createSupabaseClient(sourceUrl, sourceServiceKey) {
            install(Postgrest)
            install(Storage)
        }
        
        // Check database health
        print("   Checking source database health...")
        val tables = sourceClient.from("information_schema.tables")
            .select("table_name")
            .eq("table_schema", "public")
            .decodeList<JsonObject>()
        
        results.add(ValidationResult(
            "Source Health",
            "Database tables",
            ValidationResult.Status.PASS,
            "✅ Found ${tables.size} tables in source database"
        ))
        println(" ✅")
        
        // Check for critical business tables
        print("   Checking critical business tables...")
        val criticalTables = listOf("organisation", "member", "activities")
        val existingTables = tables.map { it["table_name"]?.jsonPrimitive?.content }.toSet()
        val missingCritical = criticalTables.filter { it !in existingTables }
        
        if (missingCritical.isEmpty()) {
            results.add(ValidationResult(
                "Source Health", 
                "Critical tables",
                ValidationResult.Status.PASS,
                "✅ All critical business tables present"
            ))
            println(" ✅")
        } else {
            results.add(ValidationResult(
                "Source Health",
                "Critical tables",
                ValidationResult.Status.WARNING,
                "⚠️ Missing critical tables: ${missingCritical.joinToString()}",
                "Verify source environment has expected data"
            ))
            println(" ⚠️")
        }
        
        // Check RLS policies
        print("   Checking RLS policies...")
        try {
            val policies = sourceClient.from("pg_policies")
                .select("policyname")
                .eq("schemaname", "public")
                .decodeList<JsonObject>()
            
            results.add(ValidationResult(
                "Source Health",
                "RLS policies",
                ValidationResult.Status.PASS,
                "✅ Found ${policies.size} RLS policies"
            ))
            println(" ✅")
        } catch (e: Exception) {
            results.add(ValidationResult(
                "Source Health",
                "RLS policies",
                ValidationResult.Status.WARNING,
                "⚠️ Cannot access RLS policies: ${e.message}",
                "Check if service role key has sufficient permissions"
            ))
            println(" ⚠️")
        }
        
    } catch (e: Exception) {
        results.add(ValidationResult(
            "Source Health",
            "Environment access",
            ValidationResult.Status.FAIL,
            "❌ Cannot validate source environment: ${e.message}",
            "Fix source environment connectivity first"
        ))
    }
    
    return results
}

suspend fun validateTargetEnvironment(): List<ValidationResult> {
    val results = mutableListOf<ValidationResult>()
    val config = loadMigrationConfig() ?: return results
    
    val targetUrl = config.getProperty("TARGET_URL")
    val targetServiceKey = config.getProperty("TARGET_SERVICE_KEY")
    
    if (targetUrl.isNullOrBlank() || targetServiceKey.isNullOrBlank()) {
        return results
    }
    
    try {
        val targetClient = createSupabaseClient(targetUrl, targetServiceKey) {
            install(Postgrest)
            install(Storage)
        }
        
        // Check if target is clean or has existing data
        print("   Checking target environment status...")
        val tables = targetClient.from("information_schema.tables")
            .select("table_name")
            .eq("table_schema", "public")
            .decodeList<JsonObject>()
        
        if (tables.isEmpty()) {
            results.add(ValidationResult(
                "Target Readiness",
                "Environment state",
                ValidationResult.Status.PASS,
                "✅ Target environment is clean (ready for migration)"
            ))
            println(" ✅")
        } else {
            results.add(ValidationResult(
                "Target Readiness", 
                "Environment state",
                ValidationResult.Status.WARNING,
                "⚠️ Target has ${tables.size} existing tables",
                "Consider if you want to reset target before migration"
            ))
            println(" ⚠️")
        }
        
        // Check storage buckets
        print("   Checking storage buckets...")
        try {
            val buckets = targetClient.storage.listBuckets()
            results.add(ValidationResult(
                "Target Readiness",
                "Storage buckets",
                if (buckets.isEmpty()) ValidationResult.Status.PASS else ValidationResult.Status.WARNING,
                if (buckets.isEmpty()) "✅ No storage buckets (clean state)" 
                else "⚠️ Found ${buckets.size} existing storage buckets"
            ))
            println(if (buckets.isEmpty()) " ✅" else " ⚠️")
        } catch (e: Exception) {
            results.add(ValidationResult(
                "Target Readiness",
                "Storage access",
                ValidationResult.Status.WARNING,
                "⚠️ Cannot access target storage: ${e.message}",
                "Verify storage permissions"
            ))
            println(" ⚠️")
        }
        
    } catch (e: Exception) {
        results.add(ValidationResult(
            "Target Readiness",
            "Environment access",
            ValidationResult.Status.FAIL,
            "❌ Cannot validate target environment: ${e.message}",
            "Fix target environment connectivity first"
        ))
    }
    
    return results
}

suspend fun generateMigrationEstimation(): List<ValidationResult> {
    val results = mutableListOf<ValidationResult>()
    val config = loadMigrationConfig() ?: return results
    
    val sourceUrl = config.getProperty("SOURCE_URL")
    val sourceServiceKey = config.getProperty("SOURCE_SERVICE_KEY")
    
    if (sourceUrl.isNullOrBlank() || sourceServiceKey.isNullOrBlank()) {
        return results
    }
    
    try {
        val sourceClient = createSupabaseClient(sourceUrl, sourceServiceKey) {
            install(Postgrest)
            install(Storage)
        }
        
        // Estimate database size
        print("   Estimating database size...")
        val tableStats = sourceClient.from("pg_stat_user_tables")
            .select("relname,n_live_tup")
            .eq("schemaname", "public")
            .decodeList<JsonObject>()
        
        val totalRows = tableStats.sumOf { 
            it["n_live_tup"]?.jsonPrimitive?.longOrNull ?: 0L 
        }
        
        results.add(ValidationResult(
            "Migration Estimation",
            "Database size",
            ValidationResult.Status.PASS,
            "📊 Estimated ${tableStats.size} tables, $totalRows total rows"
        ))
        println(" ✅")
        
        // Estimate migration time
        val estimatedMinutes = when {
            totalRows < 1000 -> "5-10 minutes"
            totalRows < 10000 -> "10-20 minutes"  
            totalRows < 100000 -> "20-45 minutes"
            else -> "45+ minutes"
        }
        
        results.add(ValidationResult(
            "Migration Estimation", 
            "Time estimate",
            ValidationResult.Status.PASS,
            "⏰ Estimated migration time: $estimatedMinutes"
        ))
        
        // Check for file references
        print("   Counting file references...")
        try {
            var fileCount = 0
            val fileQueries = listOf(
                Triple("organisation", "logo", "single"),
                Triple("member", "profile_image", "single"),
                Triple("activities", "media_files", "array"),
                Triple("learning", "thumbnail_url", "single")
            )
            
            for ((table, column, type) in fileQueries) {
                try {
                    val data = sourceClient.from(table)
                        .select(column)
                        .decodeList<JsonObject>()
                    
                    for (record in data) {
                        val value = record[column]
                        when (type) {
                            "single" -> if (value is JsonPrimitive && value.isString && value.content.isNotBlank()) fileCount++
                            "array" -> if (value is JsonArray) fileCount += value.size
                        }
                    }
                } catch (e: Exception) {
                    // Table might not exist, skip
                }
            }
            
            results.add(ValidationResult(
                "Migration Estimation",
                "File references",
                ValidationResult.Status.PASS,
                "📁 Found approximately $fileCount file references"
            ))
            println(" ✅")
            
        } catch (e: Exception) {
            results.add(ValidationResult(
                "Migration Estimation",
                "File references",
                ValidationResult.Status.WARNING,
                "⚠️ Cannot estimate file count: ${e.message}"
            ))
            println(" ⚠️")
        }
        
    } catch (e: Exception) {
        results.add(ValidationResult(
            "Migration Estimation",
            "Size estimation",
            ValidationResult.Status.WARNING,
            "⚠️ Cannot generate estimates: ${e.message}"
        ))
    }
    
    return results
}

fun getInstallationCommand(tool: String): String = when (tool) {
    "supabase" -> "npm install -g supabase"
    "psql", "pg_dump" -> "Install PostgreSQL client tools"
    "kotlin" -> "Install Kotlin runtime"
    else -> "Check documentation for installation"
}

fun printValidationSummary(results: List<ValidationResult>) {
    println("\n" + "=".repeat(50))
    println("🎯 PRE-FLIGHT VALIDATION SUMMARY")
    println("=".repeat(50))
    
    val passed = results.count { it.status == ValidationResult.Status.PASS }
    val warnings = results.count { it.status == ValidationResult.Status.WARNING }
    val failed = results.count { it.status == ValidationResult.Status.FAIL }
    
    println("📊 Results: $passed passed, $warnings warnings, $failed failed")
    println()
    
    // Show all failures first
    val failures = results.filter { it.status == ValidationResult.Status.FAIL }
    if (failures.isNotEmpty()) {
        println("❌ CRITICAL ISSUES (Must fix before migration):")
        failures.forEach { result ->
            println("   • ${result.message}")
            if (result.recommendation.isNotEmpty()) {
                println("     💡 ${result.recommendation}")
            }
        }
        println()
    }
    
    // Show warnings
    val warnings_list = results.filter { it.status == ValidationResult.Status.WARNING }
    if (warnings_list.isNotEmpty()) {
        println("⚠️ WARNINGS (Review before migration):")
        warnings_list.forEach { result ->
            println("   • ${result.message}")
            if (result.recommendation.isNotEmpty()) {
                println("     💡 ${result.recommendation}")
            }
        }
        println()
    }
    
    // Migration readiness assessment
    when {
        failed > 0 -> {
            println("🚨 MIGRATION NOT READY")
            println("   Fix all critical issues before proceeding with migration")
            println("   Run this script again after fixes to revalidate")
        }
        warnings > 0 -> {
            println("⚠️ MIGRATION READY WITH CAUTION")
            println("   Review all warnings and decide if you want to proceed")
            println("   Consider fixing warnings for optimal migration experience")
        }
        else -> {
            println("✅ MIGRATION READY!")
            println("   All validation checks passed")
            println("   You can proceed with migration confidently")
        }
    }
    
    println("\n📋 Next Steps:")
    if (failed > 0) {
        println("   1. Fix all critical issues listed above")
        println("   2. Run: kotlin scripts/pre-flight-check.main.kts")
        println("   3. Proceed with migration when all checks pass")
    } else {
        println("   1. Review any warnings if present")
        println("   2. Start migration: bash scripts/complete-migration.sh")
        println("   3. Monitor migration progress carefully")
    }
    
    println("\n" + "=".repeat(50))
}

// Main execution
runBlocking {
    try {
        val results = runPreFlightChecks()
        printValidationSummary(results)
        
        val criticalFailures = results.count { it.status == ValidationResult.Status.FAIL }
        if (criticalFailures > 0) {
            exitProcess(1) // Exit with error code for CI/CD integration
        }
        
    } catch (error: Exception) {
        println("💥 Pre-flight validation failed with error: ${error.message}")
        error.printStackTrace()
        exitProcess(1)
    }
}
