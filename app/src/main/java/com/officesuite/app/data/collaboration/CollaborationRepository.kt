package com.officesuite.app.data.collaboration

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import java.util.Base64

/**
 * Repository for managing collaboration features like comments, version history, and sharing
 */
class CollaborationRepository(private val context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "collaboration_prefs", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_COMMENTS = "document_comments"
        private const val KEY_VERSIONS = "document_versions"
        private const val KEY_SHARE_LINKS = "share_links"
        private const val KEY_CHANGES = "document_changes"
        private const val KEY_CURRENT_USER = "current_user"
    }

    // ============== USER ==============

    fun setCurrentUser(userName: String) {
        prefs.edit().putString(KEY_CURRENT_USER, userName).apply()
    }

    fun getCurrentUser(): String {
        return prefs.getString(KEY_CURRENT_USER, "User") ?: "User"
    }

    // ============== COMMENTS ==============

    /**
     * Add a comment to a document
     */
    suspend fun addComment(comment: DocumentComment): Boolean = withContext(Dispatchers.IO) {
        try {
            val comments = getAllComments().toMutableList()
            comments.add(comment)
            saveComments(comments)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get comments for a specific document
     */
    suspend fun getCommentsForDocument(documentId: String): List<DocumentComment> = withContext(Dispatchers.IO) {
        getAllComments().filter { it.documentId == documentId }
    }

    /**
     * Get comments for a specific page
     */
    suspend fun getCommentsForPage(documentId: String, pageNumber: Int): List<DocumentComment> {
        return getCommentsForDocument(documentId).filter { it.pageNumber == pageNumber }
    }

    /**
     * Update a comment
     */
    suspend fun updateComment(commentId: String, newText: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val comments = getAllComments().toMutableList()
            val index = comments.indexOfFirst { it.id == commentId }
            if (index >= 0) {
                comments[index] = comments[index].copy(
                    text = newText,
                    updatedAt = System.currentTimeMillis()
                )
                saveComments(comments)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a comment
     */
    suspend fun deleteComment(commentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val comments = getAllComments().toMutableList()
            comments.removeIf { it.id == commentId || it.parentId == commentId }
            saveComments(comments)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Resolve a comment
     */
    suspend fun resolveComment(commentId: String, resolved: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val comments = getAllComments().toMutableList()
            val index = comments.indexOfFirst { it.id == commentId }
            if (index >= 0) {
                comments[index] = comments[index].copy(resolved = resolved)
                saveComments(comments)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reply to a comment
     */
    suspend fun replyToComment(parentCommentId: String, replyText: String): Boolean {
        val parentComment = getAllComments().find { it.id == parentCommentId } ?: return false
        val reply = DocumentComment(
            documentId = parentComment.documentId,
            text = replyText,
            author = getCurrentUser(),
            pageNumber = parentComment.pageNumber,
            parentId = parentCommentId
        )
        return addComment(reply)
    }

    private fun getAllComments(): List<DocumentComment> {
        val json = prefs.getString(KEY_COMMENTS, "[]") ?: "[]"
        val type = object : TypeToken<List<DocumentComment>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveComments(comments: List<DocumentComment>) {
        prefs.edit().putString(KEY_COMMENTS, gson.toJson(comments)).apply()
    }

    // ============== VERSION HISTORY ==============

    /**
     * Save a new version of a document
     */
    suspend fun saveVersion(
        documentId: String,
        sourceFile: File,
        description: String? = null
    ): DocumentVersion? = withContext(Dispatchers.IO) {
        try {
            val versions = getVersionsForDocument(documentId)
            val newVersionNumber = (versions.maxOfOrNull { it.versionNumber } ?: 0) + 1
            
            // Create version backup directory
            val versionDir = File(context.filesDir, "versions/$documentId")
            if (!versionDir.exists()) {
                versionDir.mkdirs()
            }
            
            // Copy file to version storage
            val versionFile = File(versionDir, "v${newVersionNumber}_${sourceFile.name}")
            sourceFile.copyTo(versionFile, overwrite = true)
            
            val version = DocumentVersion(
                documentId = documentId,
                versionNumber = newVersionNumber,
                name = "Version $newVersionNumber",
                description = description,
                author = getCurrentUser(),
                filePath = versionFile.absolutePath,
                fileSize = versionFile.length()
            )
            
            val allVersions = getAllVersions().toMutableList()
            allVersions.add(version)
            saveVersions(allVersions)
            
            version
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get version history for a document
     */
    suspend fun getVersionsForDocument(documentId: String): List<DocumentVersion> = withContext(Dispatchers.IO) {
        getAllVersions()
            .filter { it.documentId == documentId }
            .sortedByDescending { it.versionNumber }
    }

    /**
     * Get a specific version
     */
    suspend fun getVersion(versionId: String): DocumentVersion? = withContext(Dispatchers.IO) {
        getAllVersions().find { it.id == versionId }
    }

    /**
     * Restore a document to a previous version
     */
    suspend fun restoreVersion(versionId: String, targetPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val version = getVersion(versionId) ?: return@withContext false
            val versionFile = File(version.filePath)
            if (!versionFile.exists()) return@withContext false
            
            versionFile.copyTo(File(targetPath), overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a version
     */
    suspend fun deleteVersion(versionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val version = getVersion(versionId) ?: return@withContext false
            
            // Delete version file
            val versionFile = File(version.filePath)
            if (versionFile.exists()) {
                versionFile.delete()
            }
            
            // Remove from list
            val versions = getAllVersions().toMutableList()
            versions.removeIf { it.id == versionId }
            saveVersions(versions)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getAllVersions(): List<DocumentVersion> {
        val json = prefs.getString(KEY_VERSIONS, "[]") ?: "[]"
        val type = object : TypeToken<List<DocumentVersion>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveVersions(versions: List<DocumentVersion>) {
        prefs.edit().putString(KEY_VERSIONS, gson.toJson(versions)).apply()
    }

    // ============== SHARE LINKS ==============

    /**
     * Create a share link for a document
     */
    suspend fun createShareLink(
        documentId: String,
        permissions: SharePermissions = SharePermissions(),
        expiresInHours: Int? = null,
        password: String? = null,
        maxAccesses: Int? = null
    ): ShareLink = withContext(Dispatchers.IO) {
        val expiresAt = expiresInHours?.let {
            System.currentTimeMillis() + (it * 60 * 60 * 1000L)
        }
        
        val shareLink = ShareLink(
            documentId = documentId,
            link = generateShareLinkId(),
            permissions = permissions,
            expiresAt = expiresAt,
            password = password,
            maxAccesses = maxAccesses
        )
        
        val links = getAllShareLinks().toMutableList()
        links.add(shareLink)
        saveShareLinks(links)
        
        shareLink
    }

    /**
     * Get share links for a document
     */
    suspend fun getShareLinksForDocument(documentId: String): List<ShareLink> = withContext(Dispatchers.IO) {
        getAllShareLinks().filter { it.documentId == documentId && it.isActive }
    }

    /**
     * Validate a share link
     */
    suspend fun validateShareLink(linkId: String, password: String? = null): ShareLink? = withContext(Dispatchers.IO) {
        val link = getAllShareLinks().find { it.link == linkId } ?: return@withContext null
        
        // Check if link is active
        if (!link.isActive) return@withContext null
        
        // Check expiration
        if (link.expiresAt != null && System.currentTimeMillis() > link.expiresAt) {
            deactivateShareLink(link.id)
            return@withContext null
        }
        
        // Check max accesses
        if (link.maxAccesses != null && link.accessCount >= link.maxAccesses) {
            return@withContext null
        }
        
        // Check password
        if (link.password != null && link.password != password) {
            return@withContext null
        }
        
        // Increment access count
        incrementShareLinkAccess(link.id)
        
        link
    }

    /**
     * Deactivate a share link
     */
    suspend fun deactivateShareLink(linkId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val links = getAllShareLinks().toMutableList()
            val index = links.indexOfFirst { it.id == linkId }
            if (index >= 0) {
                links[index] = links[index].copy(isActive = false)
                saveShareLinks(links)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update share link permissions
     */
    suspend fun updateShareLinkPermissions(linkId: String, permissions: SharePermissions): Boolean = withContext(Dispatchers.IO) {
        try {
            val links = getAllShareLinks().toMutableList()
            val index = links.indexOfFirst { it.id == linkId }
            if (index >= 0) {
                links[index] = links[index].copy(permissions = permissions)
                saveShareLinks(links)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun incrementShareLinkAccess(linkId: String) = withContext(Dispatchers.IO) {
        val links = getAllShareLinks().toMutableList()
        val index = links.indexOfFirst { it.id == linkId }
        if (index >= 0) {
            links[index] = links[index].copy(accessCount = links[index].accessCount + 1)
            saveShareLinks(links)
        }
    }

    private fun generateShareLinkId(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun getAllShareLinks(): List<ShareLink> {
        val json = prefs.getString(KEY_SHARE_LINKS, "[]") ?: "[]"
        val type = object : TypeToken<List<ShareLink>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveShareLinks(links: List<ShareLink>) {
        prefs.edit().putString(KEY_SHARE_LINKS, gson.toJson(links)).apply()
    }

    // ============== TRACK CHANGES ==============

    /**
     * Record a change in a document
     */
    suspend fun recordChange(change: DocumentChange): Boolean = withContext(Dispatchers.IO) {
        try {
            val changes = getAllChanges().toMutableList()
            changes.add(change)
            saveChanges(changes)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get changes for a document
     */
    suspend fun getChangesForDocument(documentId: String): List<DocumentChange> = withContext(Dispatchers.IO) {
        getAllChanges()
            .filter { it.documentId == documentId }
            .sortedBy { it.timestamp }
    }

    /**
     * Accept a change
     */
    suspend fun acceptChange(changeId: String): Boolean = withContext(Dispatchers.IO) {
        updateChangeStatus(changeId, true)
    }

    /**
     * Reject a change
     */
    suspend fun rejectChange(changeId: String): Boolean = withContext(Dispatchers.IO) {
        updateChangeStatus(changeId, false)
    }

    private suspend fun updateChangeStatus(changeId: String, accepted: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val changes = getAllChanges().toMutableList()
            val index = changes.indexOfFirst { it.id == changeId }
            if (index >= 0) {
                changes[index] = changes[index].copy(accepted = accepted)
                saveChanges(changes)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getAllChanges(): List<DocumentChange> {
        val json = prefs.getString(KEY_CHANGES, "[]") ?: "[]"
        val type = object : TypeToken<List<DocumentChange>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveChanges(changes: List<DocumentChange>) {
        prefs.edit().putString(KEY_CHANGES, gson.toJson(changes)).apply()
    }
}
