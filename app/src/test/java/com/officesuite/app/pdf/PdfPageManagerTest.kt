package com.officesuite.app.pdf

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PdfPageManager class.
 */
class PdfPageManagerTest {

    @Test
    fun `PageOperationResult success state with single output`() {
        val result = PdfPageManager.PageOperationResult(
            success = true,
            outputPaths = listOf("/path/to/merged.pdf")
        )
        
        assertTrue(result.success)
        assertEquals(1, result.outputPaths.size)
        assertEquals("/path/to/merged.pdf", result.outputPaths[0])
        assertNull(result.errorMessage)
    }

    @Test
    fun `PageOperationResult success state with multiple outputs`() {
        val result = PdfPageManager.PageOperationResult(
            success = true,
            outputPaths = listOf(
                "/path/to/page1.pdf",
                "/path/to/page2.pdf",
                "/path/to/page3.pdf"
            )
        )
        
        assertTrue(result.success)
        assertEquals(3, result.outputPaths.size)
        assertNull(result.errorMessage)
    }

    @Test
    fun `PageOperationResult failure state`() {
        val result = PdfPageManager.PageOperationResult(
            success = false,
            errorMessage = "Failed to merge PDFs"
        )
        
        assertFalse(result.success)
        assertTrue(result.outputPaths.isEmpty())
        assertEquals("Failed to merge PDFs", result.errorMessage)
    }

    @Test
    fun `PageOperationResult default values`() {
        val result = PdfPageManager.PageOperationResult(success = true)
        
        assertTrue(result.success)
        assertTrue(result.outputPaths.isEmpty())
        assertNull(result.errorMessage)
    }

    @Test
    fun `PdfPageManager can be instantiated`() {
        val manager = PdfPageManager()
        assertNotNull(manager)
    }
}
