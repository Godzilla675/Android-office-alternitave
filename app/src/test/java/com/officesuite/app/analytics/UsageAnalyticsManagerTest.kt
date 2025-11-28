package com.officesuite.app.analytics

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for UsageAnalyticsManager data classes.
 */
class UsageAnalyticsManagerTest {

    @Test
    fun `AnalyticsPeriod enum has expected values`() {
        val periods = AnalyticsPeriod.values()
        
        assertEquals(5, periods.size)
        assertTrue(periods.contains(AnalyticsPeriod.TODAY))
        assertTrue(periods.contains(AnalyticsPeriod.LAST_7_DAYS))
        assertTrue(periods.contains(AnalyticsPeriod.LAST_30_DAYS))
        assertTrue(periods.contains(AnalyticsPeriod.LAST_90_DAYS))
        assertTrue(periods.contains(AnalyticsPeriod.ALL_TIME))
    }

    @Test
    fun `AnalyticsPeriod has correct display names`() {
        assertEquals("Today", AnalyticsPeriod.TODAY.displayName)
        assertEquals("Last 7 Days", AnalyticsPeriod.LAST_7_DAYS.displayName)
        assertEquals("Last 30 Days", AnalyticsPeriod.LAST_30_DAYS.displayName)
        assertEquals("Last 90 Days", AnalyticsPeriod.LAST_90_DAYS.displayName)
        assertEquals("All Time", AnalyticsPeriod.ALL_TIME.displayName)
    }

    @Test
    fun `AnalyticsSummary data class holds values correctly`() {
        val summary = AnalyticsSummary(
            period = AnalyticsPeriod.LAST_7_DAYS,
            documentsViewed = 100,
            documentsEdited = 50,
            documentsCreated = 25,
            wordsWritten = 5000,
            scans = 10,
            conversions = 5,
            totalSessionsCount = 20,
            totalSessionDurationMs = 3600000, // 1 hour
            readingTimeMs = 1800000, // 30 min
            topFeatures = emptyList(),
            activeDays = 7
        )
        
        assertEquals(AnalyticsPeriod.LAST_7_DAYS, summary.period)
        assertEquals(100L, summary.documentsViewed)
        assertEquals(50L, summary.documentsEdited)
        assertEquals(25L, summary.documentsCreated)
        assertEquals(5000L, summary.wordsWritten)
        assertEquals(10L, summary.scans)
        assertEquals(5L, summary.conversions)
        assertEquals(20L, summary.totalSessionsCount)
        assertEquals(7, summary.activeDays)
    }

    @Test
    fun `AnalyticsSummary calculates averageSessionDurationMs correctly`() {
        val summary = AnalyticsSummary(
            totalSessionsCount = 10,
            totalSessionDurationMs = 600000 // 10 minutes
        )
        
        assertEquals(60000L, summary.averageSessionDurationMs) // 1 minute per session
    }

    @Test
    fun `AnalyticsSummary averageSessionDurationMs returns zero when no sessions`() {
        val summary = AnalyticsSummary(
            totalSessionsCount = 0,
            totalSessionDurationMs = 0
        )
        
        assertEquals(0L, summary.averageSessionDurationMs)
    }

    @Test
    fun `AnalyticsSummary calculates readingTimeMinutes correctly`() {
        val summary = AnalyticsSummary(
            readingTimeMs = 1800000 // 30 minutes
        )
        
        assertEquals(30L, summary.readingTimeMinutes)
    }

    @Test
    fun `AnalyticsSummary calculates sessionDurationMinutes correctly`() {
        val summary = AnalyticsSummary(
            totalSessionDurationMs = 3600000 // 60 minutes
        )
        
        assertEquals(60L, summary.sessionDurationMinutes)
    }

    @Test
    fun `FeatureUsageStat data class holds values correctly`() {
        val stat = FeatureUsageStat(
            featureName = "scanner",
            usageCount = 42
        )
        
        assertEquals("scanner", stat.featureName)
        assertEquals(42, stat.usageCount)
    }

    @Test
    fun `DailyStats data class holds values correctly`() {
        val stats = DailyStats(
            date = "2024-01-15",
            stats = mutableMapOf(
                "documents_viewed" to 10L,
                "words_written" to 500L
            )
        )
        
        assertEquals("2024-01-15", stats.date)
        assertEquals(10L, stats.stats["documents_viewed"])
        assertEquals(500L, stats.stats["words_written"])
    }

    @Test
    fun `SessionData data class holds values correctly`() {
        val session = SessionData(
            id = "session-123",
            startTime = 1234567890L,
            documentViews = 5,
            documentsEdited = 2,
            wordsWritten = 100,
            readingTimeMs = 60000
        )
        
        assertEquals("session-123", session.id)
        assertEquals(1234567890L, session.startTime)
        assertEquals(5, session.documentViews)
        assertEquals(2, session.documentsEdited)
        assertEquals(100, session.wordsWritten)
        assertEquals(60000L, session.readingTimeMs)
    }

    @Test
    fun `SessionData document types set is mutable`() {
        val session = SessionData(
            id = "test",
            startTime = System.currentTimeMillis()
        )
        
        session.documentTypes.add("PDF")
        session.documentTypes.add("DOCX")
        
        assertEquals(2, session.documentTypes.size)
        assertTrue(session.documentTypes.contains("PDF"))
        assertTrue(session.documentTypes.contains("DOCX"))
    }

    @Test
    fun `AnalyticsExport data class holds values correctly`() {
        val summary = AnalyticsSummary()
        val export = AnalyticsExport(
            exportDate = 1234567890L,
            summary = summary,
            dailyStats = emptyList(),
            featureUsage = mapOf("scanner" to 10)
        )
        
        assertEquals(1234567890L, export.exportDate)
        assertEquals(summary, export.summary)
        assertTrue(export.dailyStats.isEmpty())
        assertEquals(10, export.featureUsage["scanner"])
    }
}
