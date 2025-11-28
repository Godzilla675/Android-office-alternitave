package com.officesuite.app.data

import android.net.Uri
import com.officesuite.app.data.model.DocumentFile
import com.officesuite.app.data.model.DocumentType
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Unit tests for DocumentType and DocumentFile data classes.
 */
class DocumentTypeTest {

    @Test
    fun `DocumentType PDF has correct extension and mimeType`() {
        val type = DocumentType.PDF
        
        assertEquals("pdf", type.extension)
        assertEquals("application/pdf", type.mimeType)
    }

    @Test
    fun `DocumentType DOCX has correct extension and mimeType`() {
        val type = DocumentType.DOCX
        
        assertEquals("docx", type.extension)
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", type.mimeType)
    }

    @Test
    fun `DocumentType DOC has correct extension and mimeType`() {
        val type = DocumentType.DOC
        
        assertEquals("doc", type.extension)
        assertEquals("application/msword", type.mimeType)
    }

    @Test
    fun `DocumentType PPTX has correct extension and mimeType`() {
        val type = DocumentType.PPTX
        
        assertEquals("pptx", type.extension)
        assertEquals("application/vnd.openxmlformats-officedocument.presentationml.presentation", type.mimeType)
    }

    @Test
    fun `DocumentType PPT has correct extension and mimeType`() {
        val type = DocumentType.PPT
        
        assertEquals("ppt", type.extension)
        assertEquals("application/vnd.ms-powerpoint", type.mimeType)
    }

    @Test
    fun `DocumentType XLSX has correct extension and mimeType`() {
        val type = DocumentType.XLSX
        
        assertEquals("xlsx", type.extension)
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", type.mimeType)
    }

    @Test
    fun `DocumentType XLS has correct extension and mimeType`() {
        val type = DocumentType.XLS
        
        assertEquals("xls", type.extension)
        assertEquals("application/vnd.ms-excel", type.mimeType)
    }

    @Test
    fun `DocumentType MARKDOWN has correct extension and mimeType`() {
        val type = DocumentType.MARKDOWN
        
        assertEquals("md", type.extension)
        assertEquals("text/markdown", type.mimeType)
    }

    @Test
    fun `DocumentType TXT has correct extension and mimeType`() {
        val type = DocumentType.TXT
        
        assertEquals("txt", type.extension)
        assertEquals("text/plain", type.mimeType)
    }

    @Test
    fun `DocumentType CSV has correct extension and mimeType`() {
        val type = DocumentType.CSV
        
        assertEquals("csv", type.extension)
        assertEquals("text/csv", type.mimeType)
    }

    @Test
    fun `DocumentType JSON has correct extension and mimeType`() {
        val type = DocumentType.JSON
        
        assertEquals("json", type.extension)
        assertEquals("application/json", type.mimeType)
    }

    @Test
    fun `DocumentType XML has correct extension and mimeType`() {
        val type = DocumentType.XML
        
        assertEquals("xml", type.extension)
        assertEquals("application/xml", type.mimeType)
    }

    @Test
    fun `DocumentType UNKNOWN has empty extension`() {
        val type = DocumentType.UNKNOWN
        
        assertEquals("", type.extension)
        assertEquals("application/octet-stream", type.mimeType)
    }

    @Test
    fun `fromExtension returns correct type for pdf`() {
        val type = DocumentType.fromExtension("pdf")
        
        assertEquals(DocumentType.PDF, type)
    }

    @Test
    fun `fromExtension returns correct type for PDF uppercase`() {
        val type = DocumentType.fromExtension("PDF")
        
        assertEquals(DocumentType.PDF, type)
    }

    @Test
    fun `fromExtension returns correct type for docx`() {
        val type = DocumentType.fromExtension("docx")
        
        assertEquals(DocumentType.DOCX, type)
    }

    @Test
    fun `fromExtension returns correct type for xlsx`() {
        val type = DocumentType.fromExtension("xlsx")
        
        assertEquals(DocumentType.XLSX, type)
    }

    @Test
    fun `fromExtension returns correct type for pptx`() {
        val type = DocumentType.fromExtension("pptx")
        
        assertEquals(DocumentType.PPTX, type)
    }

    @Test
    fun `fromExtension returns correct type for md`() {
        val type = DocumentType.fromExtension("md")
        
        assertEquals(DocumentType.MARKDOWN, type)
    }

    @Test
    fun `fromExtension returns UNKNOWN for unsupported extension`() {
        val type = DocumentType.fromExtension("xyz")
        
        assertEquals(DocumentType.UNKNOWN, type)
    }

    @Test
    fun `fromExtension returns UNKNOWN for empty string`() {
        val type = DocumentType.fromExtension("")
        
        assertEquals(DocumentType.UNKNOWN, type)
    }

    @Test
    fun `fromMimeType returns correct type for application pdf`() {
        val type = DocumentType.fromMimeType("application/pdf")
        
        assertEquals(DocumentType.PDF, type)
    }

    @Test
    fun `fromMimeType returns correct type for docx mime type`() {
        val type = DocumentType.fromMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        
        assertEquals(DocumentType.DOCX, type)
    }

    @Test
    fun `fromMimeType returns correct type for xlsx mime type`() {
        val type = DocumentType.fromMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        
        assertEquals(DocumentType.XLSX, type)
    }

    @Test
    fun `fromMimeType returns correct type for pptx mime type`() {
        val type = DocumentType.fromMimeType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
        
        assertEquals(DocumentType.PPTX, type)
    }

    @Test
    fun `fromMimeType returns correct type for text plain`() {
        val type = DocumentType.fromMimeType("text/plain")
        
        assertEquals(DocumentType.TXT, type)
    }

    @Test
    fun `fromMimeType returns correct type for text markdown`() {
        val type = DocumentType.fromMimeType("text/markdown")
        
        assertEquals(DocumentType.MARKDOWN, type)
    }

    @Test
    fun `fromMimeType returns UNKNOWN for unsupported mime type`() {
        val type = DocumentType.fromMimeType("video/mp4")
        
        assertEquals(DocumentType.UNKNOWN, type)
    }

    @Test
    fun `fromMimeType is case insensitive`() {
        val type = DocumentType.fromMimeType("APPLICATION/PDF")
        
        assertEquals(DocumentType.PDF, type)
    }

    @Test
    fun `DocumentType enum has expected number of values`() {
        val types = DocumentType.values()
        
        assertEquals(13, types.size)
    }

    @Test
    fun `DocumentFile data class holds values correctly`() {
        val mockUri = mock(Uri::class.java)
        val file = DocumentFile(
            uri = mockUri,
            name = "test.pdf",
            type = DocumentType.PDF,
            size = 1024L,
            lastModified = System.currentTimeMillis(),
            path = "/documents/test.pdf"
        )
        
        assertEquals(mockUri, file.uri)
        assertEquals("test.pdf", file.name)
        assertEquals(DocumentType.PDF, file.type)
        assertEquals(1024L, file.size)
        assertEquals("/documents/test.pdf", file.path)
    }

    @Test
    fun `DocumentFile with null path`() {
        val mockUri = mock(Uri::class.java)
        val file = DocumentFile(
            uri = mockUri,
            name = "document.docx",
            type = DocumentType.DOCX,
            size = 2048L,
            lastModified = 1234567890L
        )
        
        assertNull(file.path)
    }

    @Test
    fun `DocumentFile equality based on all fields`() {
        val mockUri = mock(Uri::class.java)
        val file1 = DocumentFile(
            uri = mockUri,
            name = "test.pdf",
            type = DocumentType.PDF,
            size = 1024L,
            lastModified = 1234567890L,
            path = "/test.pdf"
        )
        val file2 = DocumentFile(
            uri = mockUri,
            name = "test.pdf",
            type = DocumentType.PDF,
            size = 1024L,
            lastModified = 1234567890L,
            path = "/test.pdf"
        )
        
        assertEquals(file1, file2)
    }

    @Test
    fun `All DocumentType extensions are unique`() {
        val types = DocumentType.values().filter { it != DocumentType.UNKNOWN }
        val extensions = types.map { it.extension }
        val uniqueExtensions = extensions.toSet()
        
        assertEquals(extensions.size, uniqueExtensions.size)
    }

    @Test
    fun `All DocumentType mimeTypes are unique`() {
        val types = DocumentType.values()
        val mimeTypes = types.map { it.mimeType }
        val uniqueMimeTypes = mimeTypes.toSet()
        
        assertEquals(mimeTypes.size, uniqueMimeTypes.size)
    }
}
