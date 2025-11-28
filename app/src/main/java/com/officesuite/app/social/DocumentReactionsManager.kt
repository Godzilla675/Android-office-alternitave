package com.officesuite.app.social

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Document Reactions Manager for adding emoji reactions to documents.
 * Implements Nice-to-Have Feature: Document Reactions from Section 14.
 * 
 * Features:
 * - Add emoji reactions to documents
 * - Track reaction counts
 * - Support for multiple reaction types
 * - User-specific reaction tracking
 */
object DocumentReactionsManager {
    
    private const val PREFS_NAME = "document_reactions_prefs"
    private const val KEY_REACTIONS = "reactions"
    
    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    
    // Cached TypeToken for JSON parsing
    private val reactionsMapType = object : TypeToken<Map<String, DocumentReactions>>() {}.type
    
    /**
     * Available reaction types with emoji and display name.
     */
    enum class ReactionType(val emoji: String, val displayName: String) {
        LIKE("👍", "Like"),
        LOVE("❤️", "Love"),
        CELEBRATE("🎉", "Celebrate"),
        INSIGHTFUL("💡", "Insightful"),
        CURIOUS("🤔", "Curious"),
        HELPFUL("👏", "Helpful"),
        STAR("⭐", "Star"),
        FIRE("🔥", "Fire"),
        CHECK("✅", "Approved"),
        EYES("👀", "Watching")
    }
    
    /**
     * Reaction data for a document.
     */
    data class DocumentReactions(
        val documentUri: String,
        val reactions: Map<String, Int> = emptyMap(),  // ReactionType.name -> count
        val userReactions: Set<String> = emptySet(),   // Reactions added by current user
        val lastReactionTime: Long = 0
    ) {
        /**
         * Gets the total number of reactions on this document.
         */
        fun totalReactions(): Int = reactions.values.sum()
        
        /**
         * Gets the count for a specific reaction type.
         */
        fun getReactionCount(type: ReactionType): Int = reactions[type.name] ?: 0
        
        /**
         * Checks if the user has reacted with a specific type.
         */
        fun hasUserReacted(type: ReactionType): Boolean = userReactions.contains(type.name)
        
        /**
         * Gets the most popular reaction.
         */
        fun mostPopularReaction(): ReactionType? {
            val maxEntry = reactions.maxByOrNull { it.value }
            return maxEntry?.let { 
                try {
                    ReactionType.valueOf(it.key)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    /**
     * Reaction summary for display.
     */
    data class ReactionSummary(
        val topReactions: List<ReactionType>,
        val totalCount: Int,
        val userHasReacted: Boolean
    )
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("DocumentReactionsManager not initialized. Call init() first.")
    }
    
    // ================== Reaction Operations ==================
    
    /**
     * Adds a reaction to a document.
     */
    fun addReaction(documentUri: String, type: ReactionType) {
        val allReactions = getAllReactionsMap().toMutableMap()
        val current = allReactions[documentUri] ?: DocumentReactions(documentUri)
        
        // Check if user already reacted with this type
        if (current.userReactions.contains(type.name)) {
            return // Already reacted
        }
        
        // Add the reaction
        val newReactions = current.reactions.toMutableMap()
        newReactions[type.name] = (newReactions[type.name] ?: 0) + 1
        
        val newUserReactions = current.userReactions.toMutableSet()
        newUserReactions.add(type.name)
        
        allReactions[documentUri] = current.copy(
            reactions = newReactions,
            userReactions = newUserReactions,
            lastReactionTime = System.currentTimeMillis()
        )
        
        saveAllReactions(allReactions)
    }
    
    /**
     * Removes a reaction from a document.
     */
    fun removeReaction(documentUri: String, type: ReactionType) {
        val allReactions = getAllReactionsMap().toMutableMap()
        val current = allReactions[documentUri] ?: return
        
        // Check if user has reacted with this type
        if (!current.userReactions.contains(type.name)) {
            return // Hasn't reacted
        }
        
        // Remove the reaction
        val newReactions = current.reactions.toMutableMap()
        val currentCount = newReactions[type.name] ?: 0
        if (currentCount > 1) {
            newReactions[type.name] = currentCount - 1
        } else {
            newReactions.remove(type.name)
        }
        
        val newUserReactions = current.userReactions.toMutableSet()
        newUserReactions.remove(type.name)
        
        allReactions[documentUri] = current.copy(
            reactions = newReactions,
            userReactions = newUserReactions,
            lastReactionTime = System.currentTimeMillis()
        )
        
        saveAllReactions(allReactions)
    }
    
    /**
     * Toggles a reaction for a document.
     */
    fun toggleReaction(documentUri: String, type: ReactionType) {
        val reactions = getDocumentReactions(documentUri)
        if (reactions?.hasUserReacted(type) == true) {
            removeReaction(documentUri, type)
        } else {
            addReaction(documentUri, type)
        }
    }
    
    /**
     * Gets all reactions for a document.
     */
    fun getDocumentReactions(documentUri: String): DocumentReactions? {
        return getAllReactionsMap()[documentUri]
    }
    
    /**
     * Gets a reaction summary for a document.
     */
    fun getReactionSummary(documentUri: String): ReactionSummary {
        val reactions = getDocumentReactions(documentUri)
        
        if (reactions == null) {
            return ReactionSummary(
                topReactions = emptyList(),
                totalCount = 0,
                userHasReacted = false
            )
        }
        
        // Get top 3 reactions by count
        val topReactions = reactions.reactions
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .mapNotNull { 
                try {
                    ReactionType.valueOf(it.first)
                } catch (e: Exception) {
                    null
                }
            }
        
        return ReactionSummary(
            topReactions = topReactions,
            totalCount = reactions.totalReactions(),
            userHasReacted = reactions.userReactions.isNotEmpty()
        )
    }
    
    /**
     * Gets most reacted documents.
     */
    fun getMostReactedDocuments(limit: Int = 10): List<DocumentReactions> {
        return getAllReactionsMap().values
            .filter { it.totalReactions() > 0 }
            .sortedByDescending { it.totalReactions() }
            .take(limit)
    }
    
    /**
     * Gets recently reacted documents.
     */
    fun getRecentlyReactedDocuments(limit: Int = 10): List<DocumentReactions> {
        return getAllReactionsMap().values
            .filter { it.lastReactionTime > 0 }
            .sortedByDescending { it.lastReactionTime }
            .take(limit)
    }
    
    /**
     * Gets documents the user has reacted to.
     */
    fun getUserReactedDocuments(): List<DocumentReactions> {
        return getAllReactionsMap().values
            .filter { it.userReactions.isNotEmpty() }
            .sortedByDescending { it.lastReactionTime }
    }
    
    /**
     * Clears all reactions for a document.
     */
    fun clearDocumentReactions(documentUri: String) {
        val allReactions = getAllReactionsMap().toMutableMap()
        allReactions.remove(documentUri)
        saveAllReactions(allReactions)
    }
    
    /**
     * Clears all user reactions.
     */
    fun clearAllUserReactions() {
        val allReactions = getAllReactionsMap().toMutableMap()
        allReactions.forEach { (uri, reactions) ->
            // Remove user reactions but keep others
            reactions.userReactions.forEach { reactionName ->
                val currentCount = reactions.reactions[reactionName] ?: 0
                if (currentCount > 0) {
                    val newReactions = reactions.reactions.toMutableMap()
                    if (currentCount > 1) {
                        newReactions[reactionName] = currentCount - 1
                    } else {
                        newReactions.remove(reactionName)
                    }
                    allReactions[uri] = reactions.copy(
                        reactions = newReactions,
                        userReactions = emptySet()
                    )
                }
            }
        }
        saveAllReactions(allReactions)
    }
    
    // ================== Internal Methods ==================
    
    private fun getAllReactionsMap(): Map<String, DocumentReactions> {
        val json = getPrefs().getString(KEY_REACTIONS, null) ?: return emptyMap()
        return try {
            gson.fromJson(json, reactionsMapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun saveAllReactions(reactions: Map<String, DocumentReactions>) {
        getPrefs().edit().putString(KEY_REACTIONS, gson.toJson(reactions)).apply()
    }
    
    /**
     * Formats reactions for display (e.g., "👍 5 ❤️ 3")
     */
    fun formatReactions(documentUri: String): String {
        val reactions = getDocumentReactions(documentUri) ?: return ""
        return reactions.reactions
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .joinToString(" ") { (typeName, count) ->
                try {
                    val type = ReactionType.valueOf(typeName)
                    "${type.emoji} $count"
                } catch (e: Exception) {
                    ""
                }
            }
    }
    
    /**
     * Gets all available reaction types.
     */
    fun getAllReactionTypes(): List<ReactionType> = ReactionType.entries.toList()
    
    /**
     * Resets all reaction data.
     */
    fun resetAllData() {
        getPrefs().edit().clear().apply()
    }
}
