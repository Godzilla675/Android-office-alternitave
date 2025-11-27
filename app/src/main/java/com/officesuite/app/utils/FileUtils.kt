package com.officesuite.app.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.officesuite.app.data.model.DocumentType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
}
