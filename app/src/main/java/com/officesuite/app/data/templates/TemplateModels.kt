package com.officesuite.app.data.templates

import java.util.UUID

/**
 * Template categories for organizing templates
 */
enum class TemplateCategory(val displayName: String) {
    RESUME("Resume & CV"),
    LETTER("Letters"),
    INVOICE("Invoice & Billing"),
    REPORT("Reports"),
    PRESENTATION("Presentations"),
    SPREADSHEET("Spreadsheets"),
    CUSTOM("Custom Templates")
}

/**
 * Template types matching document formats
 */
enum class TemplateType(val extension: String) {
    MARKDOWN("md"),
    DOCX("docx"),
    PPTX("pptx"),
    XLSX("xlsx"),
    PDF("pdf")
}

/**
 * Data model for a document template
 */
data class DocumentTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val type: TemplateType,
    val content: String,
    val thumbnailRes: Int? = null,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val usageCount: Int = 0,
    val placeholders: List<TemplatePlaceholder> = emptyList()
)

/**
 * Placeholder fields in templates that users can fill in
 */
data class TemplatePlaceholder(
    val key: String,
    val label: String,
    val defaultValue: String = "",
    val hint: String = "",
    val required: Boolean = false,
    val type: PlaceholderType = PlaceholderType.TEXT
)

/**
 * Types of placeholder fields
 */
enum class PlaceholderType {
    TEXT,
    MULTILINE_TEXT,
    DATE,
    NUMBER,
    EMAIL,
    PHONE
}

/**
 * User's filled template data
 */
data class FilledTemplate(
    val templateId: String,
    val values: Map<String, String>,
    val createdAt: Long = System.currentTimeMillis()
)
