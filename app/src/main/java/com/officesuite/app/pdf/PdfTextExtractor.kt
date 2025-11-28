package com.officesuite.app.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor as ITextPdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utility class for extracting text from PDF documents.
 * Supports:
 * - Full document text extraction
 * - Per-page text extraction
 * - Text search with page results
 */
class PdfTextExtractor {

    data class SearchResult(
        val pageNumber: Int,
        val matchCount: Int,
        val contextSnippet: String
    )

    data class TextExtractionResult(
        val success: Boolean,
        val text: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Extract all text from a PDF document
     */
    suspend fun extractAllText(pdfFile: File): TextExtractionResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(pdfFile)
            val pdfDoc = PdfDocument(reader)
            val allText = StringBuilder()
            
            for (i in 1..pdfDoc.numberOfPages) {
                val page = pdfDoc.getPage(i)
                val strategy = LocationTextExtractionStrategy()
                val pageText = ITextPdfTextExtractor.getTextFromPage(page, strategy)
                allText.append("--- Page $i ---\n")
                allText.append(pageText)
                allText.append("\n\n")
            }
            
            pdfDoc.close()
            TextExtractionResult(success = true, text = allText.toString())
        } catch (e: Exception) {
            TextExtractionResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * Extract text from a specific page
     */
    suspend fun extractTextFromPage(pdfFile: File, pageNumber: Int): TextExtractionResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(pdfFile)
            val pdfDoc = PdfDocument(reader)
            
            if (pageNumber < 1 || pageNumber > pdfDoc.numberOfPages) {
                pdfDoc.close()
                return@withContext TextExtractionResult(success = false, errorMessage = "Invalid page number")
            }
            
            val page = pdfDoc.getPage(pageNumber)
            val strategy = LocationTextExtractionStrategy()
            val pageText = ITextPdfTextExtractor.getTextFromPage(page, strategy)
            
            pdfDoc.close()
            TextExtractionResult(success = true, text = pageText)
        } catch (e: Exception) {
            TextExtractionResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * Search for text in PDF and return matching pages with context
     */
    suspend fun searchText(pdfFile: File, query: String, caseSensitive: Boolean = false): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        
        try {
            val reader = PdfReader(pdfFile)
            val pdfDoc = PdfDocument(reader)
            val searchQuery = if (caseSensitive) query else query.lowercase()
            
            for (i in 1..pdfDoc.numberOfPages) {
                val page = pdfDoc.getPage(i)
                val strategy = LocationTextExtractionStrategy()
                val pageText = ITextPdfTextExtractor.getTextFromPage(page, strategy)
                val searchText = if (caseSensitive) pageText else pageText.lowercase()
                
                // Count matches
                var matchCount = 0
                var lastIndex = 0
                while (true) {
                    val index = searchText.indexOf(searchQuery, lastIndex)
                    if (index < 0) break
                    matchCount++
                    lastIndex = index + 1
                }
                
                if (matchCount > 0) {
                    // Extract context snippet
                    val firstIndex = searchText.indexOf(searchQuery)
                    val snippetStart = maxOf(0, firstIndex - 30)
                    val snippetEnd = minOf(pageText.length, firstIndex + query.length + 30)
                    val snippet = pageText.substring(snippetStart, snippetEnd).trim()
                        .replace("\n", " ")
                        .let { if (snippetStart > 0) "...$it" else it }
                        .let { if (snippetEnd < pageText.length) "$it..." else it }
                    
                    results.add(SearchResult(
                        pageNumber = i,
                        matchCount = matchCount,
                        contextSnippet = snippet
                    ))
                }
            }
            
            pdfDoc.close()
        } catch (e: Exception) {
            // Return empty results on search failure
            // Error is logged for debugging purposes
            android.util.Log.e("PdfTextExtractor", "Search failed: ${e.message}", e)
        }
        
        results
    }

    /**
     * Get page count of a PDF document
     */
    suspend fun getPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(pdfFile)
            val pdfDoc = PdfDocument(reader)
            val count = pdfDoc.numberOfPages
            pdfDoc.close()
            count
        } catch (e: Exception) {
            0
        }
    }
}
