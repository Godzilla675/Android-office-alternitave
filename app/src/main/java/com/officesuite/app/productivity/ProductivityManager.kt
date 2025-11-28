package com.officesuite.app.productivity

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Manager for productivity features including:
 * - Writing Streaks (Section 13)
 * - Reading Goals (Section 13)
 * - Productivity Stats (Section 13)
 * - Achievement Badges (Section 13)
 * - Document Completion Tracker (Section 13)
 * - Focus Timer/Pomodoro (Section 13)
 * - Study Mode (Section 13)
 */
class ProductivityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ================== Writing Streaks ==================
    
    /**
     * Record a writing activity for today
     */
    fun recordWritingActivity(wordCount: Int = 0, duration: Long = 0) {
        val today = getTodayString()
        val stats = getDailyStats(today) ?: DailyStats(today)
        val updatedStats = stats.copy(
            writingWordCount = stats.writingWordCount + wordCount,
            writingTimeMinutes = stats.writingTimeMinutes + TimeUnit.MILLISECONDS.toMinutes(duration).toInt(),
            hasWrittenToday = true
        )
        saveDailyStats(updatedStats)
        updateStreak()
    }
    
    fun getCurrentStreak(): Int {
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }
    
    fun getLongestStreak(): Int {
        return prefs.getInt(KEY_LONGEST_STREAK, 0)
    }
    
    private fun updateStreak() {
        val today = getTodayString()
        val yesterday = getYesterdayString()
        val lastWritingDay = prefs.getString(KEY_LAST_WRITING_DAY, null)
        
        val currentStreak = getCurrentStreak()
        val newStreak = when (lastWritingDay) {
            today -> currentStreak // Already counted today
            yesterday -> currentStreak + 1 // Consecutive day
            else -> 1 // New streak
        }
        
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, newStreak)
            .putString(KEY_LAST_WRITING_DAY, today)
            .apply()
        
        // Update longest streak if needed
        if (newStreak > getLongestStreak()) {
            prefs.edit().putInt(KEY_LONGEST_STREAK, newStreak).apply()
        }
        
        // Check for streak achievements
        checkStreakAchievements(newStreak)
    }

    // ================== Reading Goals ==================
    
    fun setDailyReadingGoal(pages: Int) {
        prefs.edit().putInt(KEY_DAILY_READING_GOAL, pages).apply()
    }
    
    fun getDailyReadingGoal(): Int {
        return prefs.getInt(KEY_DAILY_READING_GOAL, 10)
    }
    
    fun setWeeklyReadingGoal(pages: Int) {
        prefs.edit().putInt(KEY_WEEKLY_READING_GOAL, pages).apply()
    }
    
    fun getWeeklyReadingGoal(): Int {
        return prefs.getInt(KEY_WEEKLY_READING_GOAL, 50)
    }
    
    fun recordPagesRead(pages: Int) {
        val today = getTodayString()
        val stats = getDailyStats(today) ?: DailyStats(today)
        val updatedStats = stats.copy(pagesRead = stats.pagesRead + pages)
        saveDailyStats(updatedStats)
        
        // Check reading goal achievements
        if (updatedStats.pagesRead >= getDailyReadingGoal()) {
            unlockAchievement(Achievement.DAILY_READER)
        }
    }
    
    fun getTodayPagesRead(): Int {
        return getDailyStats(getTodayString())?.pagesRead ?: 0
    }
    
    fun getWeeklyPagesRead(): Int {
        return getWeeklyStats().sumOf { it.pagesRead }
    }

    // ================== Productivity Stats ==================
    
    fun recordDocumentOpened(documentType: String) {
        val today = getTodayString()
        val stats = getDailyStats(today) ?: DailyStats(today)
        val typeCounts = stats.documentTypesCounts.toMutableMap()
        typeCounts[documentType] = (typeCounts[documentType] ?: 0) + 1
        val updatedStats = stats.copy(
            documentsOpened = stats.documentsOpened + 1,
            documentTypesCounts = typeCounts
        )
        saveDailyStats(updatedStats)
        
        // Check document opening achievements
        val totalDocuments = getAllStats().sumOf { it.documentsOpened }
        checkDocumentAchievements(totalDocuments)
    }
    
    fun recordDocumentEdited() {
        val today = getTodayString()
        val stats = getDailyStats(today) ?: DailyStats(today)
        val updatedStats = stats.copy(documentsEdited = stats.documentsEdited + 1)
        saveDailyStats(updatedStats)
    }
    
    fun recordDocumentCreated() {
        val today = getTodayString()
        val stats = getDailyStats(today) ?: DailyStats(today)
        val updatedStats = stats.copy(documentsCreated = stats.documentsCreated + 1)
        saveDailyStats(updatedStats)
        
        // Check creation achievements
        val totalCreated = getAllStats().sumOf { it.documentsCreated }
        if (totalCreated >= 1) unlockAchievement(Achievement.FIRST_DOCUMENT)
        if (totalCreated >= 10) unlockAchievement(Achievement.PROLIFIC_CREATOR)
    }
    
    fun recordScanCompleted() {
        val today = getTodayString()
        val stats = getDailyStats(today) ?: DailyStats(today)
        val updatedStats = stats.copy(scansCompleted = stats.scansCompleted + 1)
        saveDailyStats(updatedStats)
        
        // Check scanning achievements
        val totalScans = getAllStats().sumOf { it.scansCompleted }
        if (totalScans >= 1) unlockAchievement(Achievement.FIRST_SCAN)
        if (totalScans >= 50) unlockAchievement(Achievement.SCAN_MASTER)
    }
    
    fun recordConversionCompleted() {
        val today = getTodayString()
        val stats = getDailyStats(today) ?: DailyStats(today)
        val updatedStats = stats.copy(conversionsCompleted = stats.conversionsCompleted + 1)
        saveDailyStats(updatedStats)
    }
    
    fun getTodayStats(): DailyStats {
        return getDailyStats(getTodayString()) ?: DailyStats(getTodayString())
    }
    
    fun getWeeklyStats(): List<DailyStats> {
        val calendar = Calendar.getInstance()
        val stats = mutableListOf<DailyStats>()
        for (i in 0..6) {
            val dateString = dateFormat.format(calendar.time)
            getDailyStats(dateString)?.let { stats.add(it) }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return stats
    }
    
    fun getMonthlyStats(): List<DailyStats> {
        val calendar = Calendar.getInstance()
        val stats = mutableListOf<DailyStats>()
        for (i in 0..29) {
            val dateString = dateFormat.format(calendar.time)
            getDailyStats(dateString)?.let { stats.add(it) }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return stats
    }
    
    private fun getAllStats(): List<DailyStats> {
        val json = prefs.getString(KEY_ALL_STATS, null) ?: return emptyList()
        val type = object : TypeToken<Map<String, DailyStats>>() {}.type
        return try {
            val map: Map<String, DailyStats> = gson.fromJson(json, type) ?: emptyMap()
            map.values.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ================== Achievement Badges ==================
    
    fun getUnlockedAchievements(): List<Achievement> {
        val json = prefs.getString(KEY_ACHIEVEMENTS, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            val names: List<String> = gson.fromJson(json, type) ?: emptyList()
            names.mapNotNull { name -> 
                try { Achievement.valueOf(name) } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun unlockAchievement(achievement: Achievement) {
        val unlocked = getUnlockedAchievements().toMutableList()
        if (!unlocked.contains(achievement)) {
            unlocked.add(achievement)
            val names = unlocked.map { it.name }
            prefs.edit().putString(KEY_ACHIEVEMENTS, gson.toJson(names)).apply()
        }
    }
    
    private fun checkStreakAchievements(streak: Int) {
        if (streak >= 3) unlockAchievement(Achievement.STREAK_3_DAYS)
        if (streak >= 7) unlockAchievement(Achievement.STREAK_7_DAYS)
        if (streak >= 30) unlockAchievement(Achievement.STREAK_30_DAYS)
    }
    
    private fun checkDocumentAchievements(totalDocuments: Int) {
        if (totalDocuments >= 10) unlockAchievement(Achievement.DOCUMENT_EXPLORER)
        if (totalDocuments >= 100) unlockAchievement(Achievement.DOCUMENT_MASTER)
    }

    // ================== Document Completion Tracker ==================
    
    fun setDocumentProgress(documentUri: String, progress: Float, totalPages: Int, currentPage: Int) {
        val trackers = getDocumentTrackers().toMutableMap()
        trackers[documentUri] = DocumentProgress(
            uri = documentUri,
            progress = progress,
            totalPages = totalPages,
            currentPage = currentPage,
            lastAccessedAt = System.currentTimeMillis()
        )
        prefs.edit().putString(KEY_DOCUMENT_PROGRESS, gson.toJson(trackers)).apply()
    }
    
    fun getDocumentProgress(documentUri: String): DocumentProgress? {
        return getDocumentTrackers()[documentUri]
    }
    
    fun getAllDocumentProgress(): List<DocumentProgress> {
        return getDocumentTrackers().values.toList()
    }
    
    private fun getDocumentTrackers(): Map<String, DocumentProgress> {
        val json = prefs.getString(KEY_DOCUMENT_PROGRESS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, DocumentProgress>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ================== Focus Timer (Pomodoro) ==================
    
    fun setFocusSessionDuration(minutes: Int) {
        prefs.edit().putInt(KEY_FOCUS_DURATION, minutes).apply()
    }
    
    fun getFocusSessionDuration(): Int {
        return prefs.getInt(KEY_FOCUS_DURATION, 25) // Default: 25 minutes
    }
    
    fun setBreakDuration(minutes: Int) {
        prefs.edit().putInt(KEY_BREAK_DURATION, minutes).apply()
    }
    
    fun getBreakDuration(): Int {
        return prefs.getInt(KEY_BREAK_DURATION, 5) // Default: 5 minutes
    }
    
    fun setLongBreakDuration(minutes: Int) {
        prefs.edit().putInt(KEY_LONG_BREAK_DURATION, minutes).apply()
    }
    
    fun getLongBreakDuration(): Int {
        return prefs.getInt(KEY_LONG_BREAK_DURATION, 15) // Default: 15 minutes
    }
    
    fun recordFocusSessionCompleted() {
        val today = getTodayString()
        val stats = getDailyStats(today) ?: DailyStats(today)
        val updatedStats = stats.copy(focusSessionsCompleted = stats.focusSessionsCompleted + 1)
        saveDailyStats(updatedStats)
        
        // Check focus achievements
        val totalSessions = getAllStats().sumOf { it.focusSessionsCompleted }
        if (totalSessions >= 1) unlockAchievement(Achievement.FIRST_FOCUS)
        if (totalSessions >= 10) unlockAchievement(Achievement.FOCUS_MASTER)
    }
    
    fun getTodayFocusSessions(): Int {
        return getDailyStats(getTodayString())?.focusSessionsCompleted ?: 0
    }

    // ================== Study Mode ==================
    
    fun setStudyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STUDY_MODE, enabled).apply()
    }
    
    fun isStudyModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_STUDY_MODE, false)
    }
    
    fun setStudyModeBlockedFeatures(features: Set<String>) {
        prefs.edit().putStringSet(KEY_STUDY_MODE_BLOCKED, features).apply()
    }
    
    fun getStudyModeBlockedFeatures(): Set<String> {
        return prefs.getStringSet(KEY_STUDY_MODE_BLOCKED, setOf("share", "convert")) ?: emptySet()
    }

    // ================== Helper Methods ==================
    
    private fun getDailyStats(dateString: String): DailyStats? {
        val json = prefs.getString(KEY_ALL_STATS, null) ?: return null
        val type = object : TypeToken<Map<String, DailyStats>>() {}.type
        return try {
            val map: Map<String, DailyStats> = gson.fromJson(json, type) ?: emptyMap()
            map[dateString]
        } catch (e: Exception) {
            null
        }
    }
    
    private fun saveDailyStats(stats: DailyStats) {
        val json = prefs.getString(KEY_ALL_STATS, null)
        val type = object : TypeToken<MutableMap<String, DailyStats>>() {}.type
        val map: MutableMap<String, DailyStats> = if (json != null) {
            try { gson.fromJson(json, type) ?: mutableMapOf() } catch (e: Exception) { mutableMapOf() }
        } else {
            mutableMapOf()
        }
        map[stats.date] = stats
        
        // Clean up old entries (keep last 90 days)
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
        val keysToRemove = map.keys.filter { dateString ->
            try {
                val date = dateFormat.parse(dateString)
                date?.time ?: 0 < cutoffTime
            } catch (e: Exception) {
                false
            }
        }
        keysToRemove.forEach { map.remove(it) }
        
        prefs.edit().putString(KEY_ALL_STATS, gson.toJson(map)).apply()
    }
    
    private fun getTodayString(): String {
        return dateFormat.format(Date())
    }
    
    private fun getYesterdayString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(calendar.time)
    }

    companion object {
        private const val PREFS_NAME = "productivity_prefs"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_LAST_WRITING_DAY = "last_writing_day"
        private const val KEY_DAILY_READING_GOAL = "daily_reading_goal"
        private const val KEY_WEEKLY_READING_GOAL = "weekly_reading_goal"
        private const val KEY_ALL_STATS = "all_stats"
        private const val KEY_ACHIEVEMENTS = "achievements"
        private const val KEY_DOCUMENT_PROGRESS = "document_progress"
        private const val KEY_FOCUS_DURATION = "focus_duration"
        private const val KEY_BREAK_DURATION = "break_duration"
        private const val KEY_LONG_BREAK_DURATION = "long_break_duration"
        private const val KEY_STUDY_MODE = "study_mode"
        private const val KEY_STUDY_MODE_BLOCKED = "study_mode_blocked"
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}

/**
 * Daily statistics for tracking productivity
 */
data class DailyStats(
    val date: String,
    val documentsOpened: Int = 0,
    val documentsEdited: Int = 0,
    val documentsCreated: Int = 0,
    val pagesRead: Int = 0,
    val writingWordCount: Int = 0,
    val writingTimeMinutes: Int = 0,
    val hasWrittenToday: Boolean = false,
    val scansCompleted: Int = 0,
    val conversionsCompleted: Int = 0,
    val focusSessionsCompleted: Int = 0,
    val documentTypesCounts: Map<String, Int> = emptyMap()
)

/**
 * Progress tracking for individual documents
 */
data class DocumentProgress(
    val uri: String,
    val progress: Float,
    val totalPages: Int,
    val currentPage: Int,
    val lastAccessedAt: Long
)

/**
 * Achievement badges that can be unlocked
 */
enum class Achievement(val title: String, val description: String, val iconName: String) {
    // Writing & Creation
    FIRST_DOCUMENT("First Steps", "Create your first document", "ic_document"),
    PROLIFIC_CREATOR("Prolific Creator", "Create 10 documents", "ic_document"),
    
    // Reading
    DAILY_READER("Daily Reader", "Meet your daily reading goal", "ic_preview"),
    BOOKWORM("Bookworm", "Read 100 pages total", "ic_preview"),
    
    // Streaks
    STREAK_3_DAYS("Getting Started", "3-day writing streak", "ic_star"),
    STREAK_7_DAYS("Consistent Writer", "7-day writing streak", "ic_star"),
    STREAK_30_DAYS("Writing Champion", "30-day writing streak", "ic_star"),
    
    // Documents
    DOCUMENT_EXPLORER("Document Explorer", "Open 10 different documents", "ic_folder"),
    DOCUMENT_MASTER("Document Master", "Open 100 documents", "ic_folder"),
    
    // Scanning
    FIRST_SCAN("Scanner Activated", "Complete your first scan", "ic_scanner"),
    SCAN_MASTER("Scan Master", "Complete 50 scans", "ic_scanner"),
    
    // Focus Mode
    FIRST_FOCUS("Focused Mind", "Complete your first focus session", "ic_edit"),
    FOCUS_MASTER("Focus Master", "Complete 10 focus sessions", "ic_edit")
}
