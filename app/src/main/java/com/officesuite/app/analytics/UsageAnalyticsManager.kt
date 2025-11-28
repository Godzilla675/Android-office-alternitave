package com.officesuite.app.analytics

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Usage Analytics Manager for Enterprise Usage Reporting.
 * Implements Technical Improvements Phase 2 - Section 23: Usage Analytics Dashboard
 * 
 * Features:
 * - Document usage tracking
 * - Feature usage analytics
 * - User activity metrics
 * - Export reports
 * - Privacy-compliant data collection
 */
class UsageAnalyticsManager private constructor(context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // In-memory analytics
    private var currentSession: SessionData? = null
    private val dailyStats = mutableMapOf<String, DailyStats>()
    private val featureUsage = mutableMapOf<String, Int>()
    
    // State flows for reactive UI
    private val _analytics = MutableStateFlow(AnalyticsSummary())
    val analytics: StateFlow<AnalyticsSummary> = _analytics
    
    // Privacy flag
    var isAnalyticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANALYTICS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ANALYTICS_ENABLED, value).apply()
        }
    
    init {
        loadAnalytics()
    }
    
    /**
     * Start a new session.
     */
    fun startSession() {
        if (!isAnalyticsEnabled) return
        
        currentSession = SessionData(
            id = UUID.randomUUID().toString(),
            startTime = System.currentTimeMillis()
        )
        
        incrementDailyStat("sessions")
    }
    
    /**
     * End the current session.
     */
    fun endSession() {
        if (!isAnalyticsEnabled) return
        
        currentSession?.let { session ->
            val duration = System.currentTimeMillis() - session.startTime
            incrementDailyStat("total_session_duration_ms", duration)
            
            // Update averages
            val today = getTodayKey()
            dailyStats[today]?.let { stats ->
                val avgDuration = stats.stats["total_session_duration_ms"]?.div(
                    stats.stats["sessions"] ?: 1
                ) ?: 0
                stats.stats["avg_session_duration_ms"] = avgDuration
            }
            
            saveAnalytics()
            updateSummary()
        }
        
        currentSession = null
    }
    
    /**
     * Track a document view.
     */
    fun trackDocumentView(documentType: String, source: String = "unknown") {
        if (!isAnalyticsEnabled) return
        
        incrementDailyStat("documents_viewed")
        incrementDailyStat("documents_viewed_$documentType")
        incrementFeatureUsage("document_view")
        
        currentSession?.let { session ->
            session.documentViews++
            session.documentTypes.add(documentType)
        }
        
        saveAnalytics()
        updateSummary()
    }
    
    /**
     * Track a document edit.
     */
    fun trackDocumentEdit(documentType: String, wordsWritten: Int = 0) {
        if (!isAnalyticsEnabled) return
        
        incrementDailyStat("documents_edited")
        incrementDailyStat("documents_edited_$documentType")
        incrementDailyStat("words_written", wordsWritten.toLong())
        incrementFeatureUsage("document_edit")
        
        currentSession?.let { session ->
            session.documentsEdited++
            session.wordsWritten += wordsWritten
        }
        
        saveAnalytics()
        updateSummary()
    }
    
    /**
     * Track a document creation.
     */
    fun trackDocumentCreation(documentType: String, source: String = "blank") {
        if (!isAnalyticsEnabled) return
        
        incrementDailyStat("documents_created")
        incrementDailyStat("documents_created_$documentType")
        incrementFeatureUsage("document_create")
        
        saveAnalytics()
        updateSummary()
    }
    
    /**
     * Track feature usage.
     */
    fun trackFeatureUsage(featureName: String) {
        if (!isAnalyticsEnabled) return
        
        incrementFeatureUsage(featureName)
        incrementDailyStat("feature_$featureName")
        
        saveAnalytics()
        updateSummary()
    }
    
    /**
     * Track a scan.
     */
    fun trackScan(scanType: String, pagesScanned: Int = 1) {
        if (!isAnalyticsEnabled) return
        
        incrementDailyStat("scans")
        incrementDailyStat("pages_scanned", pagesScanned.toLong())
        incrementFeatureUsage("scanner")
        incrementFeatureUsage("scanner_$scanType")
        
        saveAnalytics()
        updateSummary()
    }
    
    /**
     * Track a conversion.
     */
    fun trackConversion(fromType: String, toType: String) {
        if (!isAnalyticsEnabled) return
        
        incrementDailyStat("conversions")
        incrementFeatureUsage("converter")
        incrementFeatureUsage("convert_${fromType}_to_$toType")
        
        saveAnalytics()
        updateSummary()
    }
    
    /**
     * Track reading time.
     */
    fun trackReadingTime(durationMs: Long) {
        if (!isAnalyticsEnabled) return
        
        incrementDailyStat("reading_time_ms", durationMs)
        incrementFeatureUsage("reading")
        
        currentSession?.readingTimeMs = (currentSession?.readingTimeMs ?: 0) + durationMs
        
        saveAnalytics()
        updateSummary()
    }
    
    /**
     * Track export/share action.
     */
    fun trackExport(exportType: String) {
        if (!isAnalyticsEnabled) return
        
        incrementDailyStat("exports")
        incrementFeatureUsage("export_$exportType")
        
        saveAnalytics()
        updateSummary()
    }
    
    /**
     * Get analytics summary for a time period.
     */
    fun getSummary(period: AnalyticsPeriod = AnalyticsPeriod.ALL_TIME): AnalyticsSummary {
        val relevantDays = getRelevantDays(period)
        val relevantStats = dailyStats.filter { relevantDays.contains(it.key) }
        
        return AnalyticsSummary(
            period = period,
            documentsViewed = sumStat(relevantStats, "documents_viewed"),
            documentsEdited = sumStat(relevantStats, "documents_edited"),
            documentsCreated = sumStat(relevantStats, "documents_created"),
            wordsWritten = sumStat(relevantStats, "words_written"),
            scans = sumStat(relevantStats, "scans"),
            conversions = sumStat(relevantStats, "conversions"),
            totalSessionsCount = sumStat(relevantStats, "sessions"),
            totalSessionDurationMs = sumStat(relevantStats, "total_session_duration_ms"),
            readingTimeMs = sumStat(relevantStats, "reading_time_ms"),
            topFeatures = getTopFeatures(10),
            activeDays = relevantStats.size
        )
    }
    
    /**
     * Get daily breakdown for a period.
     */
    fun getDailyBreakdown(period: AnalyticsPeriod = AnalyticsPeriod.LAST_7_DAYS): List<DailyStats> {
        val relevantDays = getRelevantDays(period)
        return relevantDays.mapNotNull { dailyStats[it] }.sortedBy { it.date }
    }
    
    /**
     * Get feature usage stats.
     */
    fun getFeatureUsageStats(): Map<String, Int> {
        return featureUsage.toMap()
    }
    
    /**
     * Export analytics data as JSON.
     */
    fun exportAnalytics(): String {
        val export = AnalyticsExport(
            exportDate = System.currentTimeMillis(),
            summary = getSummary(AnalyticsPeriod.ALL_TIME),
            dailyStats = dailyStats.values.toList(),
            featureUsage = featureUsage.toMap()
        )
        return gson.toJson(export)
    }
    
    /**
     * Export analytics as CSV.
     */
    fun exportAnalyticsAsCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Sessions,Documents Viewed,Documents Edited,Documents Created,Words Written,Scans,Conversions,Reading Time (min)")
        
        dailyStats.values.sortedBy { it.date }.forEach { stats ->
            sb.appendLine(listOf(
                stats.date,
                stats.stats["sessions"] ?: 0,
                stats.stats["documents_viewed"] ?: 0,
                stats.stats["documents_edited"] ?: 0,
                stats.stats["documents_created"] ?: 0,
                stats.stats["words_written"] ?: 0,
                stats.stats["scans"] ?: 0,
                stats.stats["conversions"] ?: 0,
                (stats.stats["reading_time_ms"] ?: 0) / 60000
            ).joinToString(","))
        }
        
        return sb.toString()
    }
    
    /**
     * Clear all analytics data.
     */
    fun clearAllData() {
        dailyStats.clear()
        featureUsage.clear()
        currentSession = null
        prefs.edit().clear().apply()
        prefs.edit().putBoolean(KEY_ANALYTICS_ENABLED, isAnalyticsEnabled).apply()
        updateSummary()
    }
    
    /**
     * Clear data older than specified days.
     */
    fun clearOldData(daysToKeep: Int = 90) {
        val cutoff = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToKeep)
        }.timeInMillis
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cutoffStr = dateFormat.format(Date(cutoff))
        
        dailyStats.keys.filter { it < cutoffStr }.forEach { dailyStats.remove(it) }
        
        saveAnalytics()
    }
    
    private fun incrementDailyStat(statName: String, amount: Long = 1) {
        val today = getTodayKey()
        val stats = dailyStats.getOrPut(today) { 
            DailyStats(date = today, stats = mutableMapOf()) 
        }
        stats.stats[statName] = (stats.stats[statName] ?: 0) + amount
    }
    
    private fun incrementFeatureUsage(featureName: String) {
        featureUsage[featureName] = (featureUsage[featureName] ?: 0) + 1
    }
    
    private fun getTodayKey(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    private fun getRelevantDays(period: AnalyticsPeriod): Set<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        val daysBack = when (period) {
            AnalyticsPeriod.TODAY -> 1
            AnalyticsPeriod.LAST_7_DAYS -> 7
            AnalyticsPeriod.LAST_30_DAYS -> 30
            AnalyticsPeriod.LAST_90_DAYS -> 90
            AnalyticsPeriod.ALL_TIME -> 365 * 10 // 10 years
        }
        
        return (0 until daysBack).map { daysAgo ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            dateFormat.format(calendar.time)
        }.toSet()
    }
    
    private fun sumStat(stats: Map<String, DailyStats>, statName: String): Long {
        return stats.values.sumOf { it.stats[statName] ?: 0 }
    }
    
    private fun getTopFeatures(limit: Int): List<FeatureUsageStat> {
        return featureUsage.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { FeatureUsageStat(it.key, it.value) }
    }
    
    private fun loadAnalytics() {
        try {
            val statsJson = prefs.getString(KEY_DAILY_STATS, null)
            if (statsJson != null) {
                val type = object : TypeToken<Map<String, DailyStats>>() {}.type
                val loaded: Map<String, DailyStats> = gson.fromJson(statsJson, type)
                dailyStats.putAll(loaded)
            }
            
            val featureJson = prefs.getString(KEY_FEATURE_USAGE, null)
            if (featureJson != null) {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                val loaded: Map<String, Int> = gson.fromJson(featureJson, type)
                featureUsage.putAll(loaded)
            }
        } catch (e: Exception) {
            // Start fresh on error
        }
        
        updateSummary()
    }
    
    private fun saveAnalytics() {
        prefs.edit()
            .putString(KEY_DAILY_STATS, gson.toJson(dailyStats))
            .putString(KEY_FEATURE_USAGE, gson.toJson(featureUsage))
            .apply()
    }
    
    private fun updateSummary() {
        _analytics.value = getSummary(AnalyticsPeriod.ALL_TIME)
    }
    
    companion object {
        private const val PREFS_NAME = "usage_analytics_prefs"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        private const val KEY_DAILY_STATS = "daily_stats"
        private const val KEY_FEATURE_USAGE = "feature_usage"
        
        @Volatile
        private var instance: UsageAnalyticsManager? = null
        
        fun getInstance(context: Context): UsageAnalyticsManager {
            return instance ?: synchronized(this) {
                instance ?: UsageAnalyticsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Session data.
 */
data class SessionData(
    val id: String,
    val startTime: Long,
    var documentViews: Int = 0,
    var documentsEdited: Int = 0,
    var wordsWritten: Int = 0,
    var readingTimeMs: Long = 0,
    val documentTypes: MutableSet<String> = mutableSetOf()
)

/**
 * Daily statistics.
 */
data class DailyStats(
    val date: String,
    val stats: MutableMap<String, Long>
)

/**
 * Analytics summary.
 */
data class AnalyticsSummary(
    val period: AnalyticsPeriod = AnalyticsPeriod.ALL_TIME,
    val documentsViewed: Long = 0,
    val documentsEdited: Long = 0,
    val documentsCreated: Long = 0,
    val wordsWritten: Long = 0,
    val scans: Long = 0,
    val conversions: Long = 0,
    val totalSessionsCount: Long = 0,
    val totalSessionDurationMs: Long = 0,
    val readingTimeMs: Long = 0,
    val topFeatures: List<FeatureUsageStat> = emptyList(),
    val activeDays: Int = 0
) {
    val averageSessionDurationMs: Long
        get() = if (totalSessionsCount > 0) totalSessionDurationMs / totalSessionsCount else 0
    
    val readingTimeMinutes: Long
        get() = readingTimeMs / 60000
    
    val sessionDurationMinutes: Long
        get() = totalSessionDurationMs / 60000
}

/**
 * Feature usage stat.
 */
data class FeatureUsageStat(
    val featureName: String,
    val usageCount: Int
)

/**
 * Analytics period.
 */
enum class AnalyticsPeriod(val displayName: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    ALL_TIME("All Time")
}

/**
 * Analytics export data.
 */
data class AnalyticsExport(
    val exportDate: Long,
    val summary: AnalyticsSummary,
    val dailyStats: List<DailyStats>,
    val featureUsage: Map<String, Int>
)
