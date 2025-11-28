package com.officesuite.app.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import android.content.Context

/**
 * Unit tests for DocumentAnalyzer class.
 */
class DocumentAnalyzerTest {

    private lateinit var analyzer: DocumentAnalyzer
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        analyzer = DocumentAnalyzer(mockContext)
    }

    @Test
    fun `analyzeDocument returns correct word count for simple text`() = runBlocking {
        val text = "Hello world this is a test document."
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(7, stats.wordCount)
    }

    @Test
    fun `analyzeDocument returns correct character count`() = runBlocking {
        val text = "Hello world"
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(11, stats.characterCount)
    }

    @Test
    fun `analyzeDocument returns correct character count without spaces`() = runBlocking {
        val text = "Hello world"
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(10, stats.characterCountNoSpaces)
    }

    @Test
    fun `analyzeDocument returns correct sentence count`() = runBlocking {
        val text = "This is sentence one. This is sentence two! Is this sentence three?"
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(3, stats.sentenceCount)
    }

    @Test
    fun `analyzeDocument returns correct paragraph count`() = runBlocking {
        val text = "First paragraph here.\n\nSecond paragraph here.\n\nThird paragraph."
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(3, stats.paragraphCount)
    }

    @Test
    fun `analyzeDocument handles empty text`() = runBlocking {
        val text = ""
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(0, stats.wordCount)
        assertEquals(0, stats.characterCount)
    }

    @Test
    fun `analyzeDocument handles whitespace only text`() = runBlocking {
        val text = "   \n\t  "
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(0, stats.wordCount)
    }

    @Test
    fun `analyzeDocument calculates reading time correctly`() = runBlocking {
        // 200 words should take 1 minute to read at 200 WPM
        val words = (1..200).joinToString(" ") { "word" }
        val stats = analyzer.analyzeDocument(words)
        
        assertEquals(1, stats.readingTimeMinutes)
    }

    @Test
    fun `analyzeDocument calculates speaking time correctly`() = runBlocking {
        // 150 words should take 1 minute to speak at 150 WPM
        val words = (1..150).joinToString(" ") { "word" }
        val stats = analyzer.analyzeDocument(words)
        
        assertEquals(1, stats.speakingTimeMinutes)
    }

    @Test
    fun `analyzeDocument returns minimum reading time of 1 minute for short text`() = runBlocking {
        val text = "Hello world"
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(1, stats.readingTimeMinutes)
    }

    @Test
    fun `analyzeDocument extracts keywords from text`() = runBlocking {
        val text = "technology technology technology software development software code code programming"
        val stats = analyzer.analyzeDocument(text)
        
        assertTrue(stats.topKeywords.contains("technology"))
    }

    @Test
    fun `analyzeDocument calculates average words per sentence`() = runBlocking {
        val text = "One two three. Four five six."
        val stats = analyzer.analyzeDocument(text)
        
        assertEquals(3.0, stats.averageWordsPerSentence, 0.1)
    }

    @Test
    fun `analyzeDocument returns readability score between 0 and 100`() = runBlocking {
        val text = "This is a simple text. It is easy to read. Anyone can understand it."
        val stats = analyzer.analyzeDocument(text)
        
        assertTrue(stats.readabilityScore in 0.0..100.0)
    }

    @Test
    fun `analyzeDocument returns non-empty readability level`() = runBlocking {
        val text = "This is a test document with some words."
        val stats = analyzer.analyzeDocument(text)
        
        assertTrue(stats.readabilityLevel.isNotEmpty())
    }

    @Test
    fun `summarizeDocument returns valid summary`() = runBlocking {
        val text = """
            This is an important document about technology. Technology is crucial for modern businesses.
            Companies need to invest in software development. Software helps automate many tasks.
            The future of work depends on technological advancement. Innovation drives economic growth.
            We must embrace digital transformation. Digital tools improve productivity significantly.
        """.trimIndent()
        
        val summary = analyzer.summarizeDocument(text)
        
        assertTrue(summary.summary.isNotEmpty())
        assertTrue(summary.originalWordCount > 0)
    }

    @Test
    fun `summarizeDocument calculates compression ratio`() = runBlocking {
        val text = """
            First important point here. This is significant content.
            Second important point follows. This is also essential.
            Third important point arrives. This cannot be overlooked.
            Fourth important point emerges. This is critical.
        """.trimIndent()
        
        val summary = analyzer.summarizeDocument(text)
        
        assertTrue(summary.compressionRatio >= 0.0)
        assertTrue(summary.compressionRatio <= 1.0)
    }

    @Test
    fun `smartSearch finds matching sentences`() = runBlocking {
        val text = "Kotlin is a programming language. Java is also a language. Python is popular."
        val query = "Kotlin language"
        
        val results = analyzer.smartSearch(text, query)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.text.contains("Kotlin") })
    }

    @Test
    fun `smartSearch returns empty list for non-matching query`() = runBlocking {
        val text = "The quick brown fox jumps over the lazy dog."
        val query = "quantum physics"
        
        val results = analyzer.smartSearch(text, query)
        
        assertTrue(results.isEmpty())
    }

    @Test
    fun `smartSearch results are sorted by score`() = runBlocking {
        val text = "Kotlin programming is great. Java programming also works. Kotlin is modern."
        val query = "Kotlin"
        
        val results = analyzer.smartSearch(text, query)
        
        if (results.size > 1) {
            for (i in 0 until results.size - 1) {
                assertTrue(results[i].score >= results[i + 1].score)
            }
        }
    }

    @Test
    fun `DocumentStatistics data class holds values correctly`() {
        val stats = DocumentAnalyzer.DocumentStatistics(
            wordCount = 100,
            characterCount = 500,
            characterCountNoSpaces = 450,
            sentenceCount = 10,
            paragraphCount = 3,
            readingTimeMinutes = 1,
            speakingTimeMinutes = 1,
            averageWordsPerSentence = 10.0,
            readabilityScore = 65.0,
            readabilityLevel = "Standard",
            topKeywords = listOf("test", "document")
        )
        
        assertEquals(100, stats.wordCount)
        assertEquals(500, stats.characterCount)
        assertEquals(10, stats.sentenceCount)
        assertEquals("Standard", stats.readabilityLevel)
    }

    @Test
    fun `DocumentSummary data class holds values correctly`() {
        val summary = DocumentAnalyzer.DocumentSummary(
            summary = "This is a summary",
            keyPoints = listOf("Point 1", "Point 2"),
            actionItems = listOf("Do this", "Do that"),
            originalWordCount = 100,
            summaryWordCount = 20,
            compressionRatio = 0.8
        )
        
        assertEquals("This is a summary", summary.summary)
        assertEquals(2, summary.keyPoints.size)
        assertEquals(2, summary.actionItems.size)
        assertEquals(0.8, summary.compressionRatio, 0.01)
    }

    @Test
    fun `SearchResult data class holds values correctly`() {
        val result = DocumentAnalyzer.SearchResult(
            text = "Found text",
            score = 0.85,
            position = 5,
            context = "Some context here"
        )
        
        assertEquals("Found text", result.text)
        assertEquals(0.85, result.score, 0.01)
        assertEquals(5, result.position)
    }
}
