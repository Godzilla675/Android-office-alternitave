package com.officesuite.app.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FileUtils utility class.
 */
class FileUtilsTest {

    @Test
    fun `getFileExtension returns correct extension for pdf`() {
        val extension = FileUtils.getFileExtension("document.pdf")
        
        assertEquals("pdf", extension)
    }

    @Test
    fun `getFileExtension returns correct extension for docx`() {
        val extension = FileUtils.getFileExtension("report.docx")
        
        assertEquals("docx", extension)
    }

    @Test
    fun `getFileExtension returns correct extension for xlsx`() {
        val extension = FileUtils.getFileExtension("spreadsheet.xlsx")
        
        assertEquals("xlsx", extension)
    }

    @Test
    fun `getFileExtension returns correct extension for pptx`() {
        val extension = FileUtils.getFileExtension("presentation.pptx")
        
        assertEquals("pptx", extension)
    }

    @Test
    fun `getFileExtension returns empty string for filename without extension`() {
        val extension = FileUtils.getFileExtension("filename")
        
        assertEquals("", extension)
    }

    @Test
    fun `getFileExtension returns correct extension for multiple dots`() {
        val extension = FileUtils.getFileExtension("my.file.name.pdf")
        
        assertEquals("pdf", extension)
    }

    @Test
    fun `getFileExtension handles hidden files with extension`() {
        val extension = FileUtils.getFileExtension(".hidden.txt")
        
        assertEquals("txt", extension)
    }

    @Test
    fun `getFileExtension for hidden file returns text after dot as extension`() {
        // For ".hidden", substringAfterLast('.', "") returns "hidden"
        // since it finds the dot and returns everything after it
        val extension = FileUtils.getFileExtension(".hidden")
        
        assertEquals("hidden", extension)
    }

    @Test
    fun `formatFileSize returns correct format for bytes`() {
        val formatted = FileUtils.formatFileSize(512L)
        
        assertEquals("512 B", formatted)
    }

    @Test
    fun `formatFileSize returns correct format for kilobytes`() {
        val formatted = FileUtils.formatFileSize(2048L)
        
        assertEquals("2 KB", formatted)
    }

    @Test
    fun `formatFileSize returns correct format for megabytes`() {
        val formatted = FileUtils.formatFileSize(5 * 1024 * 1024L)
        
        assertEquals("5 MB", formatted)
    }

    @Test
    fun `formatFileSize returns correct format for gigabytes`() {
        val formatted = FileUtils.formatFileSize(3L * 1024 * 1024 * 1024)
        
        assertEquals("3 GB", formatted)
    }

    @Test
    fun `formatFileSize returns correct format for 0 bytes`() {
        val formatted = FileUtils.formatFileSize(0L)
        
        assertEquals("0 B", formatted)
    }

    @Test
    fun `formatFileSize returns correct format for 1 KB boundary`() {
        val formatted = FileUtils.formatFileSize(1024L)
        
        assertEquals("1 KB", formatted)
    }

    @Test
    fun `formatFileSize returns correct format for 1 MB boundary`() {
        val formatted = FileUtils.formatFileSize(1024L * 1024)
        
        assertEquals("1 MB", formatted)
    }

    @Test
    fun `formatFileSize returns correct format for 1 GB boundary`() {
        val formatted = FileUtils.formatFileSize(1024L * 1024 * 1024)
        
        assertEquals("1 GB", formatted)
    }

    @Test
    fun `formatFileSize uses integer division for KB`() {
        // 1500 / 1024 = 1.46 which becomes 1 with integer division
        val formatted = FileUtils.formatFileSize(1500L)
        
        assertEquals("1 KB", formatted)
    }

    @Test
    fun `formatFileSize handles large file sizes`() {
        val formatted = FileUtils.formatFileSize(10L * 1024 * 1024 * 1024)
        
        assertEquals("10 GB", formatted)
    }

    @Test
    fun `getFileExtension is case sensitive for extension`() {
        val extension1 = FileUtils.getFileExtension("file.PDF")
        val extension2 = FileUtils.getFileExtension("file.pdf")
        
        assertEquals("PDF", extension1)
        assertEquals("pdf", extension2)
    }

    @Test
    fun `getFileExtension handles empty string`() {
        val extension = FileUtils.getFileExtension("")
        
        assertEquals("", extension)
    }

    @Test
    fun `getFileExtension handles filename ending with dot`() {
        val extension = FileUtils.getFileExtension("filename.")
        
        assertEquals("", extension)
    }

    @Test
    fun `formatFileSize returns B for values under 1024`() {
        val formatted = FileUtils.formatFileSize(1023L)
        
        assertEquals("1023 B", formatted)
    }
}
