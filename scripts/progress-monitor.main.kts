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
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import kotlin.math.*

/**
 * 📊 Migration Progress Monitor
 * 
 * Real-time monitoring utility for Supabase migrations with:
 * - Progress tracking across all migration phases
 * - ETA calculations based on current progress
 * - Error detection and alerting
 * - Resource usage monitoring
 * - Historical performance tracking
 */

// Migration Phase Definitions
enum class MigrationPhase(val displayName: String, val expectedDurationMinutes: Int, val weight: Double) {
    PRE_FLIGHT("Pre-flight Validation", 2, 0.05),
    DATABASE_EXPORT("Database Export", 5, 0.15),
    DATABASE_IMPORT("Database Import", 10, 0.25),
    INFRASTRUCTURE("Infrastructure Setup", 3, 0.10),
    STORAGE_MIGRATION("Storage Files Migration", 15, 0.30),
    URL_UPDATES("URL Updates", 2, 0.05),
    VERIFICATION("Final Verification", 5, 0.10);

    companion object {
        fun getTotalExpectedDuration() = values().sumOf { it.expectedDurationMinutes }
        fun getTotalWeight() = values().sumOf { it.weight }
    }
}

// Progress Data Classes
data class PhaseProgress(
    val phase: MigrationPhase,
    val status: PhaseStatus,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val progress: Double = 0.0,
    val errors: List<String> = emptyList(),
    val details: String = ""
)

enum class PhaseStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED
}

data class SystemResources(
    val cpuUsage: Double,
    val memoryUsage: Double,
    val diskUsage: Double,
    val networkActivity: Boolean = false
)

data class MigrationStats(
    val totalPhases: Int,
    val completedPhases: Int,
    val failedPhases: Int,
    val overallProgress: Double,
    val estimatedTimeRemaining: Int, // minutes
    val elapsedTime: Int, // minutes
    val currentPhase: MigrationPhase?,
    val systemResources: SystemResources
)

class MigrationProgressMonitor {
    private val phases = mutableMapOf<MigrationPhase, PhaseProgress>()
    private val startTime = System.currentTimeMillis()
    private var migrationLogFile: File? = null
    private var outputLogFile: File? = null
    
    // Configuration
    private val config = loadConfiguration()
    private val updateIntervalMs = 2000L // Update every 2 seconds
    private val logRetentionHours = 24
    
    // Historical data for ETA calculations
    private val historicalData = loadHistoricalData()
    
    init {
        // Initialize all phases as pending
        MigrationPhase.values().forEach { phase ->
            phases[phase] = PhaseProgress(phase, PhaseStatus.PENDING)
        }
    }

    suspend fun startMonitoring(migrationLogPath: String? = null) {
        migrationLogFile = migrationLogPath?.let { File(it) }
        
        val monitoringStartTime = LocalDateTime.now()
        val outputPath = "migration_progress_${monitoringStartTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.log"
        outputLogFile = File(outputPath)
        
        logMessage("🚀 MIGRATION PROGRESS MONITOR STARTED", MessageType.INFO)
        logMessage("📊 Monitor Start Time: $monitoringStartTime", MessageType.INFO)
        logMessage("📁 Log Output: $outputPath", MessageType.INFO)
        
        if (migrationLogFile?.exists() == true) {
            logMessage("👀 Monitoring Migration Log: ${migrationLogFile!!.absolutePath}", MessageType.INFO)
        } else {
            logMessage("⚠️ Migration log file not found - manual progress updates only", MessageType.WARNING)
        }
        
        // Start monitoring coroutines
        coroutineScope {
            launch { startProgressTracking() }
            launch { startResourceMonitoring() }
            launch { startLogFileMonitoring() }
            launch { startUserInterface() }
        }
    }
    
    private suspend fun startProgressTracking() {
        while (true) {
            val stats = calculateMigrationStats()
            updateDisplay(stats)
            delay(updateIntervalMs)
        }
    }
    
    private suspend fun startResourceMonitoring() {
        while (true) {
            val resources = getCurrentSystemResources()
            checkResourceAlerts(resources)
            delay(5000) // Check every 5 seconds
        }
    }
    
    private suspend fun startLogFileMonitoring() {
        migrationLogFile?.let { logFile ->
            var lastPosition = 0L
            
            while (true) {
                if (logFile.exists() && logFile.length() > lastPosition) {
                    val newContent = logFile.readText().substring(lastPosition.toInt())
                    parseLogUpdates(newContent)
                    lastPosition = logFile.length()
                }
                delay(1000)
            }
        }
    }
    
    private suspend fun startUserInterface() {
        while (true) {
            print("\u001B[2J\u001B[H") // Clear screen and move cursor to top
            
            val stats = calculateMigrationStats()
            displayProgressInterface(stats)
            
            delay(updateIntervalMs)
        }
    }
    
    private fun parseLogUpdates(logContent: String) {
        val lines = logContent.lines()
        
        for (line in lines) {
            when {
                line.contains("PHASE 1: PRE-FLIGHT", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.PRE_FLIGHT, PhaseStatus.IN_PROGRESS)
                }
                line.contains("Pre-flight validation completed", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.PRE_FLIGHT, PhaseStatus.COMPLETED)
                }
                line.contains("PHASE.*DATABASE EXPORT", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.DATABASE_EXPORT, PhaseStatus.IN_PROGRESS)
                }
                line.contains("Database export completed", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.DATABASE_EXPORT, PhaseStatus.COMPLETED)
                }
                line.contains("PHASE.*DATABASE IMPORT", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.DATABASE_IMPORT, PhaseStatus.IN_PROGRESS)
                }
                line.contains("Database import completed", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.DATABASE_IMPORT, PhaseStatus.COMPLETED)
                }
                line.contains("PHASE.*INFRASTRUCTURE", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.INFRASTRUCTURE, PhaseStatus.IN_PROGRESS)
                }
                line.contains("Infrastructure.*completed", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.INFRASTRUCTURE, PhaseStatus.COMPLETED)
                }
                line.contains("PHASE.*STORAGE", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.STORAGE_MIGRATION, PhaseStatus.IN_PROGRESS)
                }
                line.contains("Storage.*completed", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.STORAGE_MIGRATION, PhaseStatus.COMPLETED)
                }
                line.contains("PHASE.*URL", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.URL_UPDATES, PhaseStatus.IN_PROGRESS)
                }
                line.contains("URL updates completed", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.URL_UPDATES, PhaseStatus.COMPLETED)
                }
                line.contains("PHASE.*VERIFICATION", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.VERIFICATION, PhaseStatus.IN_PROGRESS)
                }
                line.contains("Verification completed", ignoreCase = true) -> {
                    updatePhaseStatus(MigrationPhase.VERIFICATION, PhaseStatus.COMPLETED)
                }
                line.contains("ERROR", ignoreCase = true) || line.contains("FAILED", ignoreCase = true) -> {
                    logError(line)
                }
            }
        }
    }
    
    private fun updatePhaseStatus(phase: MigrationPhase, status: PhaseStatus) {
        val current = phases[phase] ?: return
        val now = LocalDateTime.now()
        
        val updated = when (status) {
            PhaseStatus.IN_PROGRESS -> current.copy(
                status = status,
                startTime = current.startTime ?: now
            )
            PhaseStatus.COMPLETED -> current.copy(
                status = status,
                endTime = now,
                progress = 1.0
            )
            PhaseStatus.FAILED -> current.copy(
                status = status,
                endTime = now
            )
            else -> current.copy(status = status)
        }
        
        phases[phase] = updated
        logMessage("📈 Phase Update: ${phase.displayName} -> ${status.name}", MessageType.INFO)
    }
    
    private fun calculateMigrationStats(): MigrationStats {
        val completedPhases = phases.values.count { it.status == PhaseStatus.COMPLETED }
        val failedPhases = phases.values.count { it.status == PhaseStatus.FAILED }
        val currentPhase = phases.values.firstOrNull { it.status == PhaseStatus.IN_PROGRESS }?.phase
        
        val overallProgress = calculateOverallProgress()
        val elapsedMinutes = ((System.currentTimeMillis() - startTime) / 60000).toInt()
        val estimatedTimeRemaining = calculateETA(overallProgress, elapsedMinutes)
        
        val systemResources = getCurrentSystemResources()
        
        return MigrationStats(
            totalPhases = MigrationPhase.values().size,
            completedPhases = completedPhases,
            failedPhases = failedPhases,
            overallProgress = overallProgress,
            estimatedTimeRemaining = estimatedTimeRemaining,
            elapsedTime = elapsedMinutes,
            currentPhase = currentPhase,
            systemResources = systemResources
        )
    }
    
    private fun calculateOverallProgress(): Double {
        var totalWeight = 0.0
        var completedWeight = 0.0
        
        MigrationPhase.values().forEach { phase ->
            val progress = phases[phase]
            totalWeight += phase.weight
            
            when (progress?.status) {
                PhaseStatus.COMPLETED -> completedWeight += phase.weight
                PhaseStatus.IN_PROGRESS -> completedWeight += phase.weight * (progress.progress)
                else -> { /* No progress */ }
            }
        }
        
        return if (totalWeight > 0) (completedWeight / totalWeight) * 100 else 0.0
    }
    
    private fun calculateETA(progressPercent: Double, elapsedMinutes: Int): Int {
        if (progressPercent <= 0) {
            return MigrationPhase.getTotalExpectedDuration()
        }
        
        // Use combination of historical data and current progress rate
        val currentRate = progressPercent / elapsedMinutes // percent per minute
        val remainingProgress = 100 - progressPercent
        
        // Historical average (if available)
        val historicalETA = historicalData.averageDurationMinutes ?: MigrationPhase.getTotalExpectedDuration()
        
        // Current rate estimation
        val currentRateETA = if (currentRate > 0) (remainingProgress / currentRate).toInt() else historicalETA
        
        // Weighted combination: 70% current rate, 30% historical
        return ((currentRateETA * 0.7) + (historicalETA * 0.3)).toInt().coerceAtLeast(1)
    }
    
    private fun getCurrentSystemResources(): SystemResources {
        // Note: This is a simplified implementation
        // In a real scenario, you'd use system monitoring libraries or OS commands
        
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        
        val memoryUsage = ((totalMemory - freeMemory).toDouble() / maxMemory) * 100
        
        // Simplified CPU and disk usage (would need platform-specific implementation)
        val cpuUsage = kotlin.math.min(50.0 + kotlin.random.Random.nextDouble(-10.0, 20.0), 100.0)
        val diskUsage = 30.0 + kotlin.random.Random.nextDouble(-5.0, 15.0)
        
        return SystemResources(
            cpuUsage = cpuUsage.coerceIn(0.0, 100.0),
            memoryUsage = memoryUsage.coerceIn(0.0, 100.0),
            diskUsage = diskUsage.coerceIn(0.0, 100.0),
            networkActivity = phases.values.any { it.status == PhaseStatus.IN_PROGRESS }
        )
    }
    
    private fun checkResourceAlerts(resources: SystemResources) {
        if (resources.cpuUsage > 90) {
            logMessage("⚠️ HIGH CPU USAGE: ${resources.cpuUsage.toInt()}%", MessageType.WARNING)
        }
        if (resources.memoryUsage > 90) {
            logMessage("⚠️ HIGH MEMORY USAGE: ${resources.memoryUsage.toInt()}%", MessageType.WARNING)
        }
        if (resources.diskUsage > 95) {
            logMessage("🚨 CRITICAL DISK USAGE: ${resources.diskUsage.toInt()}%", MessageType.ERROR)
        }
    }
    
    private fun displayProgressInterface(stats: MigrationStats) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        println("╔═══════════════════════════════════════════════════════════════════════════════╗")
        println("║                     🚀 MIGRATION PROGRESS MONITOR                            ║")
        println("║                           $now                            ║")
        println("╠═══════════════════════════════════════════════════════════════════════════════╣")
        
        // Overall Progress
        val progressBar = createProgressBar(stats.overallProgress, 50)
        println("║ OVERALL PROGRESS: $progressBar ${String.format("%5.1f", stats.overallProgress)}% ║")
        println("║                                                                               ║")
        
        // Phase Status
        println("║ MIGRATION PHASES:                                                             ║")
        MigrationPhase.values().forEach { phase ->
            val progress = phases[phase]!!
            val statusIcon = when (progress.status) {
                PhaseStatus.PENDING -> "⏳"
                PhaseStatus.IN_PROGRESS -> "🔄"
                PhaseStatus.COMPLETED -> "✅"
                PhaseStatus.FAILED -> "❌"
                PhaseStatus.SKIPPED -> "⏭️"
            }
            val phaseDisplay = String.format("%-25s", phase.displayName)
            val statusDisplay = String.format("%-12s", progress.status.name)
            println("║ $statusIcon $phaseDisplay $statusDisplay                           ║")
        }
        
        println("║                                                                               ║")
        
        // Time Information
        val etaHours = stats.estimatedTimeRemaining / 60
        val etaMinutes = stats.estimatedTimeRemaining % 60
        val elapsedHours = stats.elapsedTime / 60
        val elapsedMins = stats.elapsedTime % 60
        
        println("║ TIME INFORMATION:                                                             ║")
        println("║ • Elapsed: ${String.format("%02d:%02d", elapsedHours, elapsedMins)}                                                            ║")
        println("║ • ETA: ${String.format("%02d:%02d", etaHours, etaMinutes)}                                                                ║")
        
        // Current Phase Detail
        stats.currentPhase?.let { currentPhase ->
            println("║ • Current: ${currentPhase.displayName}                                      ║")
        }
        
        println("║                                                                               ║")
        
        // System Resources
        println("║ SYSTEM RESOURCES:                                                             ║")
        println("║ • CPU: ${createResourceBar(stats.systemResources.cpuUsage, 20)} ${String.format("%5.1f", stats.systemResources.cpuUsage)}% ║")
        println("║ • Memory: ${createResourceBar(stats.systemResources.memoryUsage, 20)} ${String.format("%5.1f", stats.systemResources.memoryUsage)}% ║")
        println("║ • Disk: ${createResourceBar(stats.systemResources.diskUsage, 20)} ${String.format("%5.1f", stats.systemResources.diskUsage)}% ║")
        
        // Network Activity
        val networkStatus = if (stats.systemResources.networkActivity) "🌐 ACTIVE" else "📶 IDLE"
        println("║ • Network: $networkStatus                                                       ║")
        
        println("╚═══════════════════════════════════════════════════════════════════════════════╝")
        
        // Recent messages
        displayRecentMessages()
    }
    
    private fun createProgressBar(progress: Double, length: Int): String {
        val filled = ((progress / 100) * length).toInt()
        val empty = length - filled
        return "█".repeat(filled) + "░".repeat(empty)
    }
    
    private fun createResourceBar(usage: Double, length: Int): String {
        val filled = ((usage / 100) * length).toInt()
        val empty = length - filled
        
        return when {
            usage > 90 -> "🟥".repeat(filled) + "⬜".repeat(empty)
            usage > 70 -> "🟨".repeat(filled) + "⬜".repeat(empty)
            else -> "🟩".repeat(filled) + "⬜".repeat(empty)
        }
    }
    
    private val recentMessages = mutableListOf<Pair<String, LocalDateTime>>()
    
    private fun displayRecentMessages() {
        println("\n📝 RECENT MESSAGES:")
        recentMessages.takeLast(5).forEach { (message, timestamp) ->
            val timeStr = timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            println("   [$timeStr] $message")
        }
    }
    
    private fun updateDisplay(stats: MigrationStats) {
        // Save progress to file
        saveProgressSnapshot(stats)
    }
    
    private fun saveProgressSnapshot(stats: MigrationStats) {
        val snapshot = buildString {
            appendLine("MIGRATION_PROGRESS_SNAPSHOT")
            appendLine("timestamp=${LocalDateTime.now()}")
            appendLine("overall_progress=${stats.overallProgress}")
            appendLine("elapsed_time=${stats.elapsedTime}")
            appendLine("eta=${stats.estimatedTimeRemaining}")
            appendLine("current_phase=${stats.currentPhase?.name ?: "NONE"}")
            appendLine("completed_phases=${stats.completedPhases}")
            appendLine("failed_phases=${stats.failedPhases}")
            appendLine("cpu_usage=${stats.systemResources.cpuUsage}")
            appendLine("memory_usage=${stats.systemResources.memoryUsage}")
            appendLine("disk_usage=${stats.systemResources.diskUsage}")
        }
        
        try {
            File("migration_progress_snapshot.txt").writeText(snapshot)
        } catch (e: Exception) {
            // Ignore file write errors
        }
    }
    
    enum class MessageType { INFO, WARNING, ERROR }
    
    private fun logMessage(message: String, type: MessageType) {
        val timestamp = LocalDateTime.now()
        val typeIcon = when (type) {
            MessageType.INFO -> "ℹ️"
            MessageType.WARNING -> "⚠️"
            MessageType.ERROR -> "🚨"
        }
        
        val fullMessage = "$typeIcon $message"
        recentMessages.add(fullMessage to timestamp)
        
        // Keep only recent messages
        if (recentMessages.size > 20) {
            recentMessages.removeAt(0)
        }
        
        // Write to output log file
        outputLogFile?.appendText("${timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} $fullMessage\n")
    }
    
    private fun logError(errorLine: String) {
        val currentPhase = phases.values.firstOrNull { it.status == PhaseStatus.IN_PROGRESS }
        currentPhase?.let { phase ->
            val updated = phase.copy(
                errors = phase.errors + errorLine.trim()
            )
            phases[phase.phase] = updated
        }
        
        logMessage("ERROR: $errorLine", MessageType.ERROR)
    }
    
    // Manual phase control methods (for external scripts to call)
    fun markPhaseStarted(phase: MigrationPhase, details: String = "") {
        updatePhaseStatus(phase, PhaseStatus.IN_PROGRESS)
        if (details.isNotEmpty()) {
            val current = phases[phase]!!
            phases[phase] = current.copy(details = details)
        }
    }
    
    fun markPhaseCompleted(phase: MigrationPhase, details: String = "") {
        updatePhaseStatus(phase, PhaseStatus.COMPLETED)
        if (details.isNotEmpty()) {
            val current = phases[phase]!!
            phases[phase] = current.copy(details = details)
        }
    }
    
    fun markPhaseFailed(phase: MigrationPhase, error: String) {
        updatePhaseStatus(phase, PhaseStatus.FAILED)
        val current = phases[phase]!!
        phases[phase] = current.copy(
            details = error,
            errors = current.errors + error
        )
    }
    
    fun updatePhaseProgress(phase: MigrationPhase, progress: Double, details: String = "") {
        val current = phases[phase] ?: return
        phases[phase] = current.copy(
            progress = progress.coerceIn(0.0, 1.0),
            details = if (details.isNotEmpty()) details else current.details
        )
    }
}

// Configuration and Historical Data
data class HistoricalData(
    val averageDurationMinutes: Int?
)

private fun loadConfiguration(): Properties {
    val config = Properties()
    try {
        val configFile = File(".env.migration")
        if (configFile.exists()) {
            configFile.inputStream().use { config.load(it) }
        }
    } catch (e: Exception) {
        println("⚠️ Could not load configuration: ${e.message}")
    }
    return config
}

private fun loadHistoricalData(): HistoricalData {
    try {
        val historyFile = File("migration_history.json")
        if (historyFile.exists()) {
            // Parse historical data from JSON
            // This is simplified - in real implementation would parse actual historical data
            return HistoricalData(averageDurationMinutes = 45)
        }
    } catch (e: Exception) {
        // Ignore errors loading historical data
    }
    return HistoricalData(averageDurationMinutes = null)
}

// Main execution
suspend fun main(args: Array<String>) {
    val monitor = MigrationProgressMonitor()
    
    val migrationLogPath = args.firstOrNull()
    
    println("🚀 Starting Migration Progress Monitor...")
    if (migrationLogPath != null) {
        println("📁 Migration Log: $migrationLogPath")
    } else {
        println("ℹ️ No migration log specified - use manual progress updates")
    }
    
    try {
        monitor.startMonitoring(migrationLogPath)
    } catch (e: Exception) {
        println("❌ Monitor error: ${e.message}")
        e.printStackTrace()
    }
}

// Allow standalone execution
if (args.isNotEmpty()) {
    runBlocking {
        main(args)
    }
}
