package com.officesuite.app.data.model

import android.graphics.Bitmap

data class ScannedPage(
    val bitmap: Bitmap,
    val corners: List<Corner>? = null,
    val isProcessed: Boolean = false,
    val extractedText: String? = null
)

data class Corner(
    val x: Float,
    val y: Float
)

data class ScanResult(
    val pages: List<ScannedPage>,
    val pdfPath: String? = null
)
