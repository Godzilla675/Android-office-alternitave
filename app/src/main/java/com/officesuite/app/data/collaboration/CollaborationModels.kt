package com.officesuite.app.data.collaboration

import java.util.UUID

/**
 * Comment on a document
 */
data class DocumentComment(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val text: String,
    val author: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pageNumber: Int? = null,
    val position: CommentPosition? = null,
    val parentId: String? = null, // For reply threads
    val resolved: Boolean = false
)

/**
 * Position of a comment on a document page
 */
data class CommentPosition(
    val x: Float,
    val y: Float,
    val selectionStart: Int? = null,
    val selectionEnd: Int? = null,
    val selectedText: String? = null
)

/**
 * Version history entry for a document
 */
data class DocumentVersion(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val versionNumber: Int,
    val name: String? = null,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val author: String,
    val filePath: String,
    val fileSize: Long,
    val changesSummary: String? = null
)

/**
 * Share link for a document
 */
data class ShareLink(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val link: String,
    val permissions: SharePermissions,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val password: String? = null,
    val accessCount: Int = 0,
    val maxAccesses: Int? = null,
    val isActive: Boolean = true
)

/**
 * Permissions for a share link
 */
data class SharePermissions(
    val canView: Boolean = true,
    val canEdit: Boolean = false,
    val canComment: Boolean = false,
    val canDownload: Boolean = true,
    val canPrint: Boolean = true
)

/**
 * Track changes entry
 */
data class DocumentChange(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val author: String,
    val changeType: ChangeType,
    val timestamp: Long = System.currentTimeMillis(),
    val position: Int,
    val oldContent: String? = null,
    val newContent: String? = null,
    val accepted: Boolean? = null
)

/**
 * Types of tracked changes
 */
enum class ChangeType {
    INSERT,
    DELETE,
    FORMAT,
    REPLACE
}
