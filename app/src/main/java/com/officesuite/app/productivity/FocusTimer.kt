package com.officesuite.app.productivity

import android.content.Context
import android.content.SharedPreferences
import android.os.CountDownTimer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

/**
 * Focus Timer implementing the Pomodoro Technique.
 * Implements Nice-to-Have Feature: Focus Timer (Pomodoro) from Section 13.
 * 
 * Standard Pomodoro:
 * - 25 minutes focus session
 * - 5 minutes short break
 * - 15 minutes long break (after 4 sessions)
 */
class FocusTimer(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "focus_timer_prefs"
        private const val KEY_FOCUS_DURATION = "focus_duration"
        private const val KEY_SHORT_BREAK_DURATION = "short_break_duration"
        private const val KEY_LONG_BREAK_DURATION = "long_break_duration"
        private const val KEY_SESSIONS_BEFORE_LONG_BREAK = "sessions_before_long_break"
        private const val KEY_COMPLETED_SESSIONS = "completed_sessions"
        private const val KEY_TOTAL_FOCUS_TIME = "total_focus_time"
        private const val KEY_SESSION_HISTORY = "session_history"
        private const val KEY_AUTO_START_BREAK = "auto_start_break"
        private const val KEY_AUTO_START_FOCUS = "auto_start_focus"
        private const val KEY_DAILY_SESSIONS = "daily_sessions"
        
        // Default durations in minutes
        const val DEFAULT_FOCUS_DURATION = 25
        const val DEFAULT_SHORT_BREAK_DURATION = 5
        const val DEFAULT_LONG_BREAK_DURATION = 15
        const val DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var countDownTimer: CountDownTimer? = null
    private var currentState: TimerState = TimerState.IDLE
    private var remainingTimeMillis: Long = 0
    private var sessionType: SessionType = SessionType.FOCUS
    private var sessionsCompletedInCycle = 0
    
    private var listener: FocusTimerListener? = null
    
    /**
     * Timer states.
     */
    enum class TimerState {
        IDLE,
        RUNNING,
        PAUSED,
        COMPLETED
    }
    
    /**
     * Session types.
     */
    enum class SessionType {
        FOCUS,
        SHORT_BREAK,
        LONG_BREAK
    }
    
    /**
     * Session history entry.
     */
    data class SessionEntry(
        val date: String,
        val type: String,
        val durationMinutes: Int,
        val completedAt: Long
    )
    
    /**
     * Timer status data class.
     */
    data class TimerStatus(
        val state: TimerState,
        val sessionType: SessionType,
        val remainingTimeMillis: Long,
        val totalTimeMillis: Long,
        val progressPercent: Float,
        val sessionsCompleted: Int,
        val sessionsUntilLongBreak: Int
    )
    
    /**
     * Listener interface for timer events.
     */
    interface FocusTimerListener {
        fun onTick(remainingMillis: Long, progressPercent: Float)
        fun onSessionComplete(sessionType: SessionType)
        fun onStateChanged(state: TimerState)
    }
    
    /**
     * Sets the timer event listener.
     */
    fun setListener(listener: FocusTimerListener?) {
        this.listener = listener
    }
    
    // ================== Configuration ==================
    
    /**
     * Gets focus duration in minutes.
     */
    fun getFocusDuration(): Int {
        return prefs.getInt(KEY_FOCUS_DURATION, DEFAULT_FOCUS_DURATION)
    }
    
    /**
     * Sets focus duration in minutes.
     */
    fun setFocusDuration(minutes: Int) {
        prefs.edit().putInt(KEY_FOCUS_DURATION, minutes.coerceIn(1, 120)).apply()
    }
    
    /**
     * Gets short break duration in minutes.
     */
    fun getShortBreakDuration(): Int {
        return prefs.getInt(KEY_SHORT_BREAK_DURATION, DEFAULT_SHORT_BREAK_DURATION)
    }
    
    /**
     * Sets short break duration in minutes.
     */
    fun setShortBreakDuration(minutes: Int) {
        prefs.edit().putInt(KEY_SHORT_BREAK_DURATION, minutes.coerceIn(1, 60)).apply()
    }
    
    /**
     * Gets long break duration in minutes.
     */
    fun getLongBreakDuration(): Int {
        return prefs.getInt(KEY_LONG_BREAK_DURATION, DEFAULT_LONG_BREAK_DURATION)
    }
    
    /**
     * Sets long break duration in minutes.
     */
    fun setLongBreakDuration(minutes: Int) {
        prefs.edit().putInt(KEY_LONG_BREAK_DURATION, minutes.coerceIn(1, 60)).apply()
    }
    
    /**
     * Gets number of sessions before a long break.
     */
    fun getSessionsBeforeLongBreak(): Int {
        return prefs.getInt(KEY_SESSIONS_BEFORE_LONG_BREAK, DEFAULT_SESSIONS_BEFORE_LONG_BREAK)
    }
    
    /**
     * Sets number of sessions before a long break.
     */
    fun setSessionsBeforeLongBreak(sessions: Int) {
        prefs.edit().putInt(KEY_SESSIONS_BEFORE_LONG_BREAK, sessions.coerceIn(1, 10)).apply()
    }
    
    /**
     * Gets whether breaks auto-start after focus sessions.
     */
    fun isAutoStartBreak(): Boolean {
        return prefs.getBoolean(KEY_AUTO_START_BREAK, true)
    }
    
    /**
     * Sets whether breaks auto-start after focus sessions.
     */
    fun setAutoStartBreak(autoStart: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_BREAK, autoStart).apply()
    }
    
    /**
     * Gets whether focus sessions auto-start after breaks.
     */
    fun isAutoStartFocus(): Boolean {
        return prefs.getBoolean(KEY_AUTO_START_FOCUS, false)
    }
    
    /**
     * Sets whether focus sessions auto-start after breaks.
     */
    fun setAutoStartFocus(autoStart: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_FOCUS, autoStart).apply()
    }
    
    // ================== Timer Control ==================
    
    /**
     * Starts a focus session.
     */
    fun startFocusSession() {
        sessionType = SessionType.FOCUS
        val durationMillis = getFocusDuration() * 60 * 1000L
        startTimer(durationMillis)
    }
    
    /**
     * Starts a short break.
     */
    fun startShortBreak() {
        sessionType = SessionType.SHORT_BREAK
        val durationMillis = getShortBreakDuration() * 60 * 1000L
        startTimer(durationMillis)
    }
    
    /**
     * Starts a long break.
     */
    fun startLongBreak() {
        sessionType = SessionType.LONG_BREAK
        val durationMillis = getLongBreakDuration() * 60 * 1000L
        startTimer(durationMillis)
    }
    
    /**
     * Starts the next session automatically based on completion.
     */
    fun startNextSession() {
        when (sessionType) {
            SessionType.FOCUS -> {
                if (sessionsCompletedInCycle >= getSessionsBeforeLongBreak()) {
                    sessionsCompletedInCycle = 0
                    startLongBreak()
                } else {
                    startShortBreak()
                }
            }
            SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> {
                startFocusSession()
            }
        }
    }
    
    private fun startTimer(durationMillis: Long) {
        cancelTimer()
        remainingTimeMillis = durationMillis
        
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                val progress = 1f - (millisUntilFinished.toFloat() / durationMillis)
                listener?.onTick(millisUntilFinished, progress * 100)
            }
            
            override fun onFinish() {
                remainingTimeMillis = 0
                onSessionCompleted()
            }
        }.start()
        
        currentState = TimerState.RUNNING
        listener?.onStateChanged(currentState)
    }
    
    /**
     * Pauses the current timer.
     */
    fun pause() {
        if (currentState == TimerState.RUNNING) {
            countDownTimer?.cancel()
            currentState = TimerState.PAUSED
            listener?.onStateChanged(currentState)
        }
    }
    
    /**
     * Resumes a paused timer.
     */
    fun resume() {
        if (currentState == TimerState.PAUSED && remainingTimeMillis > 0) {
            startTimer(remainingTimeMillis)
        }
    }
    
    /**
     * Stops and resets the timer.
     */
    fun stop() {
        cancelTimer()
        currentState = TimerState.IDLE
        remainingTimeMillis = 0
        listener?.onStateChanged(currentState)
    }
    
    /**
     * Skips the current session and moves to the next.
     */
    fun skip() {
        cancelTimer()
        startNextSession()
    }
    
    private fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }
    
    private fun onSessionCompleted() {
        currentState = TimerState.COMPLETED
        
        // Record the session
        if (sessionType == SessionType.FOCUS) {
            sessionsCompletedInCycle++
            recordCompletedSession()
        }
        
        listener?.onSessionComplete(sessionType)
        listener?.onStateChanged(currentState)
        
        // Auto-start next session if enabled
        if (sessionType == SessionType.FOCUS && isAutoStartBreak()) {
            startNextSession()
        } else if (sessionType != SessionType.FOCUS && isAutoStartFocus()) {
            startNextSession()
        }
    }
    
    private fun recordCompletedSession() {
        val durationMinutes = getFocusDuration()
        
        // Update total completed sessions
        val totalSessions = prefs.getLong(KEY_COMPLETED_SESSIONS, 0L) + 1
        prefs.edit().putLong(KEY_COMPLETED_SESSIONS, totalSessions).apply()
        
        // Update total focus time
        val totalFocusTime = prefs.getLong(KEY_TOTAL_FOCUS_TIME, 0L) + durationMinutes
        prefs.edit().putLong(KEY_TOTAL_FOCUS_TIME, totalFocusTime).apply()
        
        // Record in history
        val history = getSessionHistory().toMutableList()
        history.add(
            SessionEntry(
                date = LocalDate.now().toString(),
                type = SessionType.FOCUS.name,
                durationMinutes = durationMinutes,
                completedAt = System.currentTimeMillis()
            )
        )
        // Keep last 100 sessions
        val trimmedHistory = history.takeLast(100)
        prefs.edit().putString(KEY_SESSION_HISTORY, gson.toJson(trimmedHistory)).apply()
        
        // Update daily sessions count
        updateDailySessions()
        
        // Check for achievements
        checkFocusAchievements(totalSessions)
    }
    
    private fun updateDailySessions() {
        val today = LocalDate.now().toString()
        val dailySessionsJson = prefs.getString(KEY_DAILY_SESSIONS, null)
        val dailySessions: MutableMap<String, Int> = if (dailySessionsJson != null) {
            val type = object : TypeToken<MutableMap<String, Int>>() {}.type
            try {
                gson.fromJson(dailySessionsJson, type) ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
        
        dailySessions[today] = (dailySessions[today] ?: 0) + 1
        prefs.edit().putString(KEY_DAILY_SESSIONS, gson.toJson(dailySessions)).apply()
    }
    
    private fun checkFocusAchievements(totalSessions: Long) {
        if (totalSessions >= 10) {
            ProductivityManager.unlockAchievement(ProductivityManager.Achievement.FOCUS_MASTER)
        }
    }
    
    // ================== Statistics ==================
    
    /**
     * Gets total completed focus sessions.
     */
    fun getTotalCompletedSessions(): Long {
        return prefs.getLong(KEY_COMPLETED_SESSIONS, 0L)
    }
    
    /**
     * Gets total focus time in minutes.
     */
    fun getTotalFocusTime(): Long {
        return prefs.getLong(KEY_TOTAL_FOCUS_TIME, 0L)
    }
    
    /**
     * Gets session history.
     */
    fun getSessionHistory(): List<SessionEntry> {
        val json = prefs.getString(KEY_SESSION_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<SessionEntry>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Gets today's completed sessions count.
     */
    fun getTodaySessions(): Int {
        val today = LocalDate.now().toString()
        val dailySessionsJson = prefs.getString(KEY_DAILY_SESSIONS, null) ?: return 0
        val type = object : TypeToken<Map<String, Int>>() {}.type
        val dailySessions: Map<String, Int> = try {
            gson.fromJson(dailySessionsJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        return dailySessions[today] ?: 0
    }
    
    /**
     * Gets the current timer status.
     */
    fun getStatus(): TimerStatus {
        val totalTimeMillis = when (sessionType) {
            SessionType.FOCUS -> getFocusDuration() * 60 * 1000L
            SessionType.SHORT_BREAK -> getShortBreakDuration() * 60 * 1000L
            SessionType.LONG_BREAK -> getLongBreakDuration() * 60 * 1000L
        }
        
        val progress = if (totalTimeMillis > 0) {
            ((totalTimeMillis - remainingTimeMillis).toFloat() / totalTimeMillis * 100)
        } else {
            0f
        }
        
        return TimerStatus(
            state = currentState,
            sessionType = sessionType,
            remainingTimeMillis = remainingTimeMillis,
            totalTimeMillis = totalTimeMillis,
            progressPercent = progress,
            sessionsCompleted = getTodaySessions(),
            sessionsUntilLongBreak = getSessionsBeforeLongBreak() - sessionsCompletedInCycle
        )
    }
    
    /**
     * Formats remaining time as MM:SS.
     */
    fun formatRemainingTime(): String {
        val totalSeconds = remainingTimeMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Cleans up resources.
     */
    fun destroy() {
        cancelTimer()
        listener = null
    }
    
    /**
     * Resets all focus timer data.
     */
    fun resetAllData() {
        prefs.edit().clear().apply()
        sessionsCompletedInCycle = 0
    }
}
