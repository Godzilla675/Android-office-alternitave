package com.officesuite.app.data.model

data class ConversionResult(
    val success: Boolean,
    val outputPath: String? = null,
    val errorMessage: String? = null
)

data class ConversionOptions(
    val sourceFormat: DocumentType,
    val targetFormat: DocumentType,
    val quality: Int = 100,
    val includeImages: Boolean = true,
    val ocrEnabled: Boolean = false
)
