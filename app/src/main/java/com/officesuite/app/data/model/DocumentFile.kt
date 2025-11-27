package com.officesuite.app.data.model

import android.net.Uri

data class DocumentFile(
    val uri: Uri,
    val name: String,
    val type: DocumentType,
    val size: Long,
    val lastModified: Long,
    val path: String? = null
)

enum class DocumentType(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOC("doc", "application/msword"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    PPT("ppt", "application/vnd.ms-powerpoint"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    XLS("xls", "application/vnd.ms-excel"),
    MARKDOWN("md", "text/markdown"),
    TXT("txt", "text/plain"),
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
    XML("xml", "application/xml"),
    UNKNOWN("", "application/octet-stream");

    companion object {
        fun fromExtension(ext: String): DocumentType {
            return values().find { it.extension.equals(ext, ignoreCase = true) } ?: UNKNOWN
        }

        fun fromMimeType(mimeType: String): DocumentType {
            return values().find { it.mimeType.equals(mimeType, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
