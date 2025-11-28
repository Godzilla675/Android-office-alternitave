package com.officesuite.app.platform

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DigitalWellbeingManager class.
 */
class DigitalWellbeingManagerTest {
    
    @Test
    fun `UsageState data class holds values correctly`() {
        val state = DigitalWellbeingManager.UsageState(
            todayUsageMinutes = 60,
            dailyLimitMinutes = 120,
            isLimitEnabled = true,
            isLimitReached = false,
            weeklyUsageMinutes = 420,
            averageDailyUsage = 60,
            currentSessionMinutes = 15
        )
        
        assertEquals(60, state.todayUsageMinutes)
        assertEquals(120, state.dailyLimitMinutes)
        assertTrue(state.isLimitEnabled)
        assertFalse(state.isLimitReached)
        assertEquals(420, state.weeklyUsageMinutes)
        assertEquals(60, state.averageDailyUsage)
        assertEquals(15, state.currentSessionMinutes)
    }
    
    @Test
    fun `DailyStats data class holds values correctly`() {
        val stats = DigitalWellbeingManager.DailyStats(
            date = "2024-001",
            usageMinutes = 45,
            documentsViewed = 10,
            documentsEdited = 5,
            pagesRead = 50,
            scansMade = 3
        )
        
        assertEquals("2024-001", stats.date)
        assertEquals(45, stats.usageMinutes)
        assertEquals(10, stats.documentsViewed)
        assertEquals(5, stats.documentsEdited)
        assertEquals(50, stats.pagesRead)
        assertEquals(3, stats.scansMade)
    }
    
    @Test
    fun `WeeklySummary data class holds values correctly`() {
        val summary = DigitalWellbeingManager.WeeklySummary(
            totalMinutes = 420,
            averageMinutesPerDay = 60,
            peakDay = "2024-003",
            peakUsageMinutes = 90,
            documentsViewed = 50,
            documentsEdited = 20,
            mostActiveHour = 14
        )
        
        assertEquals(420, summary.totalMinutes)
        assertEquals(60, summary.averageMinutesPerDay)
        assertEquals("2024-003", summary.peakDay)
        assertEquals(90, summary.peakUsageMinutes)
        assertEquals(50, summary.documentsViewed)
        assertEquals(20, summary.documentsEdited)
        assertEquals(14, summary.mostActiveHour)
    }
    
    @Test
    fun `UsageReminderSettings data class holds values correctly`() {
        val settings = DigitalWellbeingManager.UsageReminderSettings(
            isEnabled = true,
            reminderIntervalMinutes = 30,
            breakReminderEnabled = true,
            breakReminderIntervalMinutes = 60
        )
        
        assertTrue(settings.isEnabled)
        assertEquals(30, settings.reminderIntervalMinutes)
        assertTrue(settings.breakReminderEnabled)
        assertEquals(60, settings.breakReminderIntervalMinutes)
    }
    
    @Test
    fun `UsageState default values are correct`() {
        val defaultState = DigitalWellbeingManager.UsageState()
        
        assertEquals(0, defaultState.todayUsageMinutes)
        assertEquals(0, defaultState.dailyLimitMinutes)
        assertFalse(defaultState.isLimitEnabled)
        assertFalse(defaultState.isLimitReached)
        assertEquals(0, defaultState.weeklyUsageMinutes)
        assertEquals(0, defaultState.averageDailyUsage)
        assertEquals(0, defaultState.currentSessionMinutes)
    }
    
    @Test
    fun `UsageReminderSettings default values are correct`() {
        val defaultSettings = DigitalWellbeingManager.UsageReminderSettings(
            isEnabled = false,
            reminderIntervalMinutes = 30,
            breakReminderEnabled = true,
            breakReminderIntervalMinutes = 60
        )
        
        assertFalse(defaultSettings.isEnabled)
        assertEquals(30, defaultSettings.reminderIntervalMinutes)
        assertTrue(defaultSettings.breakReminderEnabled)
        assertEquals(60, defaultSettings.breakReminderIntervalMinutes)
    }
}
