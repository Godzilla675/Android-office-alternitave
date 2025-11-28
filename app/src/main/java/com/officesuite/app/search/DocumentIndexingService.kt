package com.officesuite.app.search

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Document Indexing Service for Background Full-Text Search.
 * Implements Technical Improvements Phase 2 - Section 21: Background Document Indexing
 * 
 * Features:
 * - Full-text indexing for instant search
 * - Incremental updates
 * - Persistent index storage
 * - Concurrent search operations
 */
class DocumentIndexingService(private val context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // In-memory index for fast searches
    private val documentIndex = ConcurrentHashMap<String, IndexedDocument>()
    
    // Word to document mapping for quick lookups
    private val wordIndex = ConcurrentHashMap<String, MutableSet<String>>()
    
    // Indexing state
    private val _indexingState = MutableStateFlow<IndexingState>(IndexingState.Idle)
    val indexingState: StateFlow<IndexingState> = _indexingState
    
    private val _indexStats = MutableStateFlow(IndexStats())
    val indexStats: StateFlow<IndexStats> = _indexStats
    
    init {
        loadIndexFromStorage()
    }
    
    /**
     * Index a document for search.
     */
    suspend fun indexDocument(
        uri: Uri,
        name: String,
        type: DocumentType,
        content: String
    ) = withContext(Dispatchers.IO) {
        _indexingState.value = IndexingState.Indexing(name)
        
        try {
            val documentId = uri.toString()
            val words = tokenizeContent(content)
            val wordFrequency = calculateWordFrequency(words)
            
            val indexedDoc = IndexedDocument(
                id = documentId,
                name = name,
                type = type.name,
                contentPreview = content.take(500),
                wordCount = words.size,
                wordFrequency = wordFrequency,
                indexedAt = System.currentTimeMillis()
            )
            
            // Update document index
            documentIndex[documentId] = indexedDoc
            
            // Update word index for reverse lookups
            words.forEach { word ->
                val normalizedWord = word.lowercase()
                wordIndex.getOrPut(normalizedWord) { ConcurrentHashMap.newKeySet() }
                    .add(documentId)
            }
            
            // Persist to storage
            saveIndexToStorage()
            updateStats()
            
            _indexingState.value = IndexingState.Complete(name)
        } catch (e: Exception) {
            _indexingState.value = IndexingState.Error(e.message ?: "Indexing failed")
        }
    }
    
    /**
     * Search indexed documents.
     */
    fun search(query: String, maxResults: Int = 20): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        
        val queryWords = tokenizeContent(query).map { it.lowercase() }
        val matchingDocs = mutableMapOf<String, Float>()
        
        // Find documents containing query words
        queryWords.forEach { queryWord ->
            // Exact match
            wordIndex[queryWord]?.forEach { docId ->
                matchingDocs[docId] = (matchingDocs[docId] ?: 0f) + 2f
            }
            
            // Prefix match
            wordIndex.keys.filter { it.startsWith(queryWord) }.forEach { word ->
                wordIndex[word]?.forEach { docId ->
                    matchingDocs[docId] = (matchingDocs[docId] ?: 0f) + 1f
                }
            }
        }
        
        // Build search results with relevance scoring
        return matchingDocs
            .mapNotNull { (docId, score) ->
                documentIndex[docId]?.let { doc ->
                    // Calculate TF-IDF-like relevance
                    val termFrequencyScore = queryWords.sumOf { word ->
                        doc.wordFrequency[word.lowercase()] ?: 0
                    }
                    
                    SearchResult(
                        documentId = docId,
                        documentName = doc.name,
                        documentType = DocumentType.valueOf(doc.type),
                        contentPreview = highlightMatches(doc.contentPreview, queryWords),
                        relevanceScore = score + termFrequencyScore.toFloat() * 0.1f
                    )
                }
            }
            .sortedByDescending { it.relevanceScore }
            .take(maxResults)
    }
    
    /**
     * Semantic search using word similarity (basic implementation).
     * This is a foundation for Smart Search feature.
     */
    fun semanticSearch(query: String, maxResults: Int = 20): List<SearchResult> {
        // For now, use basic search with expanded terms
        // In a full implementation, this would use embeddings or ML models
        return search(query, maxResults)
    }
    
    /**
     * Get recently indexed documents.
     */
    fun getRecentlyIndexed(limit: Int = 10): List<IndexedDocument> {
        return documentIndex.values
            .sortedByDescending { it.indexedAt }
            .take(limit)
    }
    
    /**
     * Remove document from index.
     */
    fun removeDocument(uri: Uri) {
        val documentId = uri.toString()
        documentIndex.remove(documentId)
        
        // Remove from word index
        wordIndex.values.forEach { docSet ->
            docSet.remove(documentId)
        }
        
        saveIndexToStorage()
        updateStats()
    }
    
    /**
     * Clear the entire index.
     */
    fun clearIndex() {
        documentIndex.clear()
        wordIndex.clear()
        prefs.edit().clear().apply()
        updateStats()
    }
    
    /**
     * Rebuild index from scratch.
     */
    suspend fun rebuildIndex(documents: List<Pair<Uri, String>>) {
        clearIndex()
        
        documents.forEachIndexed { index, (uri, content) ->
            _indexingState.value = IndexingState.Indexing("${index + 1}/${documents.size}")
            
            val name = FileUtils.getFileName(context, uri)
            val type = FileUtils.getDocumentType(context, uri)
            
            indexDocument(uri, name, type, content)
        }
        
        _indexingState.value = IndexingState.Idle
    }
    
    private fun tokenizeContent(content: String): List<String> {
        return content
            .replace(Regex("[^\\w\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
    }
    
    private fun calculateWordFrequency(words: List<String>): Map<String, Int> {
        return words
            .map { it.lowercase() }
            .groupingBy { it }
            .eachCount()
    }
    
    private fun highlightMatches(text: String, queryWords: List<String>): String {
        var result = text
        queryWords.forEach { word ->
            result = result.replace(
                Regex("(?i)\\b($word\\w*)\\b"),
                "**$1**"
            )
        }
        return result
    }
    
    private fun loadIndexFromStorage() {
        try {
            val indexJson = prefs.getString(KEY_DOCUMENT_INDEX, null)
            if (indexJson != null) {
                val type = object : TypeToken<Map<String, IndexedDocument>>() {}.type
                val storedIndex: Map<String, IndexedDocument> = gson.fromJson(indexJson, type)
                documentIndex.putAll(storedIndex)
                
                // Rebuild word index
                documentIndex.values.forEach { doc ->
                    doc.wordFrequency.keys.forEach { word ->
                        wordIndex.getOrPut(word) { ConcurrentHashMap.newKeySet() }
                            .add(doc.id)
                    }
                }
            }
            updateStats()
        } catch (e: Exception) {
            // Start fresh if loading fails
            documentIndex.clear()
            wordIndex.clear()
        }
    }
    
    private fun saveIndexToStorage() {
        val indexJson = gson.toJson(documentIndex.toMap())
        prefs.edit().putString(KEY_DOCUMENT_INDEX, indexJson).apply()
    }
    
    private fun updateStats() {
        _indexStats.value = IndexStats(
            totalDocuments = documentIndex.size,
            totalWords = wordIndex.size,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    fun shutdown() {
        scope.cancel()
    }
    
    companion object {
        private const val PREFS_NAME = "document_index_prefs"
        private const val KEY_DOCUMENT_INDEX = "document_index"
        
        @Volatile
        private var instance: DocumentIndexingService? = null
        
        fun getInstance(context: Context): DocumentIndexingService {
            return instance ?: synchronized(this) {
                instance ?: DocumentIndexingService(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Represents an indexed document.
 */
data class IndexedDocument(
    val id: String,
    val name: String,
    val type: String,
    val contentPreview: String,
    val wordCount: Int,
    val wordFrequency: Map<String, Int>,
    val indexedAt: Long
)

/**
 * Represents a search result.
 */
data class SearchResult(
    val documentId: String,
    val documentName: String,
    val documentType: DocumentType,
    val contentPreview: String,
    val relevanceScore: Float
)

/**
 * Indexing state.
 */
sealed class IndexingState {
    object Idle : IndexingState()
    data class Indexing(val documentName: String) : IndexingState()
    data class Complete(val documentName: String) : IndexingState()
    data class Error(val message: String) : IndexingState()
}

/**
 * Index statistics.
 */
data class IndexStats(
    val totalDocuments: Int = 0,
    val totalWords: Int = 0,
    val lastUpdated: Long = 0
)
