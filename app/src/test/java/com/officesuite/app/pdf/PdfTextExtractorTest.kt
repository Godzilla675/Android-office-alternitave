package com.officesuite.app.pdf

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PdfTextExtractor data classes.
 */
class PdfTextExtractorTest {

    @Test
    fun `SearchResult holds values correctly`() {
        val result = PdfTextExtractor.SearchResult(
            pageNumber = 5,
            matchCount = 3,
            contextSnippet = "...found the keyword here..."
        )
        
        assertEquals(5, result.pageNumber)
        assertEquals(3, result.matchCount)
        assertEquals("...found the keyword here...", result.contextSnippet)
    }

    @Test
    fun `SearchResult with single match`() {
        val result = PdfTextExtractor.SearchResult(
            pageNumber = 1,
            matchCount = 1,
            contextSnippet = "context"
        )
        
        assertEquals(1, result.matchCount)
    }

    @Test
    fun `TextExtractionResult success state`() {
        val result = PdfTextExtractor.TextExtractionResult(
            success = true,
            text = "Extracted text content",
            errorMessage = null
        )
        
        assertTrue(result.success)
        assertEquals("Extracted text content", result.text)
        assertNull(result.errorMessage)
    }

    @Test
    fun `TextExtractionResult failure state`() {
        val result = PdfTextExtractor.TextExtractionResult(
            success = false,
            text = null,
            errorMessage = "File not found"
        )
        
        assertFalse(result.success)
        assertNull(result.text)
        assertEquals("File not found", result.errorMessage)
    }

    @Test
    fun `TextExtractionResult with empty text`() {
        val result = PdfTextExtractor.TextExtractionResult(
            success = true,
            text = "",
            errorMessage = null
        )
        
        assertTrue(result.success)
        assertEquals("", result.text)
    }

    @Test
    fun `TextExtractionResult default values`() {
        val result = PdfTextExtractor.TextExtractionResult(success = true)
        
        assertTrue(result.success)
        assertNull(result.text)
        assertNull(result.errorMessage)
    }

    @Test
    fun `PdfTextExtractor can be instantiated`() {
        val extractor = PdfTextExtractor()
        assertNotNull(extractor)
    }

    @Test
    fun `SearchResult with long context snippet`() {
        val longSnippet = "a".repeat(500)
        val result = PdfTextExtractor.SearchResult(
            pageNumber = 10,
            matchCount = 2,
            contextSnippet = longSnippet
        )
        
        assertEquals(500, result.contextSnippet.length)
    }

    @Test
    fun `SearchResult with empty context snippet`() {
        val result = PdfTextExtractor.SearchResult(
            pageNumber = 1,
            matchCount = 1,
            contextSnippet = ""
        )
        
        assertEquals("", result.contextSnippet)
    }

    @Test
    fun `Multiple SearchResults can be created`() {
        val results = listOf(
            PdfTextExtractor.SearchResult(1, 2, "context 1"),
            PdfTextExtractor.SearchResult(3, 1, "context 2"),
            PdfTextExtractor.SearchResult(5, 4, "context 3")
        )
        
        assertEquals(3, results.size)
        assertEquals(1, results[0].pageNumber)
        assertEquals(3, results[1].pageNumber)
        assertEquals(5, results[2].pageNumber)
    }
}
