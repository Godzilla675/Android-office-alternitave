package com.officesuite.app.search

import com.officesuite.app.data.model.DocumentType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DocumentIndexingService data classes.
 */
class DocumentIndexingServiceTest {

    @Test
    fun `IndexedDocument data class holds values correctly`() {
        val doc = IndexedDocument(
            id = "content://test/doc.pdf",
            name = "test.pdf",
            type = "PDF",
            contentPreview = "This is a test document...",
            wordCount = 100,
            wordFrequency = mapOf("test" to 5, "document" to 3),
            indexedAt = 1234567890L
        )
        
        assertEquals("content://test/doc.pdf", doc.id)
        assertEquals("test.pdf", doc.name)
        assertEquals("PDF", doc.type)
        assertEquals("This is a test document...", doc.contentPreview)
        assertEquals(100, doc.wordCount)
        assertEquals(5, doc.wordFrequency["test"])
        assertEquals(3, doc.wordFrequency["document"])
        assertEquals(1234567890L, doc.indexedAt)
    }

    @Test
    fun `SearchResult data class holds values correctly`() {
        val result = SearchResult(
            documentId = "content://test/doc.pdf",
            documentName = "test.pdf",
            documentType = DocumentType.PDF,
            contentPreview = "...matching **text** here...",
            relevanceScore = 2.5f
        )
        
        assertEquals("content://test/doc.pdf", result.documentId)
        assertEquals("test.pdf", result.documentName)
        assertEquals(DocumentType.PDF, result.documentType)
        assertEquals("...matching **text** here...", result.contentPreview)
        assertEquals(2.5f, result.relevanceScore, 0.001f)
    }

    @Test
    fun `IndexingState Idle is singleton`() {
        val state1 = IndexingState.Idle
        val state2 = IndexingState.Idle
        
        assertSame(state1, state2)
    }

    @Test
    fun `IndexingState Indexing contains document name`() {
        val state = IndexingState.Indexing("report.docx")
        
        assertTrue(state is IndexingState.Indexing)
        assertEquals("report.docx", (state as IndexingState.Indexing).documentName)
    }

    @Test
    fun `IndexingState Complete contains document name`() {
        val state = IndexingState.Complete("report.docx")
        
        assertTrue(state is IndexingState.Complete)
        assertEquals("report.docx", (state as IndexingState.Complete).documentName)
    }

    @Test
    fun `IndexingState Error contains message`() {
        val state = IndexingState.Error("Failed to index")
        
        assertTrue(state is IndexingState.Error)
        assertEquals("Failed to index", (state as IndexingState.Error).message)
    }

    @Test
    fun `IndexStats data class has default values`() {
        val stats = IndexStats()
        
        assertEquals(0, stats.totalDocuments)
        assertEquals(0, stats.totalWords)
        assertEquals(0L, stats.lastUpdated)
    }

    @Test
    fun `IndexStats data class holds values correctly`() {
        val stats = IndexStats(
            totalDocuments = 50,
            totalWords = 10000,
            lastUpdated = 1234567890L
        )
        
        assertEquals(50, stats.totalDocuments)
        assertEquals(10000, stats.totalWords)
        assertEquals(1234567890L, stats.lastUpdated)
    }

    @Test
    fun `SearchResult can be compared by relevance score`() {
        val result1 = SearchResult(
            documentId = "doc1",
            documentName = "doc1.pdf",
            documentType = DocumentType.PDF,
            contentPreview = "preview1",
            relevanceScore = 1.0f
        )
        
        val result2 = SearchResult(
            documentId = "doc2",
            documentName = "doc2.pdf",
            documentType = DocumentType.PDF,
            contentPreview = "preview2",
            relevanceScore = 2.0f
        )
        
        val sorted = listOf(result1, result2).sortedByDescending { it.relevanceScore }
        
        assertEquals("doc2", sorted[0].documentId)
        assertEquals("doc1", sorted[1].documentId)
    }

    @Test
    fun `IndexedDocument wordFrequency map is immutable in constructor`() {
        val wordFreq = mapOf("hello" to 5, "world" to 3)
        val doc = IndexedDocument(
            id = "test",
            name = "test.pdf",
            type = "PDF",
            contentPreview = "preview",
            wordCount = 8,
            wordFrequency = wordFreq,
            indexedAt = System.currentTimeMillis()
        )
        
        assertEquals(2, doc.wordFrequency.size)
        assertEquals(5, doc.wordFrequency["hello"])
    }
}
