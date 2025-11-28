package com.officesuite.app.ocr

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OCR data classes and enums.
 */
class OcrDataClassesTest {

    @Test
    fun `OcrLanguage enum has all expected values`() {
        val languages = OcrLanguage.values()
        
        assertEquals(5, languages.size)
        assertTrue(languages.contains(OcrLanguage.LATIN))
        assertTrue(languages.contains(OcrLanguage.CHINESE))
        assertTrue(languages.contains(OcrLanguage.JAPANESE))
        assertTrue(languages.contains(OcrLanguage.KOREAN))
        assertTrue(languages.contains(OcrLanguage.DEVANAGARI))
    }

    @Test
    fun `OcrLanguage LATIN has correct properties`() {
        val language = OcrLanguage.LATIN
        
        assertEquals("Latin (English, etc.)", language.displayName)
        assertEquals("latin", language.code)
    }

    @Test
    fun `OcrLanguage CHINESE has correct properties`() {
        val language = OcrLanguage.CHINESE
        
        assertEquals("Chinese", language.displayName)
        assertEquals("chinese", language.code)
    }

    @Test
    fun `OcrLanguage JAPANESE has correct properties`() {
        val language = OcrLanguage.JAPANESE
        
        assertEquals("Japanese", language.displayName)
        assertEquals("japanese", language.code)
    }

    @Test
    fun `OcrLanguage KOREAN has correct properties`() {
        val language = OcrLanguage.KOREAN
        
        assertEquals("Korean", language.displayName)
        assertEquals("korean", language.code)
    }

    @Test
    fun `OcrLanguage DEVANAGARI has correct properties`() {
        val language = OcrLanguage.DEVANAGARI
        
        assertEquals("Devanagari (Hindi, etc.)", language.displayName)
        assertEquals("devanagari", language.code)
    }

    @Test
    fun `OcrResult success state`() {
        val blocks = listOf(
            TextBlock("Hello World", 0.95f, null, emptyList())
        )
        
        val result = OcrResult(
            fullText = "Hello World",
            blocks = blocks,
            success = true,
            error = null,
            language = OcrLanguage.LATIN
        )
        
        assertEquals("Hello World", result.fullText)
        assertEquals(1, result.blocks.size)
        assertTrue(result.success)
        assertNull(result.error)
        assertEquals(OcrLanguage.LATIN, result.language)
    }

    @Test
    fun `OcrResult failure state`() {
        val result = OcrResult(
            fullText = "",
            blocks = emptyList(),
            success = false,
            error = "Recognition failed",
            language = OcrLanguage.CHINESE
        )
        
        assertEquals("", result.fullText)
        assertTrue(result.blocks.isEmpty())
        assertFalse(result.success)
        assertEquals("Recognition failed", result.error)
        assertEquals(OcrLanguage.CHINESE, result.language)
    }

    @Test
    fun `OcrResult default values`() {
        val result = OcrResult(
            fullText = "Text",
            blocks = emptyList(),
            success = true
        )
        
        assertNull(result.error)
        assertEquals(OcrLanguage.LATIN, result.language)
    }

    @Test
    fun `BatchOcrResult success state`() {
        val results = listOf(
            OcrResult("Page 1", emptyList(), true),
            OcrResult("Page 2", emptyList(), true)
        )
        
        val batchResult = BatchOcrResult(
            results = results,
            combinedText = "--- Page 1 ---\nPage 1\n\n--- Page 2 ---\nPage 2",
            totalPages = 2,
            successfulPages = 2,
            success = true,
            language = OcrLanguage.LATIN
        )
        
        assertEquals(2, batchResult.results.size)
        assertTrue(batchResult.combinedText.contains("Page 1"))
        assertTrue(batchResult.combinedText.contains("Page 2"))
        assertEquals(2, batchResult.totalPages)
        assertEquals(2, batchResult.successfulPages)
        assertTrue(batchResult.success)
    }

    @Test
    fun `BatchOcrResult partial success`() {
        val results = listOf(
            OcrResult("Page 1", emptyList(), true),
            OcrResult("", emptyList(), false, "Error")
        )
        
        val batchResult = BatchOcrResult(
            results = results,
            combinedText = "--- Page 1 ---\nPage 1",
            totalPages = 2,
            successfulPages = 1,
            success = true,
            language = OcrLanguage.JAPANESE
        )
        
        assertEquals(2, batchResult.totalPages)
        assertEquals(1, batchResult.successfulPages)
        assertTrue(batchResult.success)
    }

    @Test
    fun `BatchOcrResult all failed`() {
        val batchResult = BatchOcrResult(
            results = emptyList(),
            combinedText = "",
            totalPages = 3,
            successfulPages = 0,
            success = false,
            language = OcrLanguage.KOREAN
        )
        
        assertEquals(3, batchResult.totalPages)
        assertEquals(0, batchResult.successfulPages)
        assertFalse(batchResult.success)
    }

    @Test
    fun `TextBlock holds values correctly`() {
        val lines = listOf(
            TextLine("First line", 0.92f),
            TextLine("Second line", 0.88f)
        )
        val boundingBox = BoundingBox(10, 20, 200, 50)
        
        val block = TextBlock(
            text = "First line\nSecond line",
            confidence = 0.90f,
            boundingBox = boundingBox,
            lines = lines
        )
        
        assertEquals("First line\nSecond line", block.text)
        assertEquals(0.90f, block.confidence, 0.01f)
        assertNotNull(block.boundingBox)
        assertEquals(10, block.boundingBox!!.left)
        assertEquals(2, block.lines.size)
    }

    @Test
    fun `TextBlock with null bounding box`() {
        val block = TextBlock(
            text = "Some text",
            confidence = 0.85f,
            boundingBox = null,
            lines = emptyList()
        )
        
        assertNull(block.boundingBox)
    }

    @Test
    fun `TextLine holds values correctly`() {
        val line = TextLine(
            text = "This is a line of text",
            confidence = 0.95f
        )
        
        assertEquals("This is a line of text", line.text)
        assertEquals(0.95f, line.confidence, 0.01f)
    }

    @Test
    fun `TextLine with low confidence`() {
        val line = TextLine(
            text = "Unclear text",
            confidence = 0.3f
        )
        
        assertEquals(0.3f, line.confidence, 0.01f)
    }

    @Test
    fun `BoundingBox holds values correctly`() {
        val box = BoundingBox(
            left = 100,
            top = 200,
            right = 500,
            bottom = 250
        )
        
        assertEquals(100, box.left)
        assertEquals(200, box.top)
        assertEquals(500, box.right)
        assertEquals(250, box.bottom)
    }

    @Test
    fun `BoundingBox calculates width correctly`() {
        val box = BoundingBox(
            left = 100,
            top = 50,
            right = 400,
            bottom = 100
        )
        
        val width = box.right - box.left
        val height = box.bottom - box.top
        
        assertEquals(300, width)
        assertEquals(50, height)
    }

    @Test
    fun `All OcrLanguage codes are unique`() {
        val languages = OcrLanguage.values()
        val uniqueCodes = languages.map { it.code }.toSet()
        
        assertEquals(languages.size, uniqueCodes.size)
    }

    @Test
    fun `All OcrLanguage display names are unique`() {
        val languages = OcrLanguage.values()
        val uniqueNames = languages.map { it.displayName }.toSet()
        
        assertEquals(languages.size, uniqueNames.size)
    }

    @Test
    fun `Multiple TextBlocks can be created`() {
        val blocks = listOf(
            TextBlock("Block 1", 0.9f, null, emptyList()),
            TextBlock("Block 2", 0.85f, null, emptyList()),
            TextBlock("Block 3", 0.8f, null, emptyList())
        )
        
        assertEquals(3, blocks.size)
        assertEquals("Block 1", blocks[0].text)
        assertEquals("Block 2", blocks[1].text)
        assertEquals("Block 3", blocks[2].text)
    }

    @Test
    fun `TextBlock with empty lines list`() {
        val block = TextBlock(
            text = "Single block",
            confidence = 0.88f,
            boundingBox = null,
            lines = emptyList()
        )
        
        assertTrue(block.lines.isEmpty())
    }

    @Test
    fun `OcrResult with multiple blocks`() {
        val blocks = listOf(
            TextBlock("Header", 0.95f, null, emptyList()),
            TextBlock("Content paragraph one", 0.9f, null, emptyList()),
            TextBlock("Content paragraph two", 0.88f, null, emptyList())
        )
        
        val result = OcrResult(
            fullText = "Header\n\nContent paragraph one\n\nContent paragraph two",
            blocks = blocks,
            success = true
        )
        
        assertEquals(3, result.blocks.size)
        assertTrue(result.fullText.contains("Header"))
    }
}
