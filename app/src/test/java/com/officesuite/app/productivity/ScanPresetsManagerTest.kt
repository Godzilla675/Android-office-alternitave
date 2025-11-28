package com.officesuite.app.productivity

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScanPresetsManager class.
 */
class ScanPresetsManagerTest {
    
    @Test
    fun `ImageFilter enum has correct values`() {
        val filters = ScanPresetsManager.ImageFilter.entries
        assertEquals(7, filters.size)
        
        assertEquals("Original", ScanPresetsManager.ImageFilter.NONE.displayName)
        assertEquals("Auto Enhance", ScanPresetsManager.ImageFilter.AUTO.displayName)
        assertEquals("Grayscale", ScanPresetsManager.ImageFilter.GRAYSCALE.displayName)
        assertEquals("Black & White", ScanPresetsManager.ImageFilter.BLACK_WHITE.displayName)
        assertEquals("High Contrast", ScanPresetsManager.ImageFilter.HIGH_CONTRAST.displayName)
        assertEquals("Magic Color", ScanPresetsManager.ImageFilter.MAGIC_COLOR.displayName)
        assertEquals("Document Mode", ScanPresetsManager.ImageFilter.DOCUMENT.displayName)
    }
    
    @Test
    fun `PageSize enum has correct values`() {
        val sizes = ScanPresetsManager.PageSize.entries
        assertEquals(5, sizes.size)
        
        val a4 = ScanPresetsManager.PageSize.A4
        assertEquals("A4", a4.displayName)
        assertEquals(210f, a4.widthMm, 0.01f)
        assertEquals(297f, a4.heightMm, 0.01f)
        
        val letter = ScanPresetsManager.PageSize.LETTER
        assertEquals("Letter", letter.displayName)
        assertEquals(216f, letter.widthMm, 0.01f)
        assertEquals(279f, letter.heightMm, 0.01f)
    }
    
    @Test
    fun `ScanQuality enum has correct values`() {
        val qualities = ScanPresetsManager.ScanQuality.entries
        assertEquals(4, qualities.size)
        
        assertEquals(72, ScanPresetsManager.ScanQuality.LOW.dpi)
        assertEquals(150, ScanPresetsManager.ScanQuality.MEDIUM.dpi)
        assertEquals(300, ScanPresetsManager.ScanQuality.HIGH.dpi)
        assertEquals(600, ScanPresetsManager.ScanQuality.MAXIMUM.dpi)
    }
    
    @Test
    fun `OutputFormat enum has correct values`() {
        val formats = ScanPresetsManager.OutputFormat.entries
        assertEquals(3, formats.size)
        
        assertEquals("pdf", ScanPresetsManager.OutputFormat.PDF.extension)
        assertEquals("jpg", ScanPresetsManager.OutputFormat.JPEG.extension)
        assertEquals("png", ScanPresetsManager.OutputFormat.PNG.extension)
    }
    
    @Test
    fun `ScanPreset data class holds values correctly`() {
        val preset = ScanPresetsManager.ScanPreset(
            id = "test_preset",
            name = "Test Preset",
            description = "A test preset",
            filter = ScanPresetsManager.ImageFilter.AUTO.name,
            quality = ScanPresetsManager.ScanQuality.HIGH.name,
            pageSize = ScanPresetsManager.PageSize.A4.name,
            outputFormat = ScanPresetsManager.OutputFormat.PDF.name,
            autoCrop = true,
            autoPerspectiveCorrection = true,
            enhanceColors = false,
            removeShadows = true,
            multiPageMode = false,
            autoSaveEnabled = false,
            autoSavePath = null,
            runOcrAfterScan = false,
            ocrLanguage = "en",
            isBuiltIn = false
        )
        
        assertEquals("test_preset", preset.id)
        assertEquals("Test Preset", preset.name)
        assertEquals("A test preset", preset.description)
        assertEquals("AUTO", preset.filter)
        assertEquals("HIGH", preset.quality)
        assertEquals("A4", preset.pageSize)
        assertEquals("PDF", preset.outputFormat)
        assertTrue(preset.autoCrop)
        assertTrue(preset.autoPerspectiveCorrection)
        assertFalse(preset.enhanceColors)
        assertTrue(preset.removeShadows)
        assertFalse(preset.multiPageMode)
        assertFalse(preset.autoSaveEnabled)
        assertNull(preset.autoSavePath)
        assertFalse(preset.runOcrAfterScan)
        assertEquals("en", preset.ocrLanguage)
        assertFalse(preset.isBuiltIn)
    }
    
    @Test
    fun `ScanPreset with OCR settings holds values correctly`() {
        val preset = ScanPresetsManager.ScanPreset(
            id = "ocr_preset",
            name = "OCR Preset",
            description = "Preset with OCR enabled",
            runOcrAfterScan = true,
            ocrLanguage = "de"
        )
        
        assertTrue(preset.runOcrAfterScan)
        assertEquals("de", preset.ocrLanguage)
    }
    
    @Test
    fun `ScanPreset with auto-save settings holds values correctly`() {
        val preset = ScanPresetsManager.ScanPreset(
            id = "autosave_preset",
            name = "Auto-save Preset",
            description = "Preset with auto-save enabled",
            autoSaveEnabled = true,
            autoSavePath = "/storage/Documents/Scans"
        )
        
        assertTrue(preset.autoSaveEnabled)
        assertEquals("/storage/Documents/Scans", preset.autoSavePath)
    }
    
    @Test
    fun `built-in preset has isBuiltIn true`() {
        val preset = ScanPresetsManager.ScanPreset(
            id = "builtin_preset",
            name = "Built-in Preset",
            description = "A built-in preset",
            isBuiltIn = true
        )
        
        assertTrue(preset.isBuiltIn)
    }
    
    @Test
    fun `custom preset has isBuiltIn false by default`() {
        val preset = ScanPresetsManager.ScanPreset(
            id = "custom_preset",
            name = "Custom Preset",
            description = "A custom preset"
        )
        
        assertFalse(preset.isBuiltIn)
    }
}
