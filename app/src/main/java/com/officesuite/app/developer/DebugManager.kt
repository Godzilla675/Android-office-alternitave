package com.officesuite.app.developer

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Debug Mode Manager for Detailed Logging and Troubleshooting.
 * Implements Technical Improvements Phase 2 - Section 22: Debug Mode & Beta Features
 * 
 * Features:
 * - Detailed logging with levels
 * - Log file export
 * - Performance timing
 * - Beta features toggle
 * - Crash reporting
 */
object DebugManager {
    
    private const val TAG = "DebugManager"
    private const val PREFS_NAME = "debug_prefs"
    private const val KEY_DEBUG_MODE = "debug_mode"
    private const val KEY_BETA_FEATURES = "beta_features"
    private const val KEY_LOG_LEVEL = "log_level"
    private const val KEY_ENABLED_FEATURES = "enabled_features"
    private const val MAX_LOG_ENTRIES = 1000
    
    private var isInitialized = false
    private lateinit var prefs: android.content.SharedPreferences
    private val gson = Gson()
    
    // Log storage
    private val logEntries = ConcurrentLinkedQueue<LogEntry>()
    
    // Timing map for performance measurement
    private val timingMap = mutableMapOf<String, Long>()
    
    // Beta feature flags
    private val betaFeatures = mutableMapOf<String, BetaFeature>()
    
    /**
     * Initialize the debug manager.
     */
    fun init(context: Context) {
        if (isInitialized) return
        
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isInitialized = true
        
        // Register default beta features
        registerDefaultBetaFeatures()
        
        // Load enabled features from storage
        loadEnabledFeatures()
        
        log(LogLevel.INFO, TAG, "Debug Manager initialized")
    }
    
    /**
     * Check if debug mode is enabled.
     */
    var isDebugMode: Boolean
        get() = if (isInitialized) prefs.getBoolean(KEY_DEBUG_MODE, false) else false
        set(value) {
            if (isInitialized) {
                prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()
                log(LogLevel.INFO, TAG, "Debug mode ${if (value) "enabled" else "disabled"}")
            }
        }
    
    /**
     * Current log level.
     */
    var logLevel: LogLevel
        get() {
            if (!isInitialized) return LogLevel.INFO
            val ordinal = prefs.getInt(KEY_LOG_LEVEL, LogLevel.INFO.ordinal)
            return LogLevel.values().getOrElse(ordinal) { LogLevel.INFO }
        }
        set(value) {
            if (isInitialized) {
                prefs.edit().putInt(KEY_LOG_LEVEL, value.ordinal).apply()
            }
        }
    
    /**
     * Log a message.
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        // Skip if below current level and not debug mode
        if (!isDebugMode && level.ordinal < logLevel.ordinal) return
        
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            exception = throwable?.stackTraceToString()
        )
        
        // Add to queue
        logEntries.add(entry)
        while (logEntries.size > MAX_LOG_ENTRIES) {
            logEntries.poll()
        }
        
        // Also log to Android logcat
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARNING -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
    
    /**
     * Convenience log methods.
     */
    fun v(tag: String, message: String) = log(LogLevel.VERBOSE, tag, message)
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARNING, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)
    
    /**
     * Start timing an operation.
     */
    fun startTiming(operationName: String) {
        if (!isDebugMode) return
        timingMap[operationName] = System.nanoTime()
        d(TAG, "⏱️ Started: $operationName")
    }
    
    /**
     * Stop timing and log the duration.
     */
    fun stopTiming(operationName: String): Long {
        if (!isDebugMode) return 0
        
        val startTime = timingMap.remove(operationName) ?: return 0
        val durationNanos = System.nanoTime() - startTime
        val durationMs = durationNanos / 1_000_000
        
        d(TAG, "⏱️ Completed: $operationName in ${durationMs}ms")
        return durationMs
    }
    
    /**
     * Time a block of code.
     */
    inline fun <T> timed(operationName: String, block: () -> T): T {
        startTiming(operationName)
        try {
            return block()
        } finally {
            stopTiming(operationName)
        }
    }
    
    /**
     * Get all log entries.
     */
    fun getLogs(level: LogLevel? = null, limit: Int = 100): List<LogEntry> {
        return logEntries
            .filter { level == null || it.level == level }
            .takeLast(limit)
    }
    
    /**
     * Export logs to a file.
     */
    fun exportLogs(context: Context): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "office_suite_logs_${dateFormat.format(Date())}.txt"
        val file = File(context.cacheDir, fileName)
        
        file.writeText(buildString {
            appendLine("=== Office Suite Debug Logs ===")
            appendLine("Exported: ${dateFormat.format(Date())}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.SDK_INT}")
            appendLine("App Version: 1.0")
            appendLine("Debug Mode: $isDebugMode")
            appendLine("Log Level: $logLevel")
            appendLine()
            appendLine("=== Logs ===")
            appendLine()
            
            logEntries.forEach { entry ->
                val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                    .format(Date(entry.timestamp))
                appendLine("[$time] [${entry.level.name}] ${entry.tag}: ${entry.message}")
                entry.exception?.let { appendLine(it) }
            }
        })
        
        i(TAG, "Logs exported to: ${file.absolutePath}")
        return file
    }
    
    /**
     * Clear all logs.
     */
    fun clearLogs() {
        logEntries.clear()
        i(TAG, "Logs cleared")
    }
    
    // ===================== Beta Features =====================
    
    /**
     * Register a beta feature.
     */
    fun registerBetaFeature(feature: BetaFeature) {
        betaFeatures[feature.id] = feature
    }
    
    /**
     * Check if a beta feature is enabled.
     */
    fun isBetaFeatureEnabled(featureId: String): Boolean {
        return betaFeatures[featureId]?.isEnabled ?: false
    }
    
    /**
     * Enable or disable a beta feature.
     */
    fun setBetaFeatureEnabled(featureId: String, enabled: Boolean) {
        betaFeatures[featureId]?.let { feature ->
            betaFeatures[featureId] = feature.copy(isEnabled = enabled)
            saveEnabledFeatures()
            
            i(TAG, "Beta feature '$featureId' ${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    /**
     * Get all beta features.
     */
    fun getAllBetaFeatures(): List<BetaFeature> = betaFeatures.values.toList()
    
    /**
     * Get enabled beta features.
     */
    fun getEnabledBetaFeatures(): List<BetaFeature> = betaFeatures.values.filter { it.isEnabled }
    
    private fun registerDefaultBetaFeatures() {
        // Document Streaming (Section 21)
        registerBetaFeature(BetaFeature(
            id = "document_streaming",
            name = "Document Streaming",
            description = "Stream large documents without full download",
            category = FeatureCategory.PERFORMANCE,
            isExperimental = true
        ))
        
        // Predictive Pre-loading (Section 21)
        registerBetaFeature(BetaFeature(
            id = "predictive_preloading",
            name = "Predictive Pre-loading",
            description = "AI-predicted document pre-loading for faster access",
            category = FeatureCategory.PERFORMANCE,
            isExperimental = true
        ))
        
        // Delta Compression (Section 21)
        registerBetaFeature(BetaFeature(
            id = "delta_compression",
            name = "Delta Compression",
            description = "Compress document changes instead of full files",
            category = FeatureCategory.PERFORMANCE,
            isExperimental = true
        ))
        
        // Plugin System (Section 22)
        registerBetaFeature(BetaFeature(
            id = "plugin_system",
            name = "Plugin System",
            description = "Support for third-party plugins and extensions",
            category = FeatureCategory.DEVELOPER,
            isExperimental = true
        ))
        
        // Scripting Support (Section 22)
        registerBetaFeature(BetaFeature(
            id = "scripting_support",
            name = "Scripting Support",
            description = "JavaScript scripting for automation",
            category = FeatureCategory.DEVELOPER,
            isExperimental = true
        ))
        
        // Webhook Support (Section 22)
        registerBetaFeature(BetaFeature(
            id = "webhook_support",
            name = "Webhook Support",
            description = "Trigger webhooks on document events",
            category = FeatureCategory.DEVELOPER,
            isExperimental = true
        ))
        
        // API Access (Section 22)
        registerBetaFeature(BetaFeature(
            id = "api_access",
            name = "REST API Access",
            description = "REST API for external integrations",
            category = FeatureCategory.DEVELOPER,
            isExperimental = true
        ))
        
        // AI Document Intelligence (Section 21 - Quick Wins)
        registerBetaFeature(BetaFeature(
            id = "ai_document_intelligence",
            name = "AI Document Intelligence",
            description = "AI-powered document analysis and suggestions",
            category = FeatureCategory.AI,
            isExperimental = false
        ))
        
        // CRDT Collaboration (Section 24)
        registerBetaFeature(BetaFeature(
            id = "crdt_collaboration",
            name = "Real-time Collaboration (CRDT)",
            description = "CRDT-based merge for conflict-free collaboration",
            category = FeatureCategory.COLLABORATION,
            isExperimental = true
        ))
        
        // Edge Computing (Section 24)
        registerBetaFeature(BetaFeature(
            id = "edge_computing",
            name = "Edge Computing",
            description = "Process documents at edge servers for speed",
            category = FeatureCategory.INFRASTRUCTURE,
            isExperimental = true
        ))
    }
    
    private fun loadEnabledFeatures() {
        val json = prefs.getString(KEY_ENABLED_FEATURES, null) ?: return
        try {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            val enabledMap: Map<String, Boolean> = gson.fromJson(json, type)
            
            enabledMap.forEach { (id, enabled) ->
                betaFeatures[id]?.let { feature ->
                    betaFeatures[id] = feature.copy(isEnabled = enabled)
                }
            }
        } catch (e: Exception) {
            w(TAG, "Failed to load enabled features: ${e.message}")
        }
    }
    
    private fun saveEnabledFeatures() {
        val enabledMap = betaFeatures.mapValues { it.value.isEnabled }
        val json = gson.toJson(enabledMap)
        prefs.edit().putString(KEY_ENABLED_FEATURES, json).apply()
    }
    
    /**
     * Get debug info for troubleshooting.
     */
    fun getDebugInfo(context: Context): String {
        return buildString {
            appendLine("=== Debug Information ===")
            appendLine()
            appendLine("Device Info:")
            appendLine("  Manufacturer: ${Build.MANUFACTURER}")
            appendLine("  Model: ${Build.MODEL}")
            appendLine("  Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("App Info:")
            appendLine("  Debug Mode: $isDebugMode")
            appendLine("  Log Level: $logLevel")
            appendLine("  Log Entries: ${logEntries.size}")
            appendLine()
            appendLine("Beta Features:")
            betaFeatures.values.filter { it.isEnabled }.forEach { feature ->
                appendLine("  ✓ ${feature.name}")
            }
            appendLine()
            appendLine("Memory:")
            val runtime = Runtime.getRuntime()
            val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val maxMb = runtime.maxMemory() / (1024 * 1024)
            appendLine("  Used: ${usedMb}MB / ${maxMb}MB")
        }
    }
}

/**
 * Log levels.
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

/**
 * Log entry.
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val exception: String? = null
)

/**
 * Beta feature.
 */
data class BetaFeature(
    val id: String,
    val name: String,
    val description: String,
    val category: FeatureCategory,
    val isExperimental: Boolean = false,
    val isEnabled: Boolean = false
)

/**
 * Feature categories.
 */
enum class FeatureCategory(val displayName: String) {
    PERFORMANCE("Performance"),
    DEVELOPER("Developer Tools"),
    AI("AI & ML"),
    COLLABORATION("Collaboration"),
    INFRASTRUCTURE("Infrastructure"),
    ENTERPRISE("Enterprise"),
    UI("User Interface")
}
