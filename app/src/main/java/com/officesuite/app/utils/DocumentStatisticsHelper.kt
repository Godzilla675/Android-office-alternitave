package com.officesuite.app.utils

data class DocumentStatistics(
    val wordCount: Int,
    val charCount: Int,
    val charCountNoSpaces: Int,
    val paragraphCount: Int
)

object DocumentStatisticsHelper {
    fun calculateStats(text: String): DocumentStatistics {
        if (text.isEmpty()) {
            return DocumentStatistics(0, 0, 0, 0)
        }

        // Count characters
        val charCount = text.length
        val charCountNoSpaces = text.replace(Regex("\\s"), "").length

        // Count words
        // Split by whitespace and filter out empty strings
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val wordCount = words.size

        // Count paragraphs
        // Assume paragraphs are separated by double newlines or single newlines depending on editor behavior
        // Based on DocxEditorFragment, paragraphs seem to be separated by "\n\n" in extractDocxContent
        // but RichTextEditor seems to use standard EditText which might just use \n
        // We'll count non-empty lines as paragraphs for general purpose
        val paragraphs = text.split("\n").filter { it.trim().isNotEmpty() }
        val paragraphCount = paragraphs.size

        return DocumentStatistics(
            wordCount = wordCount,
            charCount = charCount,
            charCountNoSpaces = charCountNoSpaces,
            paragraphCount = paragraphCount
        )
    }
}
