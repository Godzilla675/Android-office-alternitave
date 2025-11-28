package com.officesuite.app.productivity

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ProductivityManager class.
 */
class ProductivityManagerTest {
    
    @Test
    fun `Achievement enum has correct values`() {
        val achievements = ProductivityManager.Achievement.entries
        // Check that all expected achievements exist
        assertTrue(achievements.size >= 10)
    }
    
    @Test
    fun `Achievement FIRST_DOCUMENT has correct properties`() {
        val achievement = ProductivityManager.Achievement.FIRST_DOCUMENT
        assertEquals("first_doc", achievement.id)
        assertEquals("First Steps", achievement.title)
        assertEquals("View your first document", achievement.description)
    }
    
    @Test
    fun `Achievement READER_NOVICE has correct properties`() {
        val achievement = ProductivityManager.Achievement.READER_NOVICE
        assertEquals("reader_novice", achievement.id)
        assertEquals("Bookworm Beginner", achievement.title)
        assertEquals("Read for 1 hour total", achievement.description)
    }
    
    @Test
    fun `Achievement WRITER_NOVICE has correct properties`() {
        val achievement = ProductivityManager.Achievement.WRITER_NOVICE
        assertEquals("writer_novice", achievement.id)
        assertEquals("Wordsmith Beginner", achievement.title)
        assertEquals("Write 1,000 words", achievement.description)
    }
    
    @Test
    fun `Achievement STREAK_7 has correct properties`() {
        val achievement = ProductivityManager.Achievement.STREAK_7
        assertEquals("streak_7", achievement.id)
        assertEquals("Consistent Writer", achievement.title)
        assertEquals("7-day writing streak", achievement.description)
    }
    
    @Test
    fun `Achievement FOCUS_MASTER has correct properties`() {
        val achievement = ProductivityManager.Achievement.FOCUS_MASTER
        assertEquals("focus_master", achievement.id)
        assertEquals("Focus Master", achievement.title)
        assertEquals("Complete 10 focus sessions", achievement.description)
    }
    
    @Test
    fun `DocumentProgress data class holds values correctly`() {
        val progress = ProductivityManager.DocumentProgress(
            documentUri = "content://doc/1",
            documentName = "Test Document.pdf",
            totalPages = 100,
            currentPage = 50,
            lastAccessed = 1704067200000,
            completionPercentage = 50f
        )
        
        assertEquals("content://doc/1", progress.documentUri)
        assertEquals("Test Document.pdf", progress.documentName)
        assertEquals(100, progress.totalPages)
        assertEquals(50, progress.currentPage)
        assertEquals(1704067200000, progress.lastAccessed)
        assertEquals(50f, progress.completionPercentage, 0.01f)
    }
    
    @Test
    fun `ProductivityStats data class holds values correctly`() {
        val stats = ProductivityManager.ProductivityStats(
            totalDocumentsViewed = 100,
            totalDocumentsEdited = 50,
            totalReadingMinutes = 1000,
            totalWordsWritten = 50000,
            currentWritingStreak = 7,
            achievementsUnlocked = 5,
            todayReadingProgress = 75f,
            todayWritingProgress = 50f,
            weeklyReadingProgress = 60f
        )
        
        assertEquals(100, stats.totalDocumentsViewed)
        assertEquals(50, stats.totalDocumentsEdited)
        assertEquals(1000, stats.totalReadingMinutes)
        assertEquals(50000, stats.totalWordsWritten)
        assertEquals(7, stats.currentWritingStreak)
        assertEquals(5, stats.achievementsUnlocked)
        assertEquals(75f, stats.todayReadingProgress, 0.01f)
        assertEquals(50f, stats.todayWritingProgress, 0.01f)
        assertEquals(60f, stats.weeklyReadingProgress, 0.01f)
    }
}
