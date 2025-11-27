package com.officesuite.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrManager {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(bitmap: Bitmap): OcrResult {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val blocks = result.textBlocks.map { block ->
                        TextBlock(
                            text = block.text,
                            confidence = block.lines.firstOrNull()?.confidence ?: 0f,
                            boundingBox = block.boundingBox?.let { 
                                BoundingBox(it.left, it.top, it.right, it.bottom) 
                            },
                            lines = block.lines.map { line ->
                                TextLine(
                                    text = line.text,
                                    confidence = line.confidence ?: 0f
                                )
                            }
                        )
                    }
                    
                    continuation.resume(OcrResult(
                        fullText = result.text,
                        blocks = blocks,
                        success = true
                    ))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(OcrResult(
                        fullText = "",
                        blocks = emptyList(),
                        success = false,
                        error = exception.message
                    ))
                }
        }
    }

    fun close() {
        recognizer.close()
    }
}

data class OcrResult(
    val fullText: String,
    val blocks: List<TextBlock>,
    val success: Boolean,
    val error: String? = null
)

data class TextBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox?,
    val lines: List<TextLine>
)

data class TextLine(
    val text: String,
    val confidence: Float
)

data class BoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
