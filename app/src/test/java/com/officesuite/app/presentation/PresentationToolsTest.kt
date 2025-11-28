package com.officesuite.app.presentation

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PresentationTools class data classes and enums.
 */
class PresentationToolsTest {

    @Test
    fun `TransitionType enum has all expected values`() {
        val types = PresentationTools.TransitionType.values()
        
        assertEquals(13, types.size)
        assertTrue(types.contains(PresentationTools.TransitionType.NONE))
        assertTrue(types.contains(PresentationTools.TransitionType.FADE))
        assertTrue(types.contains(PresentationTools.TransitionType.PUSH))
        assertTrue(types.contains(PresentationTools.TransitionType.WIPE))
        assertTrue(types.contains(PresentationTools.TransitionType.SPLIT))
        assertTrue(types.contains(PresentationTools.TransitionType.REVEAL))
        assertTrue(types.contains(PresentationTools.TransitionType.RANDOM_BARS))
        assertTrue(types.contains(PresentationTools.TransitionType.SHAPE))
        assertTrue(types.contains(PresentationTools.TransitionType.UNCOVER))
        assertTrue(types.contains(PresentationTools.TransitionType.COVER))
        assertTrue(types.contains(PresentationTools.TransitionType.FLASH))
        assertTrue(types.contains(PresentationTools.TransitionType.STRIPS))
        assertTrue(types.contains(PresentationTools.TransitionType.BLINDS))
    }

    @Test
    fun `TransitionSpeed enum has all expected values`() {
        val speeds = PresentationTools.TransitionSpeed.values()
        
        assertEquals(3, speeds.size)
        assertTrue(speeds.contains(PresentationTools.TransitionSpeed.SLOW))
        assertTrue(speeds.contains(PresentationTools.TransitionSpeed.MEDIUM))
        assertTrue(speeds.contains(PresentationTools.TransitionSpeed.FAST))
    }

    @Test
    fun `SlideLayoutType enum has all expected values`() {
        val layouts = PresentationTools.SlideLayoutType.values()
        
        assertEquals(8, layouts.size)
        assertTrue(layouts.contains(PresentationTools.SlideLayoutType.TITLE))
        assertTrue(layouts.contains(PresentationTools.SlideLayoutType.TITLE_CONTENT))
        assertTrue(layouts.contains(PresentationTools.SlideLayoutType.TWO_COLUMN))
        assertTrue(layouts.contains(PresentationTools.SlideLayoutType.SECTION_HEADER))
        assertTrue(layouts.contains(PresentationTools.SlideLayoutType.TITLE_ONLY))
        assertTrue(layouts.contains(PresentationTools.SlideLayoutType.BLANK))
        assertTrue(layouts.contains(PresentationTools.SlideLayoutType.CONTENT_CAPTION))
        assertTrue(layouts.contains(PresentationTools.SlideLayoutType.PICTURE_CAPTION))
    }

    @Test
    fun `TransitionConfig holds values correctly`() {
        val config = PresentationTools.TransitionConfig(
            type = PresentationTools.TransitionType.FADE,
            speed = PresentationTools.TransitionSpeed.SLOW,
            advanceOnClick = false,
            advanceAfterMs = 5000
        )
        
        assertEquals(PresentationTools.TransitionType.FADE, config.type)
        assertEquals(PresentationTools.TransitionSpeed.SLOW, config.speed)
        assertFalse(config.advanceOnClick)
        assertEquals(5000, config.advanceAfterMs)
    }

    @Test
    fun `TransitionConfig default values`() {
        val config = PresentationTools.TransitionConfig(
            type = PresentationTools.TransitionType.PUSH
        )
        
        assertEquals(PresentationTools.TransitionSpeed.MEDIUM, config.speed)
        assertTrue(config.advanceOnClick)
        assertNull(config.advanceAfterMs)
    }

    @Test
    fun `SpeakerNotes holds values correctly`() {
        val notes = PresentationTools.SpeakerNotes(
            slideIndex = 5,
            notes = "Talk about the quarterly results",
            timeEstimate = 120
        )
        
        assertEquals(5, notes.slideIndex)
        assertEquals("Talk about the quarterly results", notes.notes)
        assertEquals(120, notes.timeEstimate)
    }

    @Test
    fun `SpeakerNotes without time estimate`() {
        val notes = PresentationTools.SpeakerNotes(
            slideIndex = 0,
            notes = "Introduction slide notes"
        )
        
        assertNull(notes.timeEstimate)
    }

    @Test
    fun `PresentationStats holds values correctly`() {
        val stats = PresentationTools.PresentationStats(
            slideCount = 25,
            totalTextCharacters = 5000,
            totalShapes = 100,
            totalImages = 15,
            slidesWithNotes = 10,
            estimatedDurationMinutes = 30
        )
        
        assertEquals(25, stats.slideCount)
        assertEquals(5000, stats.totalTextCharacters)
        assertEquals(100, stats.totalShapes)
        assertEquals(15, stats.totalImages)
        assertEquals(10, stats.slidesWithNotes)
        assertEquals(30, stats.estimatedDurationMinutes)
    }

    @Test
    fun `PresentationStats with empty presentation`() {
        val stats = PresentationTools.PresentationStats(
            slideCount = 0,
            totalTextCharacters = 0,
            totalShapes = 0,
            totalImages = 0,
            slidesWithNotes = 0,
            estimatedDurationMinutes = 0
        )
        
        assertEquals(0, stats.slideCount)
        assertEquals(0, stats.estimatedDurationMinutes)
    }

    @Test
    fun `SlideThumbnail holds values correctly`() {
        val thumbnail = PresentationTools.SlideThumbnail(
            index = 3,
            title = "Key Findings",
            textPreview = "Our research has shown that...",
            hasNotes = true,
            shapeCount = 8
        )
        
        assertEquals(3, thumbnail.index)
        assertEquals("Key Findings", thumbnail.title)
        assertEquals("Our research has shown that...", thumbnail.textPreview)
        assertTrue(thumbnail.hasNotes)
        assertEquals(8, thumbnail.shapeCount)
    }

    @Test
    fun `SlideThumbnail without notes`() {
        val thumbnail = PresentationTools.SlideThumbnail(
            index = 0,
            title = "Title Slide",
            textPreview = "Welcome to...",
            hasNotes = false,
            shapeCount = 2
        )
        
        assertFalse(thumbnail.hasNotes)
    }

    @Test
    fun `PresentationTheme holds values correctly`() {
        val theme = PresentationTools.PresentationTheme(
            name = "Corporate Blue",
            primaryColor = "#0066CC",
            secondaryColor = "#003366",
            backgroundColor = "#FFFFFF",
            textColor = "#333333",
            accentColor = "#FF6600"
        )
        
        assertEquals("Corporate Blue", theme.name)
        assertEquals("#0066CC", theme.primaryColor)
        assertEquals("#003366", theme.secondaryColor)
        assertEquals("#FFFFFF", theme.backgroundColor)
        assertEquals("#333333", theme.textColor)
        assertEquals("#FF6600", theme.accentColor)
    }

    @Test
    fun `THEME_PROFESSIONAL has expected colors`() {
        val theme = PresentationTools.THEME_PROFESSIONAL
        
        assertEquals("Professional", theme.name)
        assertEquals("#1976D2", theme.primaryColor)
        assertEquals("#424242", theme.secondaryColor)
        assertEquals("#FFFFFF", theme.backgroundColor)
        assertEquals("#212121", theme.textColor)
        assertEquals("#FF9800", theme.accentColor)
    }

    @Test
    fun `THEME_DARK has expected colors`() {
        val theme = PresentationTools.THEME_DARK
        
        assertEquals("Dark", theme.name)
        assertEquals("#BB86FC", theme.primaryColor)
        assertEquals("#03DAC6", theme.secondaryColor)
        assertEquals("#121212", theme.backgroundColor)
        assertEquals("#FFFFFF", theme.textColor)
        assertEquals("#CF6679", theme.accentColor)
    }

    @Test
    fun `THEME_NATURE has expected colors`() {
        val theme = PresentationTools.THEME_NATURE
        
        assertEquals("Nature", theme.name)
        assertEquals("#4CAF50", theme.primaryColor)
        assertEquals("#8BC34A", theme.secondaryColor)
        assertEquals("#F1F8E9", theme.backgroundColor)
        assertEquals("#33691E", theme.textColor)
        assertEquals("#FF9800", theme.accentColor)
    }

    @Test
    fun `TransitionConfig with auto-advance timing`() {
        val config = PresentationTools.TransitionConfig(
            type = PresentationTools.TransitionType.WIPE,
            advanceOnClick = false,
            advanceAfterMs = 3000
        )
        
        assertFalse(config.advanceOnClick)
        assertEquals(3000, config.advanceAfterMs)
    }

    @Test
    fun `TransitionConfig with click only advance`() {
        val config = PresentationTools.TransitionConfig(
            type = PresentationTools.TransitionType.SPLIT,
            advanceOnClick = true,
            advanceAfterMs = null
        )
        
        assertTrue(config.advanceOnClick)
        assertNull(config.advanceAfterMs)
    }

    @Test
    fun `All transition types are distinct`() {
        val types = PresentationTools.TransitionType.values()
        val uniqueNames = types.map { it.name }.toSet()
        
        assertEquals(types.size, uniqueNames.size)
    }

    @Test
    fun `All slide layout types are distinct`() {
        val layouts = PresentationTools.SlideLayoutType.values()
        val uniqueNames = layouts.map { it.name }.toSet()
        
        assertEquals(layouts.size, uniqueNames.size)
    }

    @Test
    fun `SpeakerNotes with long text`() {
        val longNotes = "A".repeat(5000)
        val notes = PresentationTools.SpeakerNotes(
            slideIndex = 1,
            notes = longNotes
        )
        
        assertEquals(5000, notes.notes.length)
    }

    @Test
    fun `Multiple SlideThumbnails can be created`() {
        val thumbnails = listOf(
            PresentationTools.SlideThumbnail(0, "Slide 1", "", false, 1),
            PresentationTools.SlideThumbnail(1, "Slide 2", "", true, 3),
            PresentationTools.SlideThumbnail(2, "Slide 3", "", false, 2)
        )
        
        assertEquals(3, thumbnails.size)
        assertEquals(0, thumbnails[0].index)
        assertEquals(1, thumbnails[1].index)
        assertEquals(2, thumbnails[2].index)
    }

    @Test
    fun `PresentationTheme colors are valid hex format`() {
        val theme = PresentationTools.THEME_PROFESSIONAL
        val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")
        
        assertTrue(hexPattern.matches(theme.primaryColor))
        assertTrue(hexPattern.matches(theme.secondaryColor))
        assertTrue(hexPattern.matches(theme.backgroundColor))
        assertTrue(hexPattern.matches(theme.textColor))
        assertTrue(hexPattern.matches(theme.accentColor))
    }
}
