package com.officesuite.app.developer

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DebugManager data classes.
 */
class DebugManagerTest {

    @Test
    fun `LogLevel enum has expected values in correct order`() {
        val levels = LogLevel.values()
        
        assertEquals(5, levels.size)
        assertEquals(LogLevel.VERBOSE, levels[0])
        assertEquals(LogLevel.DEBUG, levels[1])
        assertEquals(LogLevel.INFO, levels[2])
        assertEquals(LogLevel.WARNING, levels[3])
        assertEquals(LogLevel.ERROR, levels[4])
    }

    @Test
    fun `LogLevel ordinals are ascending`() {
        assertTrue(LogLevel.VERBOSE.ordinal < LogLevel.DEBUG.ordinal)
        assertTrue(LogLevel.DEBUG.ordinal < LogLevel.INFO.ordinal)
        assertTrue(LogLevel.INFO.ordinal < LogLevel.WARNING.ordinal)
        assertTrue(LogLevel.WARNING.ordinal < LogLevel.ERROR.ordinal)
    }

    @Test
    fun `LogEntry data class holds values correctly`() {
        val entry = LogEntry(
            timestamp = 1234567890L,
            level = LogLevel.INFO,
            tag = "TestTag",
            message = "Test message",
            exception = null
        )
        
        assertEquals(1234567890L, entry.timestamp)
        assertEquals(LogLevel.INFO, entry.level)
        assertEquals("TestTag", entry.tag)
        assertEquals("Test message", entry.message)
        assertNull(entry.exception)
    }

    @Test
    fun `LogEntry with exception holds stack trace`() {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = LogLevel.ERROR,
            tag = "ErrorTag",
            message = "Error occurred",
            exception = "java.lang.RuntimeException: Test exception\n\tat TestClass.test(TestClass.kt:10)"
        )
        
        assertNotNull(entry.exception)
        assertTrue(entry.exception!!.contains("RuntimeException"))
    }

    @Test
    fun `BetaFeature data class holds values correctly`() {
        val feature = BetaFeature(
            id = "test_feature",
            name = "Test Feature",
            description = "A test feature",
            category = FeatureCategory.PERFORMANCE,
            isExperimental = true,
            isEnabled = false
        )
        
        assertEquals("test_feature", feature.id)
        assertEquals("Test Feature", feature.name)
        assertEquals("A test feature", feature.description)
        assertEquals(FeatureCategory.PERFORMANCE, feature.category)
        assertTrue(feature.isExperimental)
        assertFalse(feature.isEnabled)
    }

    @Test
    fun `BetaFeature copy updates correctly`() {
        val feature = BetaFeature(
            id = "test",
            name = "Test",
            description = "Test",
            category = FeatureCategory.DEVELOPER,
            isExperimental = false,
            isEnabled = false
        )
        
        val updated = feature.copy(isEnabled = true)
        
        assertEquals(feature.id, updated.id)
        assertEquals(feature.name, updated.name)
        assertFalse(feature.isEnabled)
        assertTrue(updated.isEnabled)
    }

    @Test
    fun `FeatureCategory enum has expected values`() {
        val categories = FeatureCategory.values()
        
        assertTrue(categories.contains(FeatureCategory.PERFORMANCE))
        assertTrue(categories.contains(FeatureCategory.DEVELOPER))
        assertTrue(categories.contains(FeatureCategory.AI))
        assertTrue(categories.contains(FeatureCategory.COLLABORATION))
        assertTrue(categories.contains(FeatureCategory.INFRASTRUCTURE))
        assertTrue(categories.contains(FeatureCategory.ENTERPRISE))
        assertTrue(categories.contains(FeatureCategory.UI))
    }

    @Test
    fun `FeatureCategory has correct display names`() {
        assertEquals("Performance", FeatureCategory.PERFORMANCE.displayName)
        assertEquals("Developer Tools", FeatureCategory.DEVELOPER.displayName)
        assertEquals("AI & ML", FeatureCategory.AI.displayName)
        assertEquals("Collaboration", FeatureCategory.COLLABORATION.displayName)
        assertEquals("Infrastructure", FeatureCategory.INFRASTRUCTURE.displayName)
        assertEquals("Enterprise", FeatureCategory.ENTERPRISE.displayName)
        assertEquals("User Interface", FeatureCategory.UI.displayName)
    }
}
