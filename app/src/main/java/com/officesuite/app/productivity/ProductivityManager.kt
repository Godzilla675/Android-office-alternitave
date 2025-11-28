package com.officesuite.app.productivity

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

/**
 * Productivity Manager for tracking user activity and gamification features.
 * Implements Nice-to-Have Features from Phase 2:
 * - Reading Goals (Section 13)
 * - Writing Streaks (Section 13)
 * - Achievement Badges (Section 13)
 * - Productivity Stats (Section 13)
 * - Document Completion Tracker (Section 13)
 */
object ProductivityManager {
    
    private const val PREFS_NAME = "productivity_prefs"
    private const val KEY_DAILY_READING_GOAL = "daily_reading_goal"
    private const val KEY_WEEKLY_READING_GOAL = "weekly_reading_goal"
    private const val KEY_DAILY_WRITING_GOAL = "daily_writing_goal"
    private const val KEY_READING_STATS = "reading_stats"
    private const val KEY_WRITING_STATS = "writing_stats"
    private const val KEY_ACHIEVEMENTS = "achievements"
    private const val KEY_WRITING_STREAK = "writing_streak"
    private const val KEY_LAST_WRITING_DATE = "last_writing_date"
    private const val KEY_DOCUMENT_PROGRESS = "document_progress"
    private const val KEY_TOTAL_DOCUMENTS_VIEWED = "total_documents_viewed"
    private const val KEY_TOTAL_DOCUMENTS_EDITED = "total_documents_edited"
    private const val KEY_TOTAL_READING_TIME = "total_reading_time"
    private const val KEY_TOTAL_WRITING_TIME = "total_writing_time"
    private const val KEY_STUDY_MODE_ENABLED = "study_mode_enabled"
    private const val KEY_FOCUS_MODE_ENABLED = "focus_mode_enabled"
    
    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("ProductivityManager not initialized. Call init() first.")
    }
    
    // ================== Reading Goals ==================
    
    /**
     * Sets the daily reading goal in minutes.
     */
    fun setDailyReadingGoal(minutes: Int) {
        getPrefs().edit().putInt(KEY_DAILY_READING_GOAL, minutes).apply()
    }
    
    /**
     * Gets the daily reading goal in minutes.
     */
    fun getDailyReadingGoal(): Int {
        return getPrefs().getInt(KEY_DAILY_READING_GOAL, 30) // Default 30 minutes
    }
    
    /**
     * Sets the weekly reading goal in minutes.
     */
    fun setWeeklyReadingGoal(minutes: Int) {
        getPrefs().edit().putInt(KEY_WEEKLY_READING_GOAL, minutes).apply()
    }
    
    /**
     * Gets the weekly reading goal in minutes.
     */
    fun getWeeklyReadingGoal(): Int {
        return getPrefs().getInt(KEY_WEEKLY_READING_GOAL, 210) // Default 3.5 hours/week
    }
    
    /**
     * Records reading time for today.
     */
    fun recordReadingTime(minutes: Int) {
        val today = LocalDate.now().toString()
        val stats = getReadingStats().toMutableMap()
        val currentMinutes = stats[today] ?: 0
        stats[today] = currentMinutes + minutes
        saveReadingStats(stats)
        
        // Update total reading time
        val totalTime = getPrefs().getLong(KEY_TOTAL_READING_TIME, 0L)
        getPrefs().edit().putLong(KEY_TOTAL_READING_TIME, totalTime + minutes).apply()
        
        // Check for achievement unlocks
        checkReadingAchievements()
    }
    
    /**
     * Gets reading stats as a map of date to minutes.
     */
    fun getReadingStats(): Map<String, Int> {
        val json = getPrefs().getString(KEY_READING_STATS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun saveReadingStats(stats: Map<String, Int>) {
        getPrefs().edit().putString(KEY_READING_STATS, gson.toJson(stats)).apply()
    }
    
    /**
     * Gets today's reading progress as a percentage of the goal.
     */
    fun getTodayReadingProgress(): Float {
        val today = LocalDate.now().toString()
        val todayMinutes = getReadingStats()[today] ?: 0
        val goal = getDailyReadingGoal()
        return if (goal > 0) (todayMinutes.toFloat() / goal * 100).coerceAtMost(100f) else 0f
    }
    
    /**
     * Gets this week's reading progress as a percentage of the goal.
     */
    fun getWeeklyReadingProgress(): Float {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val stats = getReadingStats()
        
        var weekMinutes = 0
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong()).toString()
            weekMinutes += stats[date] ?: 0
        }
        
        val goal = getWeeklyReadingGoal()
        return if (goal > 0) (weekMinutes.toFloat() / goal * 100).coerceAtMost(100f) else 0f
    }
    
    // ================== Writing Goals & Streaks ==================
    
    /**
     * Sets the daily writing goal in words.
     */
    fun setDailyWritingGoal(words: Int) {
        getPrefs().edit().putInt(KEY_DAILY_WRITING_GOAL, words).apply()
    }
    
    /**
     * Gets the daily writing goal in words.
     */
    fun getDailyWritingGoal(): Int {
        return getPrefs().getInt(KEY_DAILY_WRITING_GOAL, 500) // Default 500 words
    }
    
    /**
     * Records words written today.
     */
    fun recordWordsWritten(words: Int) {
        val today = LocalDate.now().toString()
        val stats = getWritingStats().toMutableMap()
        val currentWords = stats[today] ?: 0
        stats[today] = currentWords + words
        saveWritingStats(stats)
        
        // Update streak
        updateWritingStreak()
        
        // Check for achievement unlocks
        checkWritingAchievements()
    }
    
    /**
     * Gets writing stats as a map of date to words.
     */
    fun getWritingStats(): Map<String, Int> {
        val json = getPrefs().getString(KEY_WRITING_STATS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun saveWritingStats(stats: Map<String, Int>) {
        getPrefs().edit().putString(KEY_WRITING_STATS, gson.toJson(stats)).apply()
    }
    
    /**
     * Gets today's writing progress as a percentage of the goal.
     */
    fun getTodayWritingProgress(): Float {
        val today = LocalDate.now().toString()
        val todayWords = getWritingStats()[today] ?: 0
        val goal = getDailyWritingGoal()
        return if (goal > 0) (todayWords.toFloat() / goal * 100).coerceAtMost(100f) else 0f
    }
    
    /**
     * Gets the current writing streak in days.
     */
    fun getWritingStreak(): Int {
        return getPrefs().getInt(KEY_WRITING_STREAK, 0)
    }
    
    private fun updateWritingStreak() {
        val today = LocalDate.now().toString()
        val lastWritingDate = getPrefs().getString(KEY_LAST_WRITING_DATE, null)
        val yesterday = LocalDate.now().minusDays(1).toString()
        
        val currentStreak = getPrefs().getInt(KEY_WRITING_STREAK, 0)
        
        val newStreak = when {
            lastWritingDate == today -> currentStreak // Already wrote today
            lastWritingDate == yesterday -> currentStreak + 1 // Consecutive day
            else -> 1 // Start new streak
        }
        
        getPrefs().edit()
            .putInt(KEY_WRITING_STREAK, newStreak)
            .putString(KEY_LAST_WRITING_DATE, today)
            .apply()
    }
    
    // ================== Achievement Badges ==================
    
    /**
     * Available achievement types.
     */
    enum class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val iconRes: String
    ) {
        FIRST_DOCUMENT("first_doc", "First Steps", "View your first document", "ic_achievement_first"),
        READER_NOVICE("reader_novice", "Bookworm Beginner", "Read for 1 hour total", "ic_achievement_reader"),
        READER_INTERMEDIATE("reader_intermediate", "Avid Reader", "Read for 10 hours total", "ic_achievement_reader_gold"),
        READER_EXPERT("reader_expert", "Reading Master", "Read for 100 hours total", "ic_achievement_reader_diamond"),
        WRITER_NOVICE("writer_novice", "Wordsmith Beginner", "Write 1,000 words", "ic_achievement_writer"),
        WRITER_INTERMEDIATE("writer_intermediate", "Prolific Writer", "Write 10,000 words", "ic_achievement_writer_gold"),
        WRITER_EXPERT("writer_expert", "Writing Master", "Write 100,000 words", "ic_achievement_writer_diamond"),
        STREAK_3("streak_3", "Getting Started", "3-day writing streak", "ic_achievement_streak"),
        STREAK_7("streak_7", "Consistent Writer", "7-day writing streak", "ic_achievement_streak_gold"),
        STREAK_30("streak_30", "Writing Machine", "30-day writing streak", "ic_achievement_streak_diamond"),
        EDITOR_NOVICE("editor_novice", "Editor Beginner", "Edit 5 documents", "ic_achievement_editor"),
        EDITOR_EXPERT("editor_expert", "Editing Pro", "Edit 50 documents", "ic_achievement_editor_gold"),
        SCANNER_NOVICE("scanner_novice", "Scanner Starter", "Scan 10 documents", "ic_achievement_scanner"),
        CONVERTER_NOVICE("converter_novice", "Format Wizard", "Convert 10 documents", "ic_achievement_converter"),
        DAILY_GOAL("daily_goal", "Goal Crusher", "Meet daily goal 7 times", "ic_achievement_goal"),
        FOCUS_MASTER("focus_master", "Focus Master", "Complete 10 focus sessions", "ic_achievement_focus")
    }
    
    /**
     * Gets all unlocked achievements.
     */
    fun getUnlockedAchievements(): Set<String> {
        val json = getPrefs().getString(KEY_ACHIEVEMENTS, null) ?: return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    /**
     * Unlocks an achievement.
     * @return true if newly unlocked, false if already unlocked
     */
    fun unlockAchievement(achievement: Achievement): Boolean {
        val achievements = getUnlockedAchievements().toMutableSet()
        if (achievements.contains(achievement.id)) return false
        
        achievements.add(achievement.id)
        getPrefs().edit().putString(KEY_ACHIEVEMENTS, gson.toJson(achievements)).apply()
        return true
    }
    
    /**
     * Checks if an achievement is unlocked.
     */
    fun isAchievementUnlocked(achievement: Achievement): Boolean {
        return getUnlockedAchievements().contains(achievement.id)
    }
    
    private fun checkReadingAchievements() {
        val totalMinutes = getPrefs().getLong(KEY_TOTAL_READING_TIME, 0L)
        
        when {
            totalMinutes >= 6000 -> unlockAchievement(Achievement.READER_EXPERT) // 100 hours
            totalMinutes >= 600 -> unlockAchievement(Achievement.READER_INTERMEDIATE) // 10 hours
            totalMinutes >= 60 -> unlockAchievement(Achievement.READER_NOVICE) // 1 hour
        }
    }
    
    private fun checkWritingAchievements() {
        val stats = getWritingStats()
        val totalWords = stats.values.sum()
        
        when {
            totalWords >= 100000 -> unlockAchievement(Achievement.WRITER_EXPERT)
            totalWords >= 10000 -> unlockAchievement(Achievement.WRITER_INTERMEDIATE)
            totalWords >= 1000 -> unlockAchievement(Achievement.WRITER_NOVICE)
        }
        
        // Check streak achievements
        val streak = getWritingStreak()
        when {
            streak >= 30 -> unlockAchievement(Achievement.STREAK_30)
            streak >= 7 -> unlockAchievement(Achievement.STREAK_7)
            streak >= 3 -> unlockAchievement(Achievement.STREAK_3)
        }
    }
    
    // ================== Document Progress Tracking ==================
    
    /**
     * Document progress data class.
     */
    data class DocumentProgress(
        val documentUri: String,
        val documentName: String,
        val totalPages: Int,
        val currentPage: Int,
        val lastAccessed: Long,
        val completionPercentage: Float
    )
    
    /**
     * Updates progress for a document.
     */
    fun updateDocumentProgress(uri: String, name: String, currentPage: Int, totalPages: Int) {
        val progressMap = getDocumentProgressMap().toMutableMap()
        val completionPercentage = if (totalPages > 0) (currentPage.toFloat() / totalPages * 100) else 0f
        
        progressMap[uri] = DocumentProgress(
            documentUri = uri,
            documentName = name,
            totalPages = totalPages,
            currentPage = currentPage,
            lastAccessed = System.currentTimeMillis(),
            completionPercentage = completionPercentage
        )
        
        getPrefs().edit().putString(KEY_DOCUMENT_PROGRESS, gson.toJson(progressMap)).apply()
    }
    
    /**
     * Gets progress for a specific document.
     */
    fun getDocumentProgress(uri: String): DocumentProgress? {
        return getDocumentProgressMap()[uri]
    }
    
    /**
     * Gets all document progress entries.
     */
    fun getAllDocumentProgress(): List<DocumentProgress> {
        return getDocumentProgressMap().values.sortedByDescending { it.lastAccessed }
    }
    
    private fun getDocumentProgressMap(): Map<String, DocumentProgress> {
        val json = getPrefs().getString(KEY_DOCUMENT_PROGRESS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, DocumentProgress>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    // ================== Productivity Stats ==================
    
    /**
     * Increments document view count.
     */
    fun recordDocumentViewed() {
        val current = getPrefs().getLong(KEY_TOTAL_DOCUMENTS_VIEWED, 0L)
        getPrefs().edit().putLong(KEY_TOTAL_DOCUMENTS_VIEWED, current + 1).apply()
        
        if (current == 0L) {
            unlockAchievement(Achievement.FIRST_DOCUMENT)
        }
    }
    
    /**
     * Increments document edit count.
     */
    fun recordDocumentEdited() {
        val current = getPrefs().getLong(KEY_TOTAL_DOCUMENTS_EDITED, 0L)
        getPrefs().edit().putLong(KEY_TOTAL_DOCUMENTS_EDITED, current + 1).apply()
        
        when (current + 1) {
            5L -> unlockAchievement(Achievement.EDITOR_NOVICE)
            50L -> unlockAchievement(Achievement.EDITOR_EXPERT)
        }
    }
    
    /**
     * Gets total documents viewed.
     */
    fun getTotalDocumentsViewed(): Long {
        return getPrefs().getLong(KEY_TOTAL_DOCUMENTS_VIEWED, 0L)
    }
    
    /**
     * Gets total documents edited.
     */
    fun getTotalDocumentsEdited(): Long {
        return getPrefs().getLong(KEY_TOTAL_DOCUMENTS_EDITED, 0L)
    }
    
    /**
     * Gets total reading time in minutes.
     */
    fun getTotalReadingTime(): Long {
        return getPrefs().getLong(KEY_TOTAL_READING_TIME, 0L)
    }
    
    /**
     * Gets total words written.
     */
    fun getTotalWordsWritten(): Long {
        return getWritingStats().values.sum().toLong()
    }
    
    /**
     * Productivity stats summary.
     */
    data class ProductivityStats(
        val totalDocumentsViewed: Long,
        val totalDocumentsEdited: Long,
        val totalReadingMinutes: Long,
        val totalWordsWritten: Long,
        val currentWritingStreak: Int,
        val achievementsUnlocked: Int,
        val todayReadingProgress: Float,
        val todayWritingProgress: Float,
        val weeklyReadingProgress: Float
    )
    
    /**
     * Gets a summary of productivity stats.
     */
    fun getProductivityStats(): ProductivityStats {
        return ProductivityStats(
            totalDocumentsViewed = getTotalDocumentsViewed(),
            totalDocumentsEdited = getTotalDocumentsEdited(),
            totalReadingMinutes = getTotalReadingTime(),
            totalWordsWritten = getTotalWordsWritten(),
            currentWritingStreak = getWritingStreak(),
            achievementsUnlocked = getUnlockedAchievements().size,
            todayReadingProgress = getTodayReadingProgress(),
            todayWritingProgress = getTodayWritingProgress(),
            weeklyReadingProgress = getWeeklyReadingProgress()
        )
    }
    
    // ================== Study Mode ==================
    
    /**
     * Enables/disables study mode (blocks distracting features).
     */
    fun setStudyModeEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_STUDY_MODE_ENABLED, enabled).apply()
    }
    
    /**
     * Checks if study mode is enabled.
     */
    fun isStudyModeEnabled(): Boolean {
        return getPrefs().getBoolean(KEY_STUDY_MODE_ENABLED, false)
    }
    
    // ================== Focus Mode ==================
    
    /**
     * Enables/disables focus mode (distraction-free editing).
     */
    fun setFocusModeEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_FOCUS_MODE_ENABLED, enabled).apply()
    }
    
    /**
     * Checks if focus mode is enabled.
     */
    fun isFocusModeEnabled(): Boolean {
        return getPrefs().getBoolean(KEY_FOCUS_MODE_ENABLED, false)
    }
    
    /**
     * Resets all productivity data.
     */
    fun resetAllData() {
        getPrefs().edit().clear().apply()
    }
}
