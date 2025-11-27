package com.officesuite.app.cloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.InputStream

/**
 * Interface for cloud storage providers.
 * Implement this interface for each cloud storage service (Google Drive, Dropbox, OneDrive).
 */
interface CloudStorageProvider {
    
    /**
     * The name of the cloud storage provider
     */
    val providerName: String

    /**
     * Check if the user is authenticated with this provider
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Get the sign-in intent for authentication
     * @return Intent to start the sign-in flow, or null if not applicable
     */
    fun getSignInIntent(): Intent?

    /**
     * Handle the result from the sign-in intent
     * @param data The result data from the sign-in activity
     * @return true if sign-in was successful
     */
    suspend fun handleSignInResult(data: Intent?): Boolean

    /**
     * Sign out from the cloud provider
     */
    suspend fun signOut()

    /**
     * List files in a folder
     * @param folderId The folder ID (null for root)
     * @return List of cloud files
     */
    suspend fun listFiles(folderId: String? = null): CloudResult<List<CloudFile>>

    /**
     * Download a file from the cloud
     * @param fileId The ID of the file to download
     * @param destinationFile The local file to save to
     * @return Result indicating success or failure
     */
    suspend fun downloadFile(fileId: String, destinationFile: File): CloudResult<File>

    /**
     * Upload a file to the cloud
     * @param localFile The local file to upload
     * @param folderId The destination folder ID (null for root)
     * @param fileName Optional custom file name
     * @return Result with the uploaded file info
     */
    suspend fun uploadFile(
        localFile: File,
        folderId: String? = null,
        fileName: String? = null
    ): CloudResult<CloudFile>

    /**
     * Delete a file from the cloud
     * @param fileId The ID of the file to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteFile(fileId: String): CloudResult<Unit>

    /**
     * Get file metadata
     * @param fileId The ID of the file
     * @return Result with file metadata
     */
    suspend fun getFileMetadata(fileId: String): CloudResult<CloudFile>

    /**
     * Search for files
     * @param query The search query
     * @return List of matching files
     */
    suspend fun searchFiles(query: String): CloudResult<List<CloudFile>>
}

/**
 * Represents a file in cloud storage
 */
data class CloudFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedTime: Long,
    val isFolder: Boolean,
    val parentId: String? = null,
    val webViewLink: String? = null,
    val downloadUrl: String? = null
)

/**
 * Result wrapper for cloud operations
 */
sealed class CloudResult<out T> {
    data class Success<T>(val data: T) : CloudResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : CloudResult<Nothing>()
    object Loading : CloudResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun errorMessageOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }
}
