package com.officesuite.app.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * AI-Powered Document Analyzer for Phase 2 features
 * Provides document summarization, statistics, and smart analysis
 */
class DocumentAnalyzer(private val context: Context) {

    companion object {
        private const val AVERAGE_READING_SPEED_WPM = 200
        private const val AVERAGE_SPEAKING_SPEED_WPM = 150
        
        /**
         * Target ratio for summary length relative to original document.
         * Default 0.2 means summary will be ~20% of original length.
         * This can be adjusted based on document type and user preference.
         */
        private const val SUMMARY_RATIO = 0.2
        
        private const val MIN_SUMMARY_SENTENCES = 3
        private const val MAX_SUMMARY_SENTENCES = 10
    }

    /**
     * Document statistics data class
     */
    data class DocumentStatistics(
        val wordCount: Int,
        val characterCount: Int,
        val characterCountNoSpaces: Int,
        val sentenceCount: Int,
        val paragraphCount: Int,
        val readingTimeMinutes: Int,
        val speakingTimeMinutes: Int,
        val averageWordsPerSentence: Double,
        val readabilityScore: Double,
        val readabilityLevel: String,
        val topKeywords: List<String>
    )

    /**
     * Document summary data class
     */
    data class DocumentSummary(
        val summary: String,
        val keyPoints: List<String>,
        val actionItems: List<String>,
        val originalWordCount: Int,
        val summaryWordCount: Int,
        val compressionRatio: Double
    )

    /**
     * Calculate comprehensive document statistics
     */
    suspend fun analyzeDocument(text: String): DocumentStatistics = withContext(Dispatchers.Default) {
        val cleanText = text.trim()
        
        val wordCount = countWords(cleanText)
        val characterCount = cleanText.length
        val characterCountNoSpaces = cleanText.replace("\\s".toRegex(), "").length
        val sentenceCount = countSentences(cleanText)
        val paragraphCount = countParagraphs(cleanText)
        
        val readingTimeMinutes = (wordCount / AVERAGE_READING_SPEED_WPM.toDouble()).let {
            if (it < 1) 1 else it.toInt()
        }
        
        val speakingTimeMinutes = (wordCount / AVERAGE_SPEAKING_SPEED_WPM.toDouble()).let {
            if (it < 1) 1 else it.toInt()
        }
        
        val averageWordsPerSentence = if (sentenceCount > 0) {
            wordCount.toDouble() / sentenceCount
        } else 0.0
        
        val readabilityScore = calculateFleschKincaidScore(cleanText, wordCount, sentenceCount)
        val readabilityLevel = getReadabilityLevel(readabilityScore)
        
        val topKeywords = extractKeywords(cleanText, 10)
        
        DocumentStatistics(
            wordCount = wordCount,
            characterCount = characterCount,
            characterCountNoSpaces = characterCountNoSpaces,
            sentenceCount = sentenceCount,
            paragraphCount = paragraphCount,
            readingTimeMinutes = readingTimeMinutes,
            speakingTimeMinutes = speakingTimeMinutes,
            averageWordsPerSentence = averageWordsPerSentence,
            readabilityScore = readabilityScore,
            readabilityLevel = readabilityLevel,
            topKeywords = topKeywords
        )
    }

    /**
     * Generate document summary using extractive summarization
     */
    suspend fun summarizeDocument(text: String): DocumentSummary = withContext(Dispatchers.Default) {
        val cleanText = text.trim()
        val sentences = extractSentences(cleanText)
        val originalWordCount = countWords(cleanText)
        
        // Score sentences by importance
        val scoredSentences = sentences.mapIndexed { index, sentence ->
            val score = calculateSentenceImportance(sentence, sentences, index)
            Pair(sentence, score)
        }.sortedByDescending { it.second }
        
        // Select top sentences for summary
        val targetSentences = (sentences.size * SUMMARY_RATIO).toInt()
            .coerceIn(MIN_SUMMARY_SENTENCES, MAX_SUMMARY_SENTENCES)
            .coerceAtMost(sentences.size)
        
        val selectedSentences = scoredSentences.take(targetSentences)
            .map { it.first }
        
        // Reorder by original position
        val orderedSummary = selectedSentences
            .sortedBy { sentences.indexOf(it) }
            .joinToString(" ")
        
        // Extract key points
        val keyPoints = extractKeyPoints(cleanText, 5)
        
        // Extract action items
        val actionItems = extractActionItems(cleanText)
        
        val summaryWordCount = countWords(orderedSummary)
        val compressionRatio = if (originalWordCount > 0) {
            1.0 - (summaryWordCount.toDouble() / originalWordCount)
        } else 0.0
        
        DocumentSummary(
            summary = orderedSummary,
            keyPoints = keyPoints,
            actionItems = actionItems,
            originalWordCount = originalWordCount,
            summaryWordCount = summaryWordCount,
            compressionRatio = compressionRatio
        )
    }

    /**
     * Perform smart semantic search across document content
     */
    suspend fun smartSearch(text: String, query: String): List<SearchResult> = withContext(Dispatchers.Default) {
        val results = mutableListOf<SearchResult>()
        val sentences = extractSentences(text)
        val queryWords = query.lowercase().split("\\s+".toRegex())
        
        sentences.forEachIndexed { index, sentence ->
            val score = calculateSemanticScore(sentence, queryWords)
            if (score > 0.0) {
                results.add(SearchResult(
                    text = sentence,
                    score = score,
                    position = index,
                    context = getContextForSentence(sentences, index)
                ))
            }
        }
        
        results.sortedByDescending { it.score }.take(10)
    }

    data class SearchResult(
        val text: String,
        val score: Double,
        val position: Int,
        val context: String
    )

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }

    private fun countSentences(text: String): Int {
        if (text.isBlank()) return 0
        val pattern = Pattern.compile("[.!?]+")
        val matcher = pattern.matcher(text)
        var count = 0
        while (matcher.find()) count++
        return if (count == 0 && text.isNotBlank()) 1 else count
    }

    private fun countParagraphs(text: String): Int {
        if (text.isBlank()) return 0
        return text.split("\n\\s*\n".toRegex()).filter { it.isNotBlank() }.size
    }

    private fun countSyllables(word: String): Int {
        val cleanWord = word.lowercase().replace("[^a-z]".toRegex(), "")
        if (cleanWord.isEmpty()) return 0
        
        var count = 0
        var prevVowel = false
        
        for (char in cleanWord) {
            val isVowel = char in "aeiouy"
            if (isVowel && !prevVowel) count++
            prevVowel = isVowel
        }
        
        // Handle silent 'e'
        if (cleanWord.endsWith("e") && count > 1) count--
        
        return if (count == 0) 1 else count
    }

    private fun calculateFleschKincaidScore(text: String, wordCount: Int, sentenceCount: Int): Double {
        if (wordCount == 0 || sentenceCount == 0) return 0.0
        
        val words = text.split("\\s+".toRegex())
        val totalSyllables = words.sumOf { countSyllables(it) }
        
        val avgSentenceLength = wordCount.toDouble() / sentenceCount
        val avgSyllablesPerWord = totalSyllables.toDouble() / wordCount
        
        // Flesch Reading Ease score
        val score = 206.835 - (1.015 * avgSentenceLength) - (84.6 * avgSyllablesPerWord)
        return score.coerceIn(0.0, 100.0)
    }

    private fun getReadabilityLevel(score: Double): String {
        return when {
            score >= 90 -> "Very Easy (5th grade)"
            score >= 80 -> "Easy (6th grade)"
            score >= 70 -> "Fairly Easy (7th grade)"
            score >= 60 -> "Standard (8th-9th grade)"
            score >= 50 -> "Fairly Difficult (10th-12th grade)"
            score >= 30 -> "Difficult (College)"
            else -> "Very Difficult (Professional)"
        }
    }

    private fun extractSentences(text: String): List<String> {
        return text.split("[.!?]+".toRegex())
            .map { it.trim() }
            .filter { it.isNotBlank() && it.split("\\s+".toRegex()).size >= 3 }
    }

    private fun calculateSentenceImportance(sentence: String, allSentences: List<String>, index: Int): Double {
        var score = 0.0
        val words = sentence.lowercase().split("\\s+".toRegex())
        
        // Position score - first and last sentences are often important
        if (index == 0) score += 2.0
        if (index == allSentences.size - 1) score += 1.0
        
        // Length score - prefer medium-length sentences
        val wordCount = words.size
        score += when {
            wordCount in 10..25 -> 1.5
            wordCount in 6..9 || wordCount in 26..35 -> 1.0
            else -> 0.5
        }
        
        // Important phrase indicators
        val importantPhrases = listOf(
            "important", "key", "main", "significant", "essential",
            "conclude", "summary", "therefore", "result", "finally",
            "first", "second", "primary", "crucial", "critical"
        )
        
        importantPhrases.forEach { phrase ->
            if (sentence.lowercase().contains(phrase)) score += 0.5
        }
        
        // Numeric content often indicates important information
        if (sentence.contains("\\d".toRegex())) score += 0.3
        
        return score
    }

    private fun extractKeywords(text: String, count: Int): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
            "be", "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "can", "need", "dare", "ought",
            "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they",
            "what", "which", "who", "whom", "whose", "where", "when", "why", "how"
        )
        
        val wordFrequency = mutableMapOf<String, Int>()
        val words = text.lowercase().split("[\\s\\p{Punct}]+".toRegex())
        
        words.filter { 
            it.length > 3 && it !in stopWords && it.matches("[a-z]+".toRegex())
        }.forEach { word ->
            wordFrequency[word] = wordFrequency.getOrDefault(word, 0) + 1
        }
        
        return wordFrequency.entries
            .sortedByDescending { it.value }
            .take(count)
            .map { it.key }
    }

    private fun extractKeyPoints(text: String, count: Int): List<String> {
        val sentences = extractSentences(text)
        val keyPointIndicators = listOf(
            "important", "key point", "note that", "remember", "crucial",
            "main", "essential", "significant", "primary", "fundamental"
        )
        
        val keyPoints = sentences.filter { sentence ->
            keyPointIndicators.any { indicator ->
                sentence.lowercase().contains(indicator)
            }
        }.take(count)
        
        // If not enough explicit key points, use top-scored sentences
        return if (keyPoints.size < count) {
            val scoredSentences = sentences.mapIndexed { index, sentence ->
                Pair(sentence, calculateSentenceImportance(sentence, sentences, index))
            }.sortedByDescending { it.second }
            
            (keyPoints + scoredSentences.take(count - keyPoints.size).map { it.first })
                .distinct()
                .take(count)
        } else {
            keyPoints
        }
    }

    private fun extractActionItems(text: String): List<String> {
        val actionPatterns = listOf(
            "(?i)\\b(action item|todo|to-do|task)[:.]?\\s*(.+?)(?=\\.|$)".toRegex(),
            "(?i)\\b(need to|should|must|will|please)\\s+(.+?)(?=\\.|$)".toRegex(),
            "(?i)\\b(follow up|follow-up)\\s+(.+?)(?=\\.|$)".toRegex()
        )
        
        val actionItems = mutableListOf<String>()
        
        actionPatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                val action = match.groupValues.lastOrNull()?.trim()
                if (!action.isNullOrBlank() && action.split("\\s+".toRegex()).size in 3..15) {
                    actionItems.add(action)
                }
            }
        }
        
        return actionItems.distinct().take(5)
    }

    private fun calculateSemanticScore(sentence: String, queryWords: List<String>): Double {
        val sentenceWords = sentence.lowercase().split("\\s+".toRegex())
        var score = 0.0
        var matchCount = 0
        
        queryWords.forEach { queryWord ->
            // Exact match
            if (sentenceWords.contains(queryWord)) {
                score += 1.0
                matchCount++
            }
            // Partial match
            else if (sentenceWords.any { it.contains(queryWord) || queryWord.contains(it) }) {
                score += 0.5
                matchCount++
            }
            // Synonym/related word matching (simplified)
            else if (findRelatedWord(queryWord, sentenceWords)) {
                score += 0.3
                matchCount++
            }
        }
        
        // Normalize by query length
        return if (queryWords.isNotEmpty()) score / queryWords.size else 0.0
    }

    private fun findRelatedWord(queryWord: String, sentenceWords: List<String>): Boolean {
        // Simple synonym mapping for common words
        val synonymMap = mapOf(
            "big" to listOf("large", "huge", "enormous", "massive"),
            "small" to listOf("tiny", "little", "minor", "compact"),
            "good" to listOf("great", "excellent", "fine", "nice"),
            "bad" to listOf("poor", "terrible", "awful", "negative"),
            "important" to listOf("significant", "crucial", "essential", "key"),
            "fast" to listOf("quick", "rapid", "swift", "speedy"),
            "slow" to listOf("gradual", "leisurely", "unhurried"),
            "new" to listOf("recent", "modern", "fresh", "latest"),
            "old" to listOf("ancient", "previous", "former", "past")
        )
        
        val relatedWords = synonymMap[queryWord] ?: return false
        return sentenceWords.any { it in relatedWords }
    }

    private fun getContextForSentence(sentences: List<String>, index: Int): String {
        val prev = if (index > 0) sentences[index - 1] else ""
        val next = if (index < sentences.size - 1) sentences[index + 1] else ""
        return "$prev ... ${sentences[index]} ... $next".trim()
    }
}
