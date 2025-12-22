package com.officesuite.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentStatisticsHelperTest {

    @Test
    fun calculateStats_emptyText_returnsZeros() {
        val stats = DocumentStatisticsHelper.calculateStats("")
        assertEquals(0, stats.wordCount)
        assertEquals(0, stats.charCount)
        assertEquals(0, stats.charCountNoSpaces)
        assertEquals(0, stats.paragraphCount)
    }

    @Test
    fun calculateStats_simpleText_returnsCorrectCounts() {
        val text = "Hello world"
        val stats = DocumentStatisticsHelper.calculateStats(text)
        assertEquals(2, stats.wordCount)
        assertEquals(11, stats.charCount)
        assertEquals(10, stats.charCountNoSpaces) // "Helloworld"
        assertEquals(1, stats.paragraphCount)
    }

    @Test
    fun calculateStats_multilineText_returnsCorrectCounts() {
        val text = "First paragraph.\nSecond paragraph."
        val stats = DocumentStatisticsHelper.calculateStats(text)
        assertEquals(4, stats.wordCount)
        assertEquals(33, stats.charCount) // 16 + 1 + 16 = 33
        assertEquals(29, stats.charCountNoSpaces) // "Firstparagraph.Secondparagraph."
        assertEquals(2, stats.paragraphCount)
    }

    @Test
    fun calculateStats_textWithMultipleSpaces_returnsCorrectCounts() {
        val text = "Hello   world"
        val stats = DocumentStatisticsHelper.calculateStats(text)
        assertEquals(2, stats.wordCount)
        assertEquals(13, stats.charCount)
        assertEquals(10, stats.charCountNoSpaces)
        assertEquals(1, stats.paragraphCount)
    }

    @Test
    fun calculateStats_textWithEmptyLines_returnsCorrectParagraphCounts() {
        val text = "Para 1\n\nPara 2\n"
        val stats = DocumentStatisticsHelper.calculateStats(text)
        assertEquals(4, stats.wordCount)
        assertEquals(15, stats.charCount)
        // "Para 1" (6) + "\n" (1) + "\n" (1) + "Para 2" (6) + "\n" (1) = 15
        assertEquals(10, stats.charCountNoSpaces)
        // Para 1 is non-empty, Para 2 is non-empty. The empty lines should be ignored by the filter
        assertEquals(2, stats.paragraphCount)
    }
}
