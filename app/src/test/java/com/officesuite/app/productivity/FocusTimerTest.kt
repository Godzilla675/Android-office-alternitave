package com.officesuite.app.productivity

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FocusTimer class.
 */
class FocusTimerTest {
    
    @Test
    fun `default focus duration is 25 minutes`() {
        assertEquals(25, FocusTimer.DEFAULT_FOCUS_DURATION)
    }
    
    @Test
    fun `default short break duration is 5 minutes`() {
        assertEquals(5, FocusTimer.DEFAULT_SHORT_BREAK_DURATION)
    }
    
    @Test
    fun `default long break duration is 15 minutes`() {
        assertEquals(15, FocusTimer.DEFAULT_LONG_BREAK_DURATION)
    }
    
    @Test
    fun `default sessions before long break is 4`() {
        assertEquals(4, FocusTimer.DEFAULT_SESSIONS_BEFORE_LONG_BREAK)
    }
    
    @Test
    fun `TimerState enum has correct values`() {
        val states = FocusTimer.TimerState.entries
        assertEquals(4, states.size)
        assertTrue(states.contains(FocusTimer.TimerState.IDLE))
        assertTrue(states.contains(FocusTimer.TimerState.RUNNING))
        assertTrue(states.contains(FocusTimer.TimerState.PAUSED))
        assertTrue(states.contains(FocusTimer.TimerState.COMPLETED))
    }
    
    @Test
    fun `SessionType enum has correct values`() {
        val types = FocusTimer.SessionType.entries
        assertEquals(3, types.size)
        assertTrue(types.contains(FocusTimer.SessionType.FOCUS))
        assertTrue(types.contains(FocusTimer.SessionType.SHORT_BREAK))
        assertTrue(types.contains(FocusTimer.SessionType.LONG_BREAK))
    }
    
    @Test
    fun `SessionEntry data class holds values correctly`() {
        val entry = FocusTimer.SessionEntry(
            date = "2024-01-01",
            type = "FOCUS",
            durationMinutes = 25,
            completedAt = 1704067200000
        )
        
        assertEquals("2024-01-01", entry.date)
        assertEquals("FOCUS", entry.type)
        assertEquals(25, entry.durationMinutes)
        assertEquals(1704067200000, entry.completedAt)
    }
    
    @Test
    fun `TimerStatus data class holds values correctly`() {
        val status = FocusTimer.TimerStatus(
            state = FocusTimer.TimerState.RUNNING,
            sessionType = FocusTimer.SessionType.FOCUS,
            remainingTimeMillis = 600000,
            totalTimeMillis = 1500000,
            progressPercent = 60f,
            sessionsCompleted = 3,
            sessionsUntilLongBreak = 1
        )
        
        assertEquals(FocusTimer.TimerState.RUNNING, status.state)
        assertEquals(FocusTimer.SessionType.FOCUS, status.sessionType)
        assertEquals(600000, status.remainingTimeMillis)
        assertEquals(1500000, status.totalTimeMillis)
        assertEquals(60f, status.progressPercent, 0.01f)
        assertEquals(3, status.sessionsCompleted)
        assertEquals(1, status.sessionsUntilLongBreak)
    }
}
