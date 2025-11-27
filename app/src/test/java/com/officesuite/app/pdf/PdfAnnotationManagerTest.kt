package com.officesuite.app.pdf

import android.graphics.Color
import android.graphics.RectF
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PdfAnnotationManager class.
 */
class PdfAnnotationManagerTest {

    @Test
    fun `HighlightColor values are correct`() {
        val yellow = PdfAnnotationManager.HighlightColor.YELLOW
        assertEquals(255, yellow.r)
        assertEquals(255, yellow.g)
        assertEquals(0, yellow.b)
        
        val green = PdfAnnotationManager.HighlightColor.GREEN
        assertEquals(0, green.r)
        assertEquals(255, green.g)
        assertEquals(0, green.b)
        
        val blue = PdfAnnotationManager.HighlightColor.BLUE
        assertEquals(0, blue.r)
        assertEquals(200, blue.g)
        assertEquals(255, blue.b)
    }

    @Test
    fun `HighlightAnnotation default values`() {
        val rect = RectF(10f, 20f, 100f, 50f)
        val annotation = PdfAnnotationManager.HighlightAnnotation(
            pageNumber = 1,
            rect = rect
        )
        
        assertEquals(1, annotation.pageNumber)
        assertSame(rect, annotation.rect) // Use assertSame since RectF may not work well with mocks
        assertEquals(PdfAnnotationManager.HighlightColor.YELLOW, annotation.color)
        assertEquals(0.5f, annotation.opacity, 0.001f)
    }

    @Test
    fun `HighlightAnnotation with custom values`() {
        val rect = RectF(0f, 0f, 200f, 100f)
        val annotation = PdfAnnotationManager.HighlightAnnotation(
            pageNumber = 5,
            rect = rect,
            color = PdfAnnotationManager.HighlightColor.GREEN,
            opacity = 0.8f
        )
        
        assertEquals(5, annotation.pageNumber)
        assertEquals(PdfAnnotationManager.HighlightColor.GREEN, annotation.color)
        assertEquals(0.8f, annotation.opacity, 0.001f)
    }

    @Test
    fun `TextBoxAnnotation default values`() {
        val rect = RectF(50f, 50f, 150f, 100f)
        val annotation = PdfAnnotationManager.TextBoxAnnotation(
            pageNumber = 1,
            rect = rect,
            text = "Test text"
        )
        
        assertEquals(1, annotation.pageNumber)
        assertEquals("Test text", annotation.text)
        assertEquals(12f, annotation.fontSize, 0.001f)
        assertEquals(Color.BLACK, annotation.textColor)
        assertEquals(Color.WHITE, annotation.backgroundColor)
    }

    @Test
    fun `FreehandAnnotation stores points correctly`() {
        val points = listOf(
            Pair(10f, 10f),
            Pair(20f, 15f),
            Pair(30f, 20f)
        )
        val annotation = PdfAnnotationManager.FreehandAnnotation(
            pageNumber = 2,
            points = points
        )
        
        assertEquals(2, annotation.pageNumber)
        assertEquals(3, annotation.points.size)
        assertEquals(Color.BLACK, annotation.strokeColor)
        assertEquals(2f, annotation.strokeWidth, 0.001f)
    }

    @Test
    fun `AnnotationResult success state`() {
        val result = PdfAnnotationManager.AnnotationResult(
            success = true,
            outputPath = "/path/to/annotated.pdf"
        )
        
        assertTrue(result.success)
        assertEquals("/path/to/annotated.pdf", result.outputPath)
        assertNull(result.errorMessage)
    }

    @Test
    fun `AnnotationResult failure state`() {
        val result = PdfAnnotationManager.AnnotationResult(
            success = false,
            errorMessage = "Failed to add annotation"
        )
        
        assertFalse(result.success)
        assertNull(result.outputPath)
        assertEquals("Failed to add annotation", result.errorMessage)
    }

    @Test
    fun `AnnotationType enum values exist`() {
        val types = PdfAnnotationManager.AnnotationType.values()
        assertEquals(6, types.size)
        assertTrue(types.contains(PdfAnnotationManager.AnnotationType.HIGHLIGHT))
        assertTrue(types.contains(PdfAnnotationManager.AnnotationType.UNDERLINE))
        assertTrue(types.contains(PdfAnnotationManager.AnnotationType.STRIKETHROUGH))
        assertTrue(types.contains(PdfAnnotationManager.AnnotationType.TEXT_BOX))
        assertTrue(types.contains(PdfAnnotationManager.AnnotationType.STICKY_NOTE))
        assertTrue(types.contains(PdfAnnotationManager.AnnotationType.FREEHAND))
    }

    @Test
    fun `PdfAnnotationManager can be instantiated`() {
        val manager = PdfAnnotationManager()
        assertNotNull(manager)
    }
}
