package com.officesuite.app.platform

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Manages Digital Wellbeing features for the Office Suite app.
 * Implements usage time tracking and daily limits.
 * 
 * This is part of Phase 2 Platform-Specific Features (Section 25).
 */
class DigitalWellbeingManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    private val _usageState = MutableStateFlow(UsageState())
    val usageState: StateFlow<UsageState> = _usageState
    
    private var sessionStartTime: Long = 0
    
    /**
     * Data class representing current usage state
     */
    data class UsageState(
        val todayUsageMinutes: Int = 0,
        val dailyLimitMinutes: Int = 0,
        val isLimitEnabled: Boolean = false,
        val isLimitReached: Boolean = false,
        val weeklyUsageMinutes: Int = 0,
        val averageDailyUsage: Int = 0,
        val currentSessionMinutes: Int = 0
    )
    
    /**
     * Daily usage statistics
     */
    data class DailyStats(
        val date: String,
        val usageMinutes: Int,
        val documentsViewed: Int,
        val documentsEdited: Int,
        val pagesRead: Int,
        val scansMade: Int
    )
    
    /**
     * Weekly usage summary
     */
    data class WeeklySummary(
        val totalMinutes: Int,
        val averageMinutesPerDay: Int,
        val peakDay: String,
        val peakUsageMinutes: Int,
        val documentsViewed: Int,
        val documentsEdited: Int,
        val mostActiveHour: Int
    )
    
    /**
     * Usage reminder settings
     */
    data class UsageReminderSettings(
        val isEnabled: Boolean,
        val reminderIntervalMinutes: Int,
        val breakReminderEnabled: Boolean,
        val breakReminderIntervalMinutes: Int
    )
    
    init {
        updateUsageState()
    }
    
    /**
     * Start tracking a new session
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
    }
    
    /**
     * End the current session and record usage
     */
    fun endSession() {
        if (sessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            val sessionMinutes = TimeUnit.MILLISECONDS.toMinutes(sessionDuration).toInt()
            addUsageTime(sessionMinutes)
            sessionStartTime = 0
        }
    }
    
    /**
     * Add usage time in minutes
     */
    fun addUsageTime(minutes: Int) {
        val today = getTodayKey()
        val currentUsage = prefs.getInt("$KEY_USAGE_PREFIX$today", 0)
        prefs.edit().putInt("$KEY_USAGE_PREFIX$today", currentUsage + minutes).apply()
        updateUsageState()
    }
    
    /**
     * Get today's usage in minutes
     */
    fun getTodayUsage(): Int {
        val today = getTodayKey()
        return prefs.getInt("$KEY_USAGE_PREFIX$today", 0)
    }
    
    /**
     * Get this week's total usage in minutes
     */
    fun getWeeklyUsage(): Int {
        var total = 0
        for (i in 0..6) {
            val dayKey = getDayKey(i)
            total += prefs.getInt("$KEY_USAGE_PREFIX$dayKey", 0)
        }
        return total
    }
    
    /**
     * Set daily usage limit in minutes
     */
    fun setDailyLimit(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_LIMIT, minutes).apply()
        updateUsageState()
    }
    
    /**
     * Get daily usage limit in minutes
     */
    fun getDailyLimit(): Int {
        return prefs.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT)
    }
    
    /**
     * Enable or disable daily limit
     */
    fun setLimitEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LIMIT_ENABLED, enabled).apply()
        updateUsageState()
    }
    
    /**
     * Check if daily limit is enabled
     */
    fun isLimitEnabled(): Boolean {
        return prefs.getBoolean(KEY_LIMIT_ENABLED, false)
    }
    
    /**
     * Check if daily limit has been reached
     */
    fun isLimitReached(): Boolean {
        return isLimitEnabled() && getTodayUsage() >= getDailyLimit()
    }
    
    /**
     * Get remaining time until limit in minutes
     */
    fun getRemainingTime(): Int {
        val limit = getDailyLimit()
        val used = getTodayUsage()
        return maxOf(0, limit - used)
    }
    
    /**
     * Get usage percentage for today
     */
    fun getUsagePercentage(): Float {
        val limit = getDailyLimit()
        if (limit <= 0) return 0f
        val used = getTodayUsage()
        return (used.toFloat() / limit.toFloat() * 100f).coerceIn(0f, 100f)
    }
    
    /**
     * Get daily statistics
     */
    fun getDailyStats(): DailyStats {
        val today = getTodayKey()
        return DailyStats(
            date = today,
            usageMinutes = prefs.getInt("$KEY_USAGE_PREFIX$today", 0),
            documentsViewed = prefs.getInt("$KEY_DOCS_VIEWED_PREFIX$today", 0),
            documentsEdited = prefs.getInt("$KEY_DOCS_EDITED_PREFIX$today", 0),
            pagesRead = prefs.getInt("$KEY_PAGES_READ_PREFIX$today", 0),
            scansMade = prefs.getInt("$KEY_SCANS_PREFIX$today", 0)
        )
    }
    
    /**
     * Get weekly summary
     */
    fun getWeeklySummary(): WeeklySummary {
        var totalMinutes = 0
        var peakMinutes = 0
        var peakDay = ""
        var totalDocsViewed = 0
        var totalDocsEdited = 0
        
        for (i in 0..6) {
            val dayKey = getDayKey(i)
            val dayUsage = prefs.getInt("$KEY_USAGE_PREFIX$dayKey", 0)
            totalMinutes += dayUsage
            totalDocsViewed += prefs.getInt("$KEY_DOCS_VIEWED_PREFIX$dayKey", 0)
            totalDocsEdited += prefs.getInt("$KEY_DOCS_EDITED_PREFIX$dayKey", 0)
            
            if (dayUsage > peakMinutes) {
                peakMinutes = dayUsage
                peakDay = dayKey
            }
        }
        
        return WeeklySummary(
            totalMinutes = totalMinutes,
            averageMinutesPerDay = totalMinutes / 7,
            peakDay = peakDay,
            peakUsageMinutes = peakMinutes,
            documentsViewed = totalDocsViewed,
            documentsEdited = totalDocsEdited,
            mostActiveHour = getMostActiveHour()
        )
    }
    
    /**
     * Get usage reminder settings
     */
    fun getReminderSettings(): UsageReminderSettings {
        return UsageReminderSettings(
            isEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
            reminderIntervalMinutes = prefs.getInt(KEY_REMINDER_INTERVAL, 30),
            breakReminderEnabled = prefs.getBoolean(KEY_BREAK_REMINDER_ENABLED, true),
            breakReminderIntervalMinutes = prefs.getInt(KEY_BREAK_INTERVAL, 60)
        )
    }
    
    /**
     * Set usage reminder settings
     */
    fun setReminderSettings(settings: UsageReminderSettings) {
        prefs.edit().apply {
            putBoolean(KEY_REMINDER_ENABLED, settings.isEnabled)
            putInt(KEY_REMINDER_INTERVAL, settings.reminderIntervalMinutes)
            putBoolean(KEY_BREAK_REMINDER_ENABLED, settings.breakReminderEnabled)
            putInt(KEY_BREAK_INTERVAL, settings.breakReminderIntervalMinutes)
            apply()
        }
    }
    
    /**
     * Record document viewed
     */
    fun recordDocumentViewed() {
        val today = getTodayKey()
        val count = prefs.getInt("$KEY_DOCS_VIEWED_PREFIX$today", 0)
        prefs.edit().putInt("$KEY_DOCS_VIEWED_PREFIX$today", count + 1).apply()
    }
    
    /**
     * Record document edited
     */
    fun recordDocumentEdited() {
        val today = getTodayKey()
        val count = prefs.getInt("$KEY_DOCS_EDITED_PREFIX$today", 0)
        prefs.edit().putInt("$KEY_DOCS_EDITED_PREFIX$today", count + 1).apply()
    }
    
    /**
     * Record pages read
     */
    fun recordPagesRead(pages: Int) {
        val today = getTodayKey()
        val count = prefs.getInt("$KEY_PAGES_READ_PREFIX$today", 0)
        prefs.edit().putInt("$KEY_PAGES_READ_PREFIX$today", count + pages).apply()
    }
    
    /**
     * Record scan made
     */
    fun recordScanMade() {
        val today = getTodayKey()
        val count = prefs.getInt("$KEY_SCANS_PREFIX$today", 0)
        prefs.edit().putInt("$KEY_SCANS_PREFIX$today", count + 1).apply()
    }
    
    /**
     * Record hourly usage for analytics
     */
    fun recordHourlyUsage() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val key = "$KEY_HOURLY_USAGE$hour"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
    }
    
    /**
     * Get most active hour
     */
    private fun getMostActiveHour(): Int {
        var maxUsage = 0
        var peakHour = 0
        
        for (hour in 0..23) {
            val usage = prefs.getInt("$KEY_HOURLY_USAGE$hour", 0)
            if (usage > maxUsage) {
                maxUsage = usage
                peakHour = hour
            }
        }
        
        return peakHour
    }
    
    /**
     * Format time for display (e.g., "2h 30m")
     */
    fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
    
    /**
     * Get current session duration in minutes
     */
    fun getCurrentSessionMinutes(): Int {
        if (sessionStartTime == 0L) return 0
        val duration = System.currentTimeMillis() - sessionStartTime
        return TimeUnit.MILLISECONDS.toMinutes(duration).toInt()
    }
    
    /**
     * Should show break reminder
     */
    fun shouldShowBreakReminder(): Boolean {
        val settings = getReminderSettings()
        if (!settings.breakReminderEnabled) return false
        
        val sessionMinutes = getCurrentSessionMinutes()
        return sessionMinutes > 0 && 
               sessionMinutes >= settings.breakReminderIntervalMinutes &&
               sessionMinutes % settings.breakReminderIntervalMinutes == 0
    }
    
    /**
     * Reset daily statistics (for testing)
     */
    fun resetDailyStats() {
        val today = getTodayKey()
        prefs.edit().apply {
            remove("$KEY_USAGE_PREFIX$today")
            remove("$KEY_DOCS_VIEWED_PREFIX$today")
            remove("$KEY_DOCS_EDITED_PREFIX$today")
            remove("$KEY_PAGES_READ_PREFIX$today")
            remove("$KEY_SCANS_PREFIX$today")
            apply()
        }
        updateUsageState()
    }
    
    private fun updateUsageState() {
        _usageState.value = UsageState(
            todayUsageMinutes = getTodayUsage(),
            dailyLimitMinutes = getDailyLimit(),
            isLimitEnabled = isLimitEnabled(),
            isLimitReached = isLimitReached(),
            weeklyUsageMinutes = getWeeklyUsage(),
            averageDailyUsage = getWeeklyUsage() / 7,
            currentSessionMinutes = getCurrentSessionMinutes()
        )
    }
    
    private fun getTodayKey(): String {
        val cal = Calendar.getInstance()
        // Use explicit format: YYYY-MM-DD for clarity and no ambiguity
        val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
        return "${cal.get(Calendar.YEAR)}-$month-$day"
    }
    
    private fun getDayKey(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        // Use explicit format: YYYY-MM-DD for clarity and no ambiguity
        val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
        return "${cal.get(Calendar.YEAR)}-$month-$day"
    }
    
    companion object {
        private const val PREFS_NAME = "digital_wellbeing_prefs"
        private const val KEY_USAGE_PREFIX = "usage_"
        private const val KEY_DOCS_VIEWED_PREFIX = "docs_viewed_"
        private const val KEY_DOCS_EDITED_PREFIX = "docs_edited_"
        private const val KEY_PAGES_READ_PREFIX = "pages_read_"
        private const val KEY_SCANS_PREFIX = "scans_"
        private const val KEY_HOURLY_USAGE = "hourly_usage_"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_LIMIT_ENABLED = "limit_enabled"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_INTERVAL = "reminder_interval"
        private const val KEY_BREAK_REMINDER_ENABLED = "break_reminder_enabled"
        private const val KEY_BREAK_INTERVAL = "break_interval"
        private const val DEFAULT_DAILY_LIMIT = 120 // 2 hours in minutes
    }
}
