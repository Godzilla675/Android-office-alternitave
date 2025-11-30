package com.officesuite.app.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.officesuite.app.data.model.DocumentType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown"
        
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
        } else {
            uri.path?.let { path ->
                name = File(path).name
            }
        }
        
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } else {
            uri.path?.let { path ->
                size = File(path).length()
            }
        }
        
        return size
    }

    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.getType(uri)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }

    fun getDocumentType(context: Context, uri: Uri): DocumentType {
        val mimeType = getMimeType(context, uri)
        if (mimeType != null) {
            val type = DocumentType.fromMimeType(mimeType)
            if (type != DocumentType.UNKNOWN) {
                return type
            }
        }
        
        val fileName = getFileName(context, uri)
        val extension = getFileExtension(fileName)
        return DocumentType.fromExtension(extension)
    }

    fun copyToCache(context: Context, uri: Uri, fileName: String? = null): File? {
        return try {
            val name = fileName ?: getFileName(context, uri)
            val cacheDir = File(context.cacheDir, "documents")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val outputFile = File(cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    fun getOutputDirectory(context: Context): File {
        val outputDir = File(context.getExternalFilesDir(null), "output")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        return outputDir
    }

    fun createTempFile(context: Context, prefix: String, suffix: String): File {
        val tempDir = File(context.cacheDir, "temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return File.createTempFile(prefix, suffix, tempDir)
    }

    /**
     * Save a file to the public Downloads folder so it's visible in file managers.
     * Uses MediaStore on Android Q+ and direct file access on older versions.
     * 
     * @param context The context
     * @param sourceFile The source file to save
     * @param fileName The name of the file to save (with extension)
     * @param mimeType The MIME type of the file
     * @return The URI of the saved file, or null if failed
     */
    fun saveToPublicDownloads(
        context: Context,
        sourceFile: File,
        fileName: String,
        mimeType: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OfficeSuite")
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let { outputUri ->
                    resolver.openOutputStream(outputUri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    // Mark the file as complete
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(outputUri, updateValues, null, null)
                    
                    outputUri
                }
            } else {
                // Direct file access for older versions
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "OfficeSuite"
                )
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val outputFile = File(downloadsDir, fileName)
                FileInputStream(sourceFile).use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                Uri.fromFile(outputFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get a file path from a URI that can be used for file operations.
     * This handles both file:// and content:// URIs.
     * 
     * @param context The context
     * @param uri The URI to get the path from
     * @return The file path, or null if unable to resolve
     */
    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "file" -> uri.path
                "content" -> {
                    // For content:// URIs, we need to copy to cache first
                    val cachedFile = copyToCache(context, uri)
                    cachedFile?.absolutePath
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Check if a URI is accessible (has read permission).
     * 
     * @param context The context
     * @param uri The URI to check
     * @return true if the URI can be read, false otherwise
     */
    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
