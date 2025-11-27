package com.officesuite.app.cloud

import android.content.Context
import android.content.Intent
import java.io.File

/**
 * Local storage provider that serves as a fallback and reference implementation.
 * This allows documents to be saved to a local "cloud-like" folder structure.
 */
class LocalStorageProvider(private val context: Context) : CloudStorageProvider {

    override val providerName: String = "Local Storage"

    private val rootFolder: File by lazy {
        val folder = File(context.getExternalFilesDir(null), "OfficeSuite")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        folder
    }

    override suspend fun isAuthenticated(): Boolean = true

    override fun getSignInIntent(): Intent? = null

    override suspend fun handleSignInResult(data: Intent?): Boolean = true

    override suspend fun signOut() {
        // Local storage doesn't require sign out
    }

    override suspend fun listFiles(folderId: String?): CloudResult<List<CloudFile>> {
        return try {
            val folder = if (folderId != null) {
                File(rootFolder, folderId)
            } else {
                rootFolder
            }

            if (!folder.exists() || !folder.isDirectory) {
                return CloudResult.Success(emptyList())
            }

            val files = folder.listFiles()?.map { file ->
                CloudFile(
                    id = file.relativeTo(rootFolder).path,
                    name = file.name,
                    mimeType = getMimeType(file),
                    size = if (file.isFile) file.length() else 0,
                    modifiedTime = file.lastModified(),
                    isFolder = file.isDirectory,
                    parentId = file.parentFile?.relativeTo(rootFolder)?.path
                )
            } ?: emptyList()

            CloudResult.Success(files)
        } catch (e: Exception) {
            CloudResult.Error(e.message ?: "Failed to list files", e)
        }
    }

    override suspend fun downloadFile(fileId: String, destinationFile: File): CloudResult<File> {
        return try {
            val sourceFile = File(rootFolder, fileId)
            if (!sourceFile.exists()) {
                return CloudResult.Error("File not found")
            }

            sourceFile.copyTo(destinationFile, overwrite = true)
            CloudResult.Success(destinationFile)
        } catch (e: Exception) {
            CloudResult.Error(e.message ?: "Failed to download file", e)
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        folderId: String?,
        fileName: String?
    ): CloudResult<CloudFile> {
        return try {
            val targetFolder = if (folderId != null) {
                File(rootFolder, folderId).also { it.mkdirs() }
            } else {
                rootFolder
            }

            val targetFile = File(targetFolder, fileName ?: localFile.name)
            localFile.copyTo(targetFile, overwrite = true)

            val cloudFile = CloudFile(
                id = targetFile.relativeTo(rootFolder).path,
                name = targetFile.name,
                mimeType = getMimeType(targetFile),
                size = targetFile.length(),
                modifiedTime = targetFile.lastModified(),
                isFolder = false,
                parentId = folderId
            )

            CloudResult.Success(cloudFile)
        } catch (e: Exception) {
            CloudResult.Error(e.message ?: "Failed to upload file", e)
        }
    }

    override suspend fun deleteFile(fileId: String): CloudResult<Unit> {
        return try {
            val file = File(rootFolder, fileId)
            if (file.exists()) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
            CloudResult.Success(Unit)
        } catch (e: Exception) {
            CloudResult.Error(e.message ?: "Failed to delete file", e)
        }
    }

    override suspend fun getFileMetadata(fileId: String): CloudResult<CloudFile> {
        return try {
            val file = File(rootFolder, fileId)
            if (!file.exists()) {
                return CloudResult.Error("File not found")
            }

            val cloudFile = CloudFile(
                id = file.relativeTo(rootFolder).path,
                name = file.name,
                mimeType = getMimeType(file),
                size = if (file.isFile) file.length() else 0,
                modifiedTime = file.lastModified(),
                isFolder = file.isDirectory,
                parentId = file.parentFile?.relativeTo(rootFolder)?.path
            )

            CloudResult.Success(cloudFile)
        } catch (e: Exception) {
            CloudResult.Error(e.message ?: "Failed to get file metadata", e)
        }
    }

    override suspend fun searchFiles(query: String): CloudResult<List<CloudFile>> {
        return try {
            val results = mutableListOf<CloudFile>()
            searchRecursive(rootFolder, query.lowercase(), results)
            CloudResult.Success(results)
        } catch (e: Exception) {
            CloudResult.Error(e.message ?: "Failed to search files", e)
        }
    }

    private fun searchRecursive(folder: File, query: String, results: MutableList<CloudFile>) {
        folder.listFiles()?.forEach { file ->
            if (file.name.lowercase().contains(query)) {
                results.add(
                    CloudFile(
                        id = file.relativeTo(rootFolder).path,
                        name = file.name,
                        mimeType = getMimeType(file),
                        size = if (file.isFile) file.length() else 0,
                        modifiedTime = file.lastModified(),
                        isFolder = file.isDirectory,
                        parentId = file.parentFile?.relativeTo(rootFolder)?.path
                    )
                )
            }
            if (file.isDirectory) {
                searchRecursive(file, query, results)
            }
        }
    }

    /**
     * Create a folder in local storage
     */
    fun createFolder(folderName: String, parentId: String? = null): CloudResult<CloudFile> {
        return try {
            val parent = if (parentId != null) {
                File(rootFolder, parentId)
            } else {
                rootFolder
            }

            val newFolder = File(parent, folderName)
            if (newFolder.mkdirs() || newFolder.exists()) {
                CloudResult.Success(
                    CloudFile(
                        id = newFolder.relativeTo(rootFolder).path,
                        name = folderName,
                        mimeType = "application/directory",
                        size = 0,
                        modifiedTime = System.currentTimeMillis(),
                        isFolder = true,
                        parentId = parentId
                    )
                )
            } else {
                CloudResult.Error("Failed to create folder")
            }
        } catch (e: Exception) {
            CloudResult.Error(e.message ?: "Failed to create folder", e)
        }
    }

    private fun getMimeType(file: File): String {
        if (file.isDirectory) return "application/directory"
        
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }
}
