package com.officesuite.app.performance

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LazyFontManager data classes.
 */
class LazyFontManagerTest {

    @Test
    fun `FontInfo data class holds values correctly`() {
        val fontInfo = FontInfo(
            id = "roboto-regular",
            displayName = "Roboto",
            assetPath = "fonts/roboto_regular.ttf",
            isSystem = false,
            category = FontCategory.SANS_SERIF,
            weight = 400,
            isItalic = false
        )
        
        assertEquals("roboto-regular", fontInfo.id)
        assertEquals("Roboto", fontInfo.displayName)
        assertEquals("fonts/roboto_regular.ttf", fontInfo.assetPath)
        assertFalse(fontInfo.isSystem)
        assertEquals(FontCategory.SANS_SERIF, fontInfo.category)
        assertEquals(400, fontInfo.weight)
        assertFalse(fontInfo.isItalic)
    }

    @Test
    fun `FontInfo for system font has null assetPath`() {
        val fontInfo = FontInfo(
            id = "sans-serif",
            displayName = "Sans Serif",
            assetPath = null,
            isSystem = true
        )
        
        assertTrue(fontInfo.isSystem)
        assertNull(fontInfo.assetPath)
    }

    @Test
    fun `FontCategory enum has expected values`() {
        val categories = FontCategory.values()
        
        assertEquals(6, categories.size)
        assertTrue(categories.contains(FontCategory.SERIF))
        assertTrue(categories.contains(FontCategory.SANS_SERIF))
        assertTrue(categories.contains(FontCategory.MONOSPACE))
        assertTrue(categories.contains(FontCategory.DISPLAY))
        assertTrue(categories.contains(FontCategory.HANDWRITING))
        assertTrue(categories.contains(FontCategory.ACCESSIBILITY))
    }

    @Test
    fun `FontLoadState Loading is singleton`() {
        val state1 = FontLoadState.Loading
        val state2 = FontLoadState.Loading
        
        assertSame(state1, state2)
    }

    @Test
    fun `FontLoadState Error contains message`() {
        val state = FontLoadState.Error("Font not found")
        
        assertTrue(state is FontLoadState.Error)
        assertEquals("Font not found", (state as FontLoadState.Error).message)
    }

    @Test
    fun `FontCacheStats data class holds values correctly`() {
        val stats = FontCacheStats(
            loadedFonts = 5,
            availableFonts = 12,
            loadingFonts = 2
        )
        
        assertEquals(5, stats.loadedFonts)
        assertEquals(12, stats.availableFonts)
        assertEquals(2, stats.loadingFonts)
    }

    @Test
    fun `FontInfo default values are correct`() {
        val fontInfo = FontInfo(
            id = "test",
            displayName = "Test",
            assetPath = "path"
        )
        
        // Check default values
        assertFalse(fontInfo.isSystem)
        assertEquals(FontCategory.SANS_SERIF, fontInfo.category)
        assertEquals(400, fontInfo.weight)
        assertFalse(fontInfo.isItalic)
    }

    @Test
    fun `FontInfo italic variant has isItalic true`() {
        val fontInfo = FontInfo(
            id = "roboto-italic",
            displayName = "Roboto Italic",
            assetPath = "fonts/roboto_italic.ttf",
            isItalic = true
        )
        
        assertTrue(fontInfo.isItalic)
    }

    @Test
    fun `FontInfo bold variant has higher weight`() {
        val regular = FontInfo(
            id = "roboto-regular",
            displayName = "Roboto",
            assetPath = "fonts/roboto_regular.ttf",
            weight = 400
        )
        
        val bold = FontInfo(
            id = "roboto-bold",
            displayName = "Roboto Bold",
            assetPath = "fonts/roboto_bold.ttf",
            weight = 700
        )
        
        assertTrue(bold.weight > regular.weight)
    }
}
