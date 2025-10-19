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

// Environment comparison results
data class ComparisonResult(
    val category: String,
    val objectName: String,
    val status: Status,
    val sourceCount: Int = 0,
    val targetCount: Int = 0,
    val details: String = "",
    val recommendation: String = ""
) {
    enum class Status { IDENTICAL, MISSING_IN_TARGET, EXTRA_IN_TARGET, COUNT_MISMATCH, ERROR }
}

data class EnvironmentSnapshot(
    val tables: Set<String>,
    val views: Set<String>, 
    val functions: Set<String>,
    val triggers: Set<String>,
    val indexes: Set<String>,
    val rlsPolicies: Set<String>,
    val storageBuckets: Set<String>,
    val dataRowCounts: Map<String, Long>,
    val extensions: Set<String>
)

// Load configuration from .env.migration file
fun loadMigrationConfig(): Properties? {
    val envFile = File(".env.migration")
    return if (envFile.exists()) {
        val props = Properties()
        envFile.inputStream().use { props.load(it) }
        props
    } else null
}

suspend fun runEnvironmentComparison(): List<ComparisonResult> = runBlocking {
    val results = mutableListOf<ComparisonResult>()
    
    println("🔍 ENVIRONMENT COMPARISON ANALYSIS")
    println("==================================")
    println("📋 Comparing source and target environments comprehensively...")
    println("⏰ Started at: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
    println()

    val config = loadMigrationConfig()
    if (config == null) {
        println("❌ .env.migration file not found!")
        return@runBlocking emptyList()
    }

    val sourceUrl = config.getProperty("SOURCE_URL")
    val sourceServiceKey = config.getProperty("SOURCE_SERVICE_KEY")
    val targetUrl = config.getProperty("TARGET_URL")
    val targetServiceKey = config.getProperty("TARGET_SERVICE_KEY")

    if (sourceUrl.isNullOrBlank() || targetUrl.isNullOrBlank()) {
        println("❌ Missing SOURCE_URL or TARGET_URL in configuration!")
        return@runBlocking emptyList()
    }

    try {
        // Create clients
        val sourceClient = createSupabaseClient(sourceUrl, sourceServiceKey) {
            install(Postgrest)
            install(Storage)
        }
        
        val targetClient = createSupabaseClient(targetUrl, targetServiceKey) {
            install(Postgrest)
            install(Storage)
        }

        println("📊 1. CAPTURING ENVIRONMENT SNAPSHOTS")
        println("=====================================")
        
        print("   Capturing source environment snapshot...")
        val sourceSnapshot = captureEnvironmentSnapshot(sourceClient, "Source")
        println(" ✅")
        
        print("   Capturing target environment snapshot...")
        val targetSnapshot = captureEnvironmentSnapshot(targetClient, "Target")
        println(" ✅")

        println("\n🔍 2. ANALYZING DIFFERENCES")
        println("===========================")
        
        // Compare database objects
        results.addAll(compareDatabaseObjects(sourceSnapshot, targetSnapshot))
        
        // Compare data counts
        results.addAll(compareDataCounts(sourceSnapshot, targetSnapshot))
        
        // Compare storage
        results.addAll(compareStorageBuckets(sourceSnapshot, targetSnapshot))
        
        // Compare extensions
        results.addAll(compareExtensions(sourceSnapshot, targetSnapshot))

    } catch (error: Exception) {
        results.add(ComparisonResult(
            "Connection",
            "Environment access",
            ComparisonResult.Status.ERROR,
            details = "Failed to connect: ${error.message}",
            recommendation = "Check environment connectivity and credentials"
        ))
    }

    return@runBlocking results
}

suspend fun captureEnvironmentSnapshot(client: io.github.jan.supabase.SupabaseClient, envName: String): EnvironmentSnapshot {
    val tables = mutableSetOf<String>()
    val views = mutableSetOf<String>()
    val functions = mutableSetOf<String>()
    val triggers = mutableSetOf<String>()
    val indexes = mutableSetOf<String>()
    val rlsPolicies = mutableSetOf<String>()
    val storageBuckets = mutableSetOf<String>()
    val dataRowCounts = mutableMapOf<String, Long>()
    val extensions = mutableSetOf<String>()

    try {
        // Capture tables
        val tablesResult = client.from("information_schema.tables")
            .select("table_name")
            .eq("table_schema", "public")
            .decodeList<JsonObject>()
        
        tables.addAll(tablesResult.mapNotNull { it["table_name"]?.jsonPrimitive?.content })

        // Capture views
        val viewsResult = client.from("information_schema.views")
            .select("table_name")
            .eq("table_schema", "public")
            .decodeList<JsonObject>()
        
        views.addAll(viewsResult.mapNotNull { it["table_name"]?.jsonPrimitive?.content })

        // Capture functions
        try {
            val functionsResult = client.from("information_schema.routines")
                .select("routine_name")
                .eq("routine_schema", "public")
                .eq("routine_type", "FUNCTION")
                .decodeList<JsonObject>()
            
            functions.addAll(functionsResult.mapNotNull { it["routine_name"]?.jsonPrimitive?.content })
        } catch (e: Exception) {
            // Functions might not be accessible, continue
        }

        // Capture triggers
        try {
            val triggersResult = client.from("information_schema.triggers")
                .select("trigger_name")
                .eq("trigger_schema", "public")
                .decodeList<JsonObject>()
            
            triggers.addAll(triggersResult.mapNotNull { it["trigger_name"]?.jsonPrimitive?.content })
        } catch (e: Exception) {
            // Triggers might not be accessible, continue
        }

        // Capture indexes
        try {
            val indexesResult = client.from("pg_indexes")
                .select("indexname")
                .eq("schemaname", "public")
                .decodeList<JsonObject>()
            
            indexes.addAll(indexesResult.mapNotNull { it["indexname"]?.jsonPrimitive?.content })
        } catch (e: Exception) {
            // Indexes might not be accessible, continue
        }

        // Capture RLS policies
        try {
            val policiesResult = client.from("pg_policies")
                .select("policyname")
                .eq("schemaname", "public")
                .decodeList<JsonObject>()
            
            rlsPolicies.addAll(policiesResult.mapNotNull { it["policyname"]?.jsonPrimitive?.content })
        } catch (e: Exception) {
            // Policies might not be accessible, continue
        }

        // Capture data row counts for tables
        if (tables.isNotEmpty()) {
            try {
                val statsResult = client.from("pg_stat_user_tables")
                    .select("relname,n_live_tup")
                    .eq("schemaname", "public")
                    .decodeList<JsonObject>()
                
                for (stat in statsResult) {
                    val tableName = stat["relname"]?.jsonPrimitive?.content
                    val rowCount = stat["n_live_tup"]?.jsonPrimitive?.longOrNull ?: 0L
                    if (tableName != null) {
                        dataRowCounts[tableName] = rowCount
                    }
                }
            } catch (e: Exception) {
                // Statistics might not be accessible, continue
            }
        }

        // Capture storage buckets
        try {
            val bucketsResult = client.storage.listBuckets()
            storageBuckets.addAll(bucketsResult.map { it.name })
        } catch (e: Exception) {
            // Storage might not be accessible, continue
        }

        // Capture extensions
        try {
            val extensionsResult = client.from("pg_extension")
                .select("extname")
                .decodeList<JsonObject>()
            
            extensions.addAll(extensionsResult.mapNotNull { it["extname"]?.jsonPrimitive?.content })
        } catch (e: Exception) {
            // Extensions might not be accessible, continue
        }

    } catch (error: Exception) {
        println("   ⚠️ Error capturing $envName snapshot: ${error.message}")
    }

    return EnvironmentSnapshot(
        tables = tables,
        views = views,
        functions = functions,
        triggers = triggers,
        indexes = indexes,
        rlsPolicies = rlsPolicies,
        storageBuckets = storageBuckets,
        dataRowCounts = dataRowCounts,
        extensions = extensions
    )
}

fun compareDatabaseObjects(source: EnvironmentSnapshot, target: EnvironmentSnapshot): List<ComparisonResult> {
    val results = mutableListOf<ComparisonResult>()

    // Compare tables
    val allTables = source.tables + target.tables
    for (table in allTables) {
        when {
            table in source.tables && table in target.tables -> {
                results.add(ComparisonResult(
                    "Tables",
                    table,
                    ComparisonResult.Status.IDENTICAL,
                    1, 1,
                    "Table exists in both environments"
                ))
            }
            table in source.tables && table !in target.tables -> {
                results.add(ComparisonResult(
                    "Tables",
                    table,
                    ComparisonResult.Status.MISSING_IN_TARGET,
                    1, 0,
                    "Table missing in target",
                    "Ensure table is migrated to target environment"
                ))
            }
            table !in source.tables && table in target.tables -> {
                results.add(ComparisonResult(
                    "Tables",
                    table,
                    ComparisonResult.Status.EXTRA_IN_TARGET,
                    0, 1,
                    "Extra table in target",
                    "Consider if this table should exist in target"
                ))
            }
        }
    }

    // Compare views
    val allViews = source.views + target.views
    for (view in allViews) {
        when {
            view in source.views && view in target.views -> {
                results.add(ComparisonResult(
                    "Views",
                    view,
                    ComparisonResult.Status.IDENTICAL,
                    1, 1,
                    "View exists in both environments"
                ))
            }
            view in source.views && view !in target.views -> {
                results.add(ComparisonResult(
                    "Views",
                    view,
                    ComparisonResult.Status.MISSING_IN_TARGET,
                    1, 0,
                    "View missing in target",
                    "Ensure view is migrated to target environment"
                ))
            }
            view !in source.views && view in target.views -> {
                results.add(ComparisonResult(
                    "Views",
                    view,
                    ComparisonResult.Status.EXTRA_IN_TARGET,
                    0, 1,
                    "Extra view in target"
                ))
            }
        }
    }

    // Compare functions
    val allFunctions = source.functions + target.functions
    for (function in allFunctions) {
        when {
            function in source.functions && function in target.functions -> {
                results.add(ComparisonResult(
                    "Functions",
                    function,
                    ComparisonResult.Status.IDENTICAL,
                    1, 1,
                    "Function exists in both environments"
                ))
            }
            function in source.functions && function !in target.functions -> {
                results.add(ComparisonResult(
                    "Functions",
                    function,
                    ComparisonResult.Status.MISSING_IN_TARGET,
                    1, 0,
                    "Function missing in target",
                    "Ensure function is migrated to target environment"
                ))
            }
            function !in source.functions && function in target.functions -> {
                results.add(ComparisonResult(
                    "Functions",
                    function,
                    ComparisonResult.Status.EXTRA_IN_TARGET,
                    0, 1,
                    "Extra function in target"
                ))
            }
        }
    }

    // Compare RLS policies
    val allPolicies = source.rlsPolicies + target.rlsPolicies
    for (policy in allPolicies) {
        when {
            policy in source.rlsPolicies && policy in target.rlsPolicies -> {
                results.add(ComparisonResult(
                    "RLS Policies",
                    policy,
                    ComparisonResult.Status.IDENTICAL,
                    1, 1,
                    "RLS policy exists in both environments"
                ))
            }
            policy in source.rlsPolicies && policy !in target.rlsPolicies -> {
                results.add(ComparisonResult(
                    "RLS Policies",
                    policy,
                    ComparisonResult.Status.MISSING_IN_TARGET,
                    1, 0,
                    "RLS policy missing in target",
                    "CRITICAL: Ensure RLS policy is migrated for security"
                ))
            }
            policy !in source.rlsPolicies && policy in target.rlsPolicies -> {
                results.add(ComparisonResult(
                    "RLS Policies",
                    policy,
                    ComparisonResult.Status.EXTRA_IN_TARGET,
                    0, 1,
                    "Extra RLS policy in target"
                ))
            }
        }
    }

    // Compare indexes
    val allIndexes = source.indexes + target.indexes
    for (index in allIndexes) {
        when {
            index in source.indexes && index in target.indexes -> {
                results.add(ComparisonResult(
                    "Indexes",
                    index,
                    ComparisonResult.Status.IDENTICAL,
                    1, 1,
                    "Index exists in both environments"
                ))
            }
            index in source.indexes && index !in target.indexes -> {
                results.add(ComparisonResult(
                    "Indexes",
                    index,
                    ComparisonResult.Status.MISSING_IN_TARGET,
                    1, 0,
                    "Index missing in target",
                    "Performance impact: Ensure index is created in target"
                ))
            }
            index !in source.indexes && index in target.indexes -> {
                results.add(ComparisonResult(
                    "Indexes",
                    index,
                    ComparisonResult.Status.EXTRA_IN_TARGET,
                    0, 1,
                    "Extra index in target"
                ))
            }
        }
    }

    return results
}

fun compareDataCounts(source: EnvironmentSnapshot, target: EnvironmentSnapshot): List<ComparisonResult> {
    val results = mutableListOf<ComparisonResult>()

    val allTables = (source.dataRowCounts.keys + target.dataRowCounts.keys).toSet()
    
    for (table in allTables) {
        val sourceCount = source.dataRowCounts[table] ?: 0L
        val targetCount = target.dataRowCounts[table] ?: 0L
        
        when {
            sourceCount == targetCount -> {
                results.add(ComparisonResult(
                    "Data Counts",
                    table,
                    ComparisonResult.Status.IDENTICAL,
                    sourceCount.toInt(),
                    targetCount.toInt(),
                    "Row counts match ($sourceCount rows)"
                ))
            }
            sourceCount != targetCount -> {
                val status = if (targetCount < sourceCount) {
                    ComparisonResult.Status.MISSING_IN_TARGET
                } else {
                    ComparisonResult.Status.EXTRA_IN_TARGET
                }
                
                results.add(ComparisonResult(
                    "Data Counts",
                    table,
                    status,
                    sourceCount.toInt(),
                    targetCount.toInt(),
                    "Row count mismatch: Source=$sourceCount, Target=$targetCount",
                    "Investigate data migration completeness"
                ))
            }
        }
    }

    return results
}

fun compareStorageBuckets(source: EnvironmentSnapshot, target: EnvironmentSnapshot): List<ComparisonResult> {
    val results = mutableListOf<ComparisonResult>()

    val allBuckets = source.storageBuckets + target.storageBuckets
    
    for (bucket in allBuckets) {
        when {
            bucket in source.storageBuckets && bucket in target.storageBuckets -> {
                results.add(ComparisonResult(
                    "Storage Buckets",
                    bucket,
                    ComparisonResult.Status.IDENTICAL,
                    1, 1,
                    "Storage bucket exists in both environments"
                ))
            }
            bucket in source.storageBuckets && bucket !in target.storageBuckets -> {
                results.add(ComparisonResult(
                    "Storage Buckets",
                    bucket,
                    ComparisonResult.Status.MISSING_IN_TARGET,
                    1, 0,
                    "Storage bucket missing in target",
                    "Create missing storage bucket in target environment"
                ))
            }
            bucket !in source.storageBuckets && bucket in target.storageBuckets -> {
                results.add(ComparisonResult(
                    "Storage Buckets",
                    bucket,
                    ComparisonResult.Status.EXTRA_IN_TARGET,
                    0, 1,
                    "Extra storage bucket in target"
                ))
            }
        }
    }

    return results
}

fun compareExtensions(source: EnvironmentSnapshot, target: EnvironmentSnapshot): List<ComparisonResult> {
    val results = mutableListOf<ComparisonResult>()

    val allExtensions = source.extensions + target.extensions
    
    for (extension in allExtensions) {
        when {
            extension in source.extensions && extension in target.extensions -> {
                results.add(ComparisonResult(
                    "Extensions",
                    extension,
                    ComparisonResult.Status.IDENTICAL,
                    1, 1,
                    "Extension exists in both environments"
                ))
            }
            extension in source.extensions && extension !in target.extensions -> {
                results.add(ComparisonResult(
                    "Extensions",
                    extension,
                    ComparisonResult.Status.MISSING_IN_TARGET,
                    1, 0,
                    "Extension missing in target",
                    "Install missing extension in target environment"
                ))
            }
            extension !in source.extensions && extension in target.extensions -> {
                results.add(ComparisonResult(
                    "Extensions",
                    extension,
                    ComparisonResult.Status.EXTRA_IN_TARGET,
                    0, 1,
                    "Extra extension in target"
                ))
            }
        }
    }

    return results
}

fun generateComparisonReport(results: List<ComparisonResult>) {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val reportFile = "environment-comparison-report-$timestamp.md"
    
    val identical = results.count { it.status == ComparisonResult.Status.IDENTICAL }
    val missingInTarget = results.count { it.status == ComparisonResult.Status.MISSING_IN_TARGET }
    val extraInTarget = results.count { it.status == ComparisonResult.Status.EXTRA_IN_TARGET }
    val countMismatch = results.count { it.status == ComparisonResult.Status.COUNT_MISMATCH }
    val errors = results.count { it.status == ComparisonResult.Status.ERROR }

    println("\n" + "=".repeat(60))
    println("📊 ENVIRONMENT COMPARISON SUMMARY")
    println("=".repeat(60))
    println("📈 Results: $identical identical, $missingInTarget missing, $extraInTarget extra, $countMismatch mismatched, $errors errors")
    println()

    // Show critical issues first
    val criticalIssues = results.filter { 
        it.status == ComparisonResult.Status.MISSING_IN_TARGET || 
        it.status == ComparisonResult.Status.ERROR 
    }
    
    if (criticalIssues.isNotEmpty()) {
        println("🚨 CRITICAL ISSUES (Must address before migration):")
        criticalIssues.groupBy { it.category }.forEach { (category, issues) ->
            println("   $category:")
            issues.forEach { issue ->
                println("      ❌ ${issue.objectName}: ${issue.details}")
                if (issue.recommendation.isNotEmpty()) {
                    println("         💡 ${issue.recommendation}")
                }
            }
        }
        println()
    }

    // Show warnings
    val warnings = results.filter { 
        it.status == ComparisonResult.Status.EXTRA_IN_TARGET || 
        it.status == ComparisonResult.Status.COUNT_MISMATCH 
    }
    
    if (warnings.isNotEmpty()) {
        println("⚠️ WARNINGS (Review before migration):")
        warnings.groupBy { it.category }.forEach { (category, issues) ->
            println("   $category:")
            issues.take(5).forEach { issue -> // Limit output for readability
                println("      ⚠️ ${issue.objectName}: ${issue.details}")
                if (issue.recommendation.isNotEmpty()) {
                    println("         💡 ${issue.recommendation}")
                }
            }
            if (issues.size > 5) {
                println("      ... and ${issues.size - 5} more (see full report)")
            }
        }
        println()
    }

    // Migration readiness assessment
    when {
        missingInTarget > 0 || errors > 0 -> {
            println("🚨 ENVIRONMENTS NOT READY FOR MIGRATION")
            println("   Critical differences detected that must be resolved")
            println("   Fix all critical issues before proceeding")
        }
        countMismatch > 0 || extraInTarget > 0 -> {
            println("⚠️ ENVIRONMENTS READY WITH CAUTION")
            println("   Some differences detected that should be reviewed")
            println("   Consider if these differences are expected")
        }
        else -> {
            println("✅ ENVIRONMENTS ARE SYNCHRONIZED!")
            println("   No critical differences detected")
            println("   Safe to proceed with operations")
        }
    }

    // Write detailed report to file
    try {
        File(reportFile).writeText(generateDetailedReport(results))
        println("\n📝 Detailed report written to: $reportFile")
    } catch (e: Exception) {
        println("\n⚠️ Could not write report file: ${e.message}")
    }

    println("\n📋 Summary by Category:")
    results.groupBy { it.category }.forEach { (category, categoryResults) ->
        val categoryIdentical = categoryResults.count { it.status == ComparisonResult.Status.IDENTICAL }
        val categoryMissing = categoryResults.count { it.status == ComparisonResult.Status.MISSING_IN_TARGET }
        val categoryExtra = categoryResults.count { it.status == ComparisonResult.Status.EXTRA_IN_TARGET }
        val categoryMismatch = categoryResults.count { it.status == ComparisonResult.Status.COUNT_MISMATCH }
        
        val status = when {
            categoryMissing > 0 -> "🚨"
            categoryMismatch > 0 || categoryExtra > 0 -> "⚠️"
            else -> "✅"
        }
        
        println("   $status $category: $categoryIdentical identical, $categoryMissing missing, $categoryExtra extra, $categoryMismatch mismatched")
    }

    println("\n" + "=".repeat(60))
}

fun generateDetailedReport(results: List<ComparisonResult>): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    
    return buildString {
        appendLine("# Environment Comparison Report")
        appendLine("**Generated:** $timestamp")
        appendLine()
        
        appendLine("## Summary")
        appendLine()
        val statusCounts = results.groupBy { it.status }.mapValues { it.value.size }
        statusCounts.forEach { (status, count) ->
            appendLine("- **${status.name}**: $count")
        }
        appendLine()
        
        appendLine("## Detailed Results")
        appendLine()
        
        results.groupBy { it.category }.forEach { (category, categoryResults) ->
            appendLine("### $category")
            appendLine()
            
            categoryResults.forEach { result ->
                val icon = when (result.status) {
                    ComparisonResult.Status.IDENTICAL -> "✅"
                    ComparisonResult.Status.MISSING_IN_TARGET -> "❌"
                    ComparisonResult.Status.EXTRA_IN_TARGET -> "⚠️"
                    ComparisonResult.Status.COUNT_MISMATCH -> "📊"
                    ComparisonResult.Status.ERROR -> "💥"
                }
                
                appendLine("$icon **${result.objectName}**")
                appendLine("   - Status: ${result.status.name}")
                appendLine("   - Details: ${result.details}")
                if (result.sourceCount > 0 || result.targetCount > 0) {
                    appendLine("   - Count: Source=${result.sourceCount}, Target=${result.targetCount}")
                }
                if (result.recommendation.isNotEmpty()) {
                    appendLine("   - Recommendation: ${result.recommendation}")
                }
                appendLine()
            }
        }
    }
}

// Main execution
runBlocking {
    try {
        val results = runEnvironmentComparison()
        
        if (results.isNotEmpty()) {
            generateComparisonReport(results)
        } else {
            println("❌ No comparison results generated")
            println("Check your .env.migration configuration and environment connectivity")
        }
        
    } catch (error: Exception) {
        println("💥 Environment comparison failed with error: ${error.message}")
        error.printStackTrace()
    }
}
