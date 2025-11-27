package com.officesuite.app.pdf

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.utils.PdfMerger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manager for PDF page operations including merge, split, reorder, rotate, and delete.
 */
class PdfPageManager {

    /**
     * Result class for page operations
     */
    data class PageOperationResult(
        val success: Boolean,
        val outputPaths: List<String> = emptyList(),
        val errorMessage: String? = null
    )

    /**
     * Merge multiple PDF files into one.
     * 
     * @param inputFiles List of PDF files to merge
     * @param outputFile The output merged PDF file
     * @return PageOperationResult indicating success or failure
     */
    suspend fun mergePdfs(
        inputFiles: List<File>,
        outputFile: File
    ): PageOperationResult = withContext(Dispatchers.IO) {
        try {
            if (inputFiles.isEmpty()) {
                return@withContext PageOperationResult(
                    success = false,
                    errorMessage = "No input files provided"
                )
            }

            val writer = PdfWriter(FileOutputStream(outputFile))
            val outputPdf = PdfDocument(writer)
            val merger = PdfMerger(outputPdf)

            inputFiles.forEach { inputFile ->
                val reader = PdfReader(FileInputStream(inputFile))
                val inputPdf = PdfDocument(reader)
                merger.merge(inputPdf, 1, inputPdf.numberOfPages)
                inputPdf.close()
            }

            outputPdf.close()

            PageOperationResult(
                success = true,
                outputPaths = listOf(outputFile.absolutePath)
            )
        } catch (e: Exception) {
            PageOperationResult(
                success = false,
                errorMessage = e.message ?: "Failed to merge PDFs"
            )
        }
    }

    /**
     * Split a PDF into multiple files (one per page or by page ranges).
     * 
     * @param inputFile The PDF file to split
     * @param outputDir The directory to save split files
     * @param pageRanges Optional page ranges (e.g., listOf(1..3, 5..7)). If null, splits by single pages.
     * @return PageOperationResult with paths to split files
     */
    suspend fun splitPdf(
        inputFile: File,
        outputDir: File,
        pageRanges: List<IntRange>? = null
    ): PageOperationResult = withContext(Dispatchers.IO) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val reader = PdfReader(FileInputStream(inputFile))
            val inputPdf = PdfDocument(reader)
            val baseName = inputFile.nameWithoutExtension
            val outputPaths = mutableListOf<String>()

            if (pageRanges != null) {
                // Split by page ranges
                pageRanges.forEachIndexed { index, range ->
                    val outputFile = File(outputDir, "${baseName}_part${index + 1}.pdf")
                    val writer = PdfWriter(FileOutputStream(outputFile))
                    val outputPdf = PdfDocument(writer)

                    range.forEach { pageNum ->
                        if (pageNum in 1..inputPdf.numberOfPages) {
                            inputPdf.copyPagesTo(pageNum, pageNum, outputPdf)
                        }
                    }

                    outputPdf.close()
                    outputPaths.add(outputFile.absolutePath)
                }
            } else {
                // Split into individual pages
                for (pageNum in 1..inputPdf.numberOfPages) {
                    val outputFile = File(outputDir, "${baseName}_page$pageNum.pdf")
                    val writer = PdfWriter(FileOutputStream(outputFile))
                    val outputPdf = PdfDocument(writer)

                    inputPdf.copyPagesTo(pageNum, pageNum, outputPdf)

                    outputPdf.close()
                    outputPaths.add(outputFile.absolutePath)
                }
            }

            inputPdf.close()

            PageOperationResult(
                success = true,
                outputPaths = outputPaths
            )
        } catch (e: Exception) {
            PageOperationResult(
                success = false,
                errorMessage = e.message ?: "Failed to split PDF"
            )
        }
    }

    /**
     * Extract specific pages from a PDF.
     * 
     * @param inputFile The source PDF file
     * @param outputFile The output PDF file
     * @param pages List of page numbers to extract (1-based)
     * @return PageOperationResult indicating success or failure
     */
    suspend fun extractPages(
        inputFile: File,
        outputFile: File,
        pages: List<Int>
    ): PageOperationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(FileInputStream(inputFile))
            val inputPdf = PdfDocument(reader)
            val writer = PdfWriter(FileOutputStream(outputFile))
            val outputPdf = PdfDocument(writer)

            pages.filter { it in 1..inputPdf.numberOfPages }.forEach { pageNum ->
                inputPdf.copyPagesTo(pageNum, pageNum, outputPdf)
            }

            outputPdf.close()
            inputPdf.close()

            PageOperationResult(
                success = true,
                outputPaths = listOf(outputFile.absolutePath)
            )
        } catch (e: Exception) {
            PageOperationResult(
                success = false,
                errorMessage = e.message ?: "Failed to extract pages"
            )
        }
    }

    /**
     * Reorder pages in a PDF.
     * 
     * @param inputFile The source PDF file
     * @param outputFile The output PDF file
     * @param newOrder New order of pages (1-based, e.g., listOf(3, 1, 2) moves page 3 to first)
     * @return PageOperationResult indicating success or failure
     */
    suspend fun reorderPages(
        inputFile: File,
        outputFile: File,
        newOrder: List<Int>
    ): PageOperationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(FileInputStream(inputFile))
            val inputPdf = PdfDocument(reader)
            val writer = PdfWriter(FileOutputStream(outputFile))
            val outputPdf = PdfDocument(writer)

            newOrder.filter { it in 1..inputPdf.numberOfPages }.forEach { pageNum ->
                inputPdf.copyPagesTo(pageNum, pageNum, outputPdf)
            }

            outputPdf.close()
            inputPdf.close()

            PageOperationResult(
                success = true,
                outputPaths = listOf(outputFile.absolutePath)
            )
        } catch (e: Exception) {
            PageOperationResult(
                success = false,
                errorMessage = e.message ?: "Failed to reorder pages"
            )
        }
    }

    /**
     * Rotate specific pages in a PDF.
     * 
     * @param inputFile The source PDF file
     * @param outputFile The output PDF file
     * @param pageRotations Map of page number to rotation angle (90, 180, 270)
     * @return PageOperationResult indicating success or failure
     */
    suspend fun rotatePages(
        inputFile: File,
        outputFile: File,
        pageRotations: Map<Int, Int>
    ): PageOperationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(FileInputStream(inputFile))
            val writer = PdfWriter(FileOutputStream(outputFile))
            val pdfDoc = PdfDocument(reader, writer)

            pageRotations.forEach { (pageNum, rotation) ->
                if (pageNum in 1..pdfDoc.numberOfPages) {
                    val page = pdfDoc.getPage(pageNum)
                    val currentRotation = page.rotation
                    val newRotation = (currentRotation + rotation) % 360
                    page.setRotation(newRotation)
                }
            }

            pdfDoc.close()

            PageOperationResult(
                success = true,
                outputPaths = listOf(outputFile.absolutePath)
            )
        } catch (e: Exception) {
            PageOperationResult(
                success = false,
                errorMessage = e.message ?: "Failed to rotate pages"
            )
        }
    }

    /**
     * Delete specific pages from a PDF.
     * 
     * @param inputFile The source PDF file
     * @param outputFile The output PDF file
     * @param pagesToDelete List of page numbers to delete (1-based)
     * @return PageOperationResult indicating success or failure
     */
    suspend fun deletePages(
        inputFile: File,
        outputFile: File,
        pagesToDelete: List<Int>
    ): PageOperationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(FileInputStream(inputFile))
            val inputPdf = PdfDocument(reader)
            val writer = PdfWriter(FileOutputStream(outputFile))
            val outputPdf = PdfDocument(writer)

            val pagesToKeep = (1..inputPdf.numberOfPages).filter { it !in pagesToDelete }
            
            pagesToKeep.forEach { pageNum ->
                inputPdf.copyPagesTo(pageNum, pageNum, outputPdf)
            }

            outputPdf.close()
            inputPdf.close()

            PageOperationResult(
                success = true,
                outputPaths = listOf(outputFile.absolutePath)
            )
        } catch (e: Exception) {
            PageOperationResult(
                success = false,
                errorMessage = e.message ?: "Failed to delete pages"
            )
        }
    }

    /**
     * Get the number of pages in a PDF.
     * 
     * @param file The PDF file
     * @return Number of pages, or -1 if error
     */
    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(FileInputStream(file))
            val pdfDoc = PdfDocument(reader)
            val count = pdfDoc.numberOfPages
            pdfDoc.close()
            count
        } catch (e: Exception) {
            -1
        }
    }
}
