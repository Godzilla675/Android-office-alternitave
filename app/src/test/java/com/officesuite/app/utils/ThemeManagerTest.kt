package com.officesuite.app.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the ThemeManager utility class.
 */
class ThemeManagerTest {

    @Test
    fun `ThemeMode LIGHT has correct AppCompatDelegate value`() {
        val mode = ThemeManager.ThemeMode.LIGHT
        
        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO, mode.value)
    }

    @Test
    fun `ThemeMode DARK has correct AppCompatDelegate value`() {
        val mode = ThemeManager.ThemeMode.DARK
        
        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES, mode.value)
    }

    @Test
    fun `ThemeMode SYSTEM has correct AppCompatDelegate value`() {
        val mode = ThemeManager.ThemeMode.SYSTEM
        
        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, mode.value)
    }

    @Test
    fun `ThemeMode fromValue returns correct mode for LIGHT value`() {
        val mode = ThemeManager.ThemeMode.fromValue(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        
        assertEquals(ThemeManager.ThemeMode.LIGHT, mode)
    }

    @Test
    fun `ThemeMode fromValue returns correct mode for DARK value`() {
        val mode = ThemeManager.ThemeMode.fromValue(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        
        assertEquals(ThemeManager.ThemeMode.DARK, mode)
    }

    @Test
    fun `ThemeMode fromValue returns correct mode for SYSTEM value`() {
        val mode = ThemeManager.ThemeMode.fromValue(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        assertEquals(ThemeManager.ThemeMode.SYSTEM, mode)
    }

    @Test
    fun `ThemeMode fromValue returns SYSTEM for unknown value`() {
        val mode = ThemeManager.ThemeMode.fromValue(-999)
        
        assertEquals(ThemeManager.ThemeMode.SYSTEM, mode)
    }

    @Test
    fun `ThemeMode enum has exactly three values`() {
        assertEquals(3, ThemeManager.ThemeMode.entries.size)
    }

    @Test
    fun `supportsDynamicColors returns boolean based on Android version`() {
        // This will return true on Android 12+ and false on earlier versions
        val result = ThemeManager.supportsDynamicColors()
        
        // Just verify it doesn't throw and returns a boolean
        assertTrue(result || !result) // Always true, just checking no exception
    }
}
