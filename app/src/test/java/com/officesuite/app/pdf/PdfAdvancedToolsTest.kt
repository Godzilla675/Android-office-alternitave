package com.officesuite.app.pdf

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PdfAdvancedTools data classes and enums.
 */
class PdfAdvancedToolsTest {

    @Test
    fun `WatermarkPosition enum has all expected values`() {
        val positions = PdfAdvancedTools.WatermarkPosition.values()
        
        assertEquals(6, positions.size)
        assertTrue(positions.contains(PdfAdvancedTools.WatermarkPosition.CENTER))
        assertTrue(positions.contains(PdfAdvancedTools.WatermarkPosition.TOP_LEFT))
        assertTrue(positions.contains(PdfAdvancedTools.WatermarkPosition.TOP_RIGHT))
        assertTrue(positions.contains(PdfAdvancedTools.WatermarkPosition.BOTTOM_LEFT))
        assertTrue(positions.contains(PdfAdvancedTools.WatermarkPosition.BOTTOM_RIGHT))
        assertTrue(positions.contains(PdfAdvancedTools.WatermarkPosition.DIAGONAL))
    }

    @Test
    fun `CompressionLevel enum has all expected values`() {
        val levels = PdfAdvancedTools.CompressionLevel.values()
        
        assertEquals(3, levels.size)
        assertTrue(levels.contains(PdfAdvancedTools.CompressionLevel.LOW))
        assertTrue(levels.contains(PdfAdvancedTools.CompressionLevel.MEDIUM))
        assertTrue(levels.contains(PdfAdvancedTools.CompressionLevel.HIGH))
    }

    @Test
    fun `DifferenceType enum has all expected values`() {
        val types = PdfAdvancedTools.DifferenceType.values()
        
        assertEquals(5, types.size)
        assertTrue(types.contains(PdfAdvancedTools.DifferenceType.PAGE_MISSING))
        assertTrue(types.contains(PdfAdvancedTools.DifferenceType.CONTENT_CHANGED))
        assertTrue(types.contains(PdfAdvancedTools.DifferenceType.TEXT_ADDED))
        assertTrue(types.contains(PdfAdvancedTools.DifferenceType.TEXT_REMOVED))
        assertTrue(types.contains(PdfAdvancedTools.DifferenceType.IMAGE_CHANGED))
    }

    @Test
    fun `BatesPosition enum has all expected values`() {
        val positions = PdfAdvancedTools.BatesPosition.values()
        
        assertEquals(6, positions.size)
        assertTrue(positions.contains(PdfAdvancedTools.BatesPosition.BOTTOM_LEFT))
        assertTrue(positions.contains(PdfAdvancedTools.BatesPosition.BOTTOM_CENTER))
        assertTrue(positions.contains(PdfAdvancedTools.BatesPosition.BOTTOM_RIGHT))
        assertTrue(positions.contains(PdfAdvancedTools.BatesPosition.TOP_LEFT))
        assertTrue(positions.contains(PdfAdvancedTools.BatesPosition.TOP_CENTER))
        assertTrue(positions.contains(PdfAdvancedTools.BatesPosition.TOP_RIGHT))
    }

    @Test
    fun `PdfMetadata holds values correctly`() {
        val metadata = PdfAdvancedTools.PdfMetadata(
            title = "Test Document",
            author = "John Doe",
            subject = "Testing",
            keywords = "test, unit, kotlin",
            creator = "Test App",
            producer = "iText",
            creationDate = "2024-01-15",
            modificationDate = "2024-01-16",
            pageCount = 10,
            fileSize = 102400L
        )
        
        assertEquals("Test Document", metadata.title)
        assertEquals("John Doe", metadata.author)
        assertEquals("Testing", metadata.subject)
        assertEquals("test, unit, kotlin", metadata.keywords)
        assertEquals("Test App", metadata.creator)
        assertEquals("iText", metadata.producer)
        assertEquals("2024-01-15", metadata.creationDate)
        assertEquals("2024-01-16", metadata.modificationDate)
        assertEquals(10, metadata.pageCount)
        assertEquals(102400L, metadata.fileSize)
    }

    @Test
    fun `PdfMetadata with null values`() {
        val metadata = PdfAdvancedTools.PdfMetadata(
            title = null,
            author = null,
            subject = null,
            keywords = null,
            creator = null,
            producer = null,
            creationDate = null,
            modificationDate = null,
            pageCount = 5,
            fileSize = 50000L
        )
        
        assertNull(metadata.title)
        assertNull(metadata.author)
        assertEquals(5, metadata.pageCount)
    }

    @Test
    fun `WatermarkConfig holds values correctly`() {
        val config = PdfAdvancedTools.WatermarkConfig(
            text = "CONFIDENTIAL",
            fontSize = 72f,
            opacity = 0.5f,
            rotation = 30f,
            color = 0xFF0000,
            position = PdfAdvancedTools.WatermarkPosition.DIAGONAL
        )
        
        assertEquals("CONFIDENTIAL", config.text)
        assertEquals(72f, config.fontSize, 0.01f)
        assertEquals(0.5f, config.opacity, 0.01f)
        assertEquals(30f, config.rotation, 0.01f)
        assertEquals(0xFF0000, config.color)
        assertEquals(PdfAdvancedTools.WatermarkPosition.DIAGONAL, config.position)
    }

    @Test
    fun `WatermarkConfig default values`() {
        val config = PdfAdvancedTools.WatermarkConfig(text = "DRAFT")
        
        assertEquals("DRAFT", config.text)
        assertEquals(48f, config.fontSize, 0.01f)
        assertEquals(0.3f, config.opacity, 0.01f)
        assertEquals(45f, config.rotation, 0.01f)
        assertEquals(0x888888, config.color)
        assertEquals(PdfAdvancedTools.WatermarkPosition.CENTER, config.position)
    }

    @Test
    fun `ComparisonResult identical PDFs`() {
        val result = PdfAdvancedTools.ComparisonResult(
            areIdentical = true,
            pdf1PageCount = 10,
            pdf2PageCount = 10,
            differences = emptyList(),
            similarityPercentage = 100.0
        )
        
        assertTrue(result.areIdentical)
        assertEquals(10, result.pdf1PageCount)
        assertEquals(10, result.pdf2PageCount)
        assertTrue(result.differences.isEmpty())
        assertEquals(100.0, result.similarityPercentage, 0.01)
    }

    @Test
    fun `ComparisonResult with differences`() {
        val differences = listOf(
            PdfAdvancedTools.PageDifference(
                pageNumber = 3,
                differenceType = PdfAdvancedTools.DifferenceType.TEXT_ADDED,
                description = "5 words added"
            ),
            PdfAdvancedTools.PageDifference(
                pageNumber = 5,
                differenceType = PdfAdvancedTools.DifferenceType.TEXT_REMOVED,
                description = "2 words removed"
            )
        )
        
        val result = PdfAdvancedTools.ComparisonResult(
            areIdentical = false,
            pdf1PageCount = 10,
            pdf2PageCount = 10,
            differences = differences,
            similarityPercentage = 80.0
        )
        
        assertFalse(result.areIdentical)
        assertEquals(2, result.differences.size)
        assertEquals(80.0, result.similarityPercentage, 0.01)
    }

    @Test
    fun `PageDifference holds values correctly`() {
        val diff = PdfAdvancedTools.PageDifference(
            pageNumber = 7,
            differenceType = PdfAdvancedTools.DifferenceType.CONTENT_CHANGED,
            description = "Content changed on page 7"
        )
        
        assertEquals(7, diff.pageNumber)
        assertEquals(PdfAdvancedTools.DifferenceType.CONTENT_CHANGED, diff.differenceType)
        assertEquals("Content changed on page 7", diff.description)
    }

    @Test
    fun `OptimizationResult success state`() {
        val result = PdfAdvancedTools.OptimizationResult(
            success = true,
            originalSize = 1000000L,
            newSize = 500000L,
            savedBytes = 500000L,
            savedPercentage = 50.0
        )
        
        assertTrue(result.success)
        assertEquals(1000000L, result.originalSize)
        assertEquals(500000L, result.newSize)
        assertEquals(500000L, result.savedBytes)
        assertEquals(50.0, result.savedPercentage, 0.01)
        assertNull(result.error)
    }

    @Test
    fun `OptimizationResult failure state`() {
        val result = PdfAdvancedTools.OptimizationResult(
            success = false,
            originalSize = 1000000L,
            newSize = 0,
            savedBytes = 0,
            savedPercentage = 0.0,
            error = "Failed to open file"
        )
        
        assertFalse(result.success)
        assertEquals("Failed to open file", result.error)
    }

    @Test
    fun `OptimizationResult with minimal savings`() {
        val result = PdfAdvancedTools.OptimizationResult(
            success = true,
            originalSize = 100000L,
            newSize = 99000L,
            savedBytes = 1000L,
            savedPercentage = 1.0
        )
        
        assertTrue(result.success)
        assertEquals(1.0, result.savedPercentage, 0.01)
    }

    @Test
    fun `ComparisonResult with missing pages`() {
        val differences = listOf(
            PdfAdvancedTools.PageDifference(
                pageNumber = 11,
                differenceType = PdfAdvancedTools.DifferenceType.PAGE_MISSING,
                description = "Page 11 exists only in first PDF"
            )
        )
        
        val result = PdfAdvancedTools.ComparisonResult(
            areIdentical = false,
            pdf1PageCount = 12,
            pdf2PageCount = 10,
            differences = differences,
            similarityPercentage = 83.33
        )
        
        assertFalse(result.areIdentical)
        assertEquals(12, result.pdf1PageCount)
        assertEquals(10, result.pdf2PageCount)
    }

    @Test
    fun `WatermarkConfig with custom color`() {
        val config = PdfAdvancedTools.WatermarkConfig(
            text = "TEST",
            color = 0x00FF00
        )
        
        assertEquals(0x00FF00, config.color)
    }

    @Test
    fun `All DifferenceType values are distinct`() {
        val types = PdfAdvancedTools.DifferenceType.values()
        val uniqueNames = types.map { it.name }.toSet()
        
        assertEquals(types.size, uniqueNames.size)
    }
}
