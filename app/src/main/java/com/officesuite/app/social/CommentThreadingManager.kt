package com.officesuite.app.social

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Comment Threading Manager for nested comment discussions.
 * Implements Nice-to-Have Features from Section 14:
 * - Comment Threading: Nested comment discussions
 * - @Mentions: Tag collaborators in comments
 * 
 * Features:
 * - Threaded/nested comment replies
 * - @mention support for tagging users
 * - Comment editing and deletion
 * - Reply notifications tracking
 */
object CommentThreadingManager {
    
    private const val PREFS_NAME = "comment_threading_prefs"
    private const val KEY_COMMENTS = "comments"
    private const val KEY_MENTIONS = "mentions"
    private const val KEY_CURRENT_USER = "current_user"
    
    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    
    // Pre-compiled regex patterns for mention extraction
    private val mentionPattern = Regex("""@(\w+)|@\[([^\]]+)\]""")
    private val mentionHighlightPattern = Regex("""(@\w+|@\[[^\]]+\])""")
    
    // Cached TypeTokens for JSON parsing
    private val commentsMapType = object : TypeToken<Map<String, Comment>>() {}.type
    private val mentionsMapType = object : TypeToken<Map<String, MentionNotification>>() {}.type
    
    /**
     * Comment data class with threading support.
     */
    data class Comment(
        val id: String = UUID.randomUUID().toString(),
        val documentUri: String,
        val parentId: String? = null,  // null for top-level comments
        val author: String,
        val authorId: String,
        val text: String,
        val mentions: List<String> = emptyList(),  // List of mentioned user IDs
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val isEdited: Boolean = false,
        val isResolved: Boolean = false,
        val pageNumber: Int? = null,  // Optional page reference
        val x: Float? = null,  // Optional position for annotation-style comments
        val y: Float? = null
    ) {
        /**
         * Checks if this is a reply (has a parent).
         */
        fun isReply(): Boolean = parentId != null
    }
    
    /**
     * Comment thread containing a parent comment and its replies.
     */
    data class CommentThread(
        val parent: Comment,
        val replies: List<Comment> = emptyList()
    ) {
        /**
         * Total number of comments in this thread (parent + replies).
         */
        fun totalComments(): Int = 1 + replies.size
        
        /**
         * Gets all unique authors in this thread.
         */
        fun uniqueAuthors(): Set<String> {
            val authors = mutableSetOf(parent.authorId)
            replies.forEach { authors.add(it.authorId) }
            return authors
        }
    }
    
    /**
     * Mention notification for a user.
     */
    data class MentionNotification(
        val id: String = UUID.randomUUID().toString(),
        val commentId: String,
        val documentUri: String,
        val mentionedUserId: String,
        val mentionedByUserId: String,
        val mentionedByName: String,
        val commentPreview: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isRead: Boolean = false
    )
    
    /**
     * User data for mentions.
     */
    data class User(
        val id: String,
        val name: String,
        val displayName: String,
        val avatarUrl: String? = null
    )
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("CommentThreadingManager not initialized. Call init() first.")
    }
    
    // ================== User Management ==================
    
    /**
     * Sets the current user.
     */
    fun setCurrentUser(user: User) {
        getPrefs().edit().putString(KEY_CURRENT_USER, gson.toJson(user)).apply()
    }
    
    /**
     * Gets the current user.
     */
    fun getCurrentUser(): User? {
        val json = getPrefs().getString(KEY_CURRENT_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    // ================== Comment Operations ==================
    
    /**
     * Adds a top-level comment to a document.
     */
    fun addComment(
        documentUri: String,
        text: String,
        pageNumber: Int? = null,
        x: Float? = null,
        y: Float? = null
    ): Comment? {
        val user = getCurrentUser() ?: return null
        val mentions = extractMentions(text)
        
        val comment = Comment(
            documentUri = documentUri,
            author = user.displayName,
            authorId = user.id,
            text = text,
            mentions = mentions,
            pageNumber = pageNumber,
            x = x,
            y = y
        )
        
        saveComment(comment)
        processMentions(comment, mentions)
        
        return comment
    }
    
    /**
     * Adds a reply to an existing comment.
     */
    fun addReply(
        parentCommentId: String,
        text: String
    ): Comment? {
        val user = getCurrentUser() ?: return null
        val parentComment = getComment(parentCommentId) ?: return null
        val mentions = extractMentions(text)
        
        val reply = Comment(
            documentUri = parentComment.documentUri,
            parentId = parentCommentId,
            author = user.displayName,
            authorId = user.id,
            text = text,
            mentions = mentions
        )
        
        saveComment(reply)
        processMentions(reply, mentions)
        
        return reply
    }
    
    /**
     * Edits a comment.
     */
    fun editComment(commentId: String, newText: String): Comment? {
        val user = getCurrentUser() ?: return null
        val comment = getComment(commentId) ?: return null
        
        // Only the author can edit
        if (comment.authorId != user.id) return null
        
        val mentions = extractMentions(newText)
        val updatedComment = comment.copy(
            text = newText,
            mentions = mentions,
            updatedAt = System.currentTimeMillis(),
            isEdited = true
        )
        
        saveComment(updatedComment)
        
        // Process new mentions
        val newMentions = mentions.filter { !comment.mentions.contains(it) }
        processMentions(updatedComment, newMentions)
        
        return updatedComment
    }
    
    /**
     * Deletes a comment and its replies.
     */
    fun deleteComment(commentId: String): Boolean {
        val user = getCurrentUser() ?: return false
        val comment = getComment(commentId) ?: return false
        
        // Only the author can delete
        if (comment.authorId != user.id) return false
        
        val allComments = getAllCommentsMap().toMutableMap()
        
        // Delete the comment
        allComments.remove(commentId)
        
        // Delete all replies if this is a parent comment
        if (comment.parentId == null) {
            val repliesToDelete = allComments.values.filter { it.parentId == commentId }
            repliesToDelete.forEach { allComments.remove(it.id) }
        }
        
        saveAllComments(allComments)
        return true
    }
    
    /**
     * Resolves a comment thread.
     */
    fun resolveComment(commentId: String): Comment? {
        val comment = getComment(commentId) ?: return null
        val updatedComment = comment.copy(
            isResolved = true,
            updatedAt = System.currentTimeMillis()
        )
        saveComment(updatedComment)
        return updatedComment
    }
    
    /**
     * Unresolves a comment thread.
     */
    fun unresolveComment(commentId: String): Comment? {
        val comment = getComment(commentId) ?: return null
        val updatedComment = comment.copy(
            isResolved = false,
            updatedAt = System.currentTimeMillis()
        )
        saveComment(updatedComment)
        return updatedComment
    }
    
    /**
     * Gets a specific comment.
     */
    fun getComment(commentId: String): Comment? {
        return getAllCommentsMap()[commentId]
    }
    
    /**
     * Gets all comments for a document.
     */
    fun getDocumentComments(documentUri: String): List<Comment> {
        return getAllCommentsMap().values
            .filter { it.documentUri == documentUri }
            .sortedBy { it.createdAt }
    }
    
    /**
     * Gets top-level comments for a document (not replies).
     */
    fun getTopLevelComments(documentUri: String): List<Comment> {
        return getDocumentComments(documentUri)
            .filter { it.parentId == null }
    }
    
    /**
     * Gets replies to a comment.
     */
    fun getReplies(parentCommentId: String): List<Comment> {
        return getAllCommentsMap().values
            .filter { it.parentId == parentCommentId }
            .sortedBy { it.createdAt }
    }
    
    /**
     * Gets a complete comment thread.
     */
    fun getCommentThread(parentCommentId: String): CommentThread? {
        val parent = getComment(parentCommentId) ?: return null
        if (parent.parentId != null) return null // Not a parent comment
        
        val replies = getReplies(parentCommentId)
        return CommentThread(parent, replies)
    }
    
    /**
     * Gets all comment threads for a document.
     */
    fun getDocumentThreads(documentUri: String): List<CommentThread> {
        return getTopLevelComments(documentUri).mapNotNull { parent ->
            getCommentThread(parent.id)
        }
    }
    
    /**
     * Gets unresolved threads for a document.
     */
    fun getUnresolvedThreads(documentUri: String): List<CommentThread> {
        return getDocumentThreads(documentUri).filter { !it.parent.isResolved }
    }
    
    /**
     * Gets resolved threads for a document.
     */
    fun getResolvedThreads(documentUri: String): List<CommentThread> {
        return getDocumentThreads(documentUri).filter { it.parent.isResolved }
    }
    
    /**
     * Gets comments for a specific page.
     */
    fun getPageComments(documentUri: String, pageNumber: Int): List<Comment> {
        return getDocumentComments(documentUri)
            .filter { it.pageNumber == pageNumber }
    }
    
    // ================== Mention Operations ==================
    
    /**
     * Extracts @mentions from text.
     * Mentions are in the format @username or @[Display Name]
     */
    fun extractMentions(text: String): List<String> {
        return mentionPattern.findAll(text).mapNotNull { result ->
            result.groupValues[1].takeIf { it.isNotEmpty() }
                ?: result.groupValues[2].takeIf { it.isNotEmpty() }
        }.toList()
    }
    
    /**
     * Formats text with mentions highlighted.
     * Returns the text with mentions wrapped in a format suitable for display.
     */
    fun formatMentions(text: String): String {
        return mentionHighlightPattern.replace(text) { result ->
            "<mention>${result.value}</mention>"
        }
    }
    
    private fun processMentions(comment: Comment, mentions: List<String>) {
        val user = getCurrentUser() ?: return
        
        val allMentions = getAllMentionsMap().toMutableMap()
        
        mentions.forEach { mentionedUserId ->
            val notification = MentionNotification(
                commentId = comment.id,
                documentUri = comment.documentUri,
                mentionedUserId = mentionedUserId,
                mentionedByUserId = user.id,
                mentionedByName = user.displayName,
                commentPreview = comment.text.take(100)
            )
            allMentions[notification.id] = notification
        }
        
        saveAllMentions(allMentions)
    }
    
    /**
     * Gets unread mentions for a user.
     */
    fun getUnreadMentions(userId: String): List<MentionNotification> {
        return getAllMentionsMap().values
            .filter { it.mentionedUserId == userId && !it.isRead }
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Gets all mentions for a user.
     */
    fun getAllMentions(userId: String): List<MentionNotification> {
        return getAllMentionsMap().values
            .filter { it.mentionedUserId == userId }
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Marks a mention as read.
     */
    fun markMentionAsRead(mentionId: String) {
        val allMentions = getAllMentionsMap().toMutableMap()
        val mention = allMentions[mentionId] ?: return
        allMentions[mentionId] = mention.copy(isRead = true)
        saveAllMentions(allMentions)
    }
    
    /**
     * Marks all mentions for a user as read.
     */
    fun markAllMentionsAsRead(userId: String) {
        val allMentions = getAllMentionsMap().toMutableMap()
        allMentions.forEach { (id, mention) ->
            if (mention.mentionedUserId == userId && !mention.isRead) {
                allMentions[id] = mention.copy(isRead = true)
            }
        }
        saveAllMentions(allMentions)
    }
    
    /**
     * Gets unread mention count for a user.
     */
    fun getUnreadMentionCount(userId: String): Int {
        return getUnreadMentions(userId).size
    }
    
    // ================== Statistics ==================
    
    /**
     * Gets comment count for a document.
     */
    fun getDocumentCommentCount(documentUri: String): Int {
        return getDocumentComments(documentUri).size
    }
    
    /**
     * Gets thread count for a document.
     */
    fun getDocumentThreadCount(documentUri: String): Int {
        return getTopLevelComments(documentUri).size
    }
    
    /**
     * Gets unresolved comment count for a document.
     */
    fun getUnresolvedCount(documentUri: String): Int {
        return getUnresolvedThreads(documentUri).size
    }
    
    // ================== Internal Methods ==================
    
    private fun getAllCommentsMap(): Map<String, Comment> {
        val json = getPrefs().getString(KEY_COMMENTS, null) ?: return emptyMap()
        return try {
            gson.fromJson(json, commentsMapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun saveAllComments(comments: Map<String, Comment>) {
        getPrefs().edit().putString(KEY_COMMENTS, gson.toJson(comments)).apply()
    }
    
    private fun saveComment(comment: Comment) {
        val allComments = getAllCommentsMap().toMutableMap()
        allComments[comment.id] = comment
        saveAllComments(allComments)
    }
    
    private fun getAllMentionsMap(): Map<String, MentionNotification> {
        val json = getPrefs().getString(KEY_MENTIONS, null) ?: return emptyMap()
        return try {
            gson.fromJson(json, mentionsMapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun saveAllMentions(mentions: Map<String, MentionNotification>) {
        getPrefs().edit().putString(KEY_MENTIONS, gson.toJson(mentions)).apply()
    }
    
    /**
     * Clears all comments for a document.
     */
    fun clearDocumentComments(documentUri: String) {
        val allComments = getAllCommentsMap().toMutableMap()
        val toRemove = allComments.values.filter { it.documentUri == documentUri }
        toRemove.forEach { allComments.remove(it.id) }
        saveAllComments(allComments)
    }
    
    /**
     * Resets all comment data.
     */
    fun resetAllData() {
        getPrefs().edit().clear().apply()
    }
}
