package com.officesuite.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Supported OCR languages
 */
enum class OcrLanguage(val displayName: String, val code: String) {
    LATIN("Latin (English, etc.)", "latin"),
    CHINESE("Chinese", "chinese"),
    JAPANESE("Japanese", "japanese"),
    KOREAN("Korean", "korean"),
    DEVANAGARI("Devanagari (Hindi, etc.)", "devanagari")
}

/**
 * Enhanced OCR Manager with multi-language support
 */
class OcrManager {

    private var currentRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var currentLanguage: OcrLanguage = OcrLanguage.LATIN

    /**
     * Sets the OCR language for text recognition
     * 
     * @param language The language to use for OCR
     */
    fun setLanguage(language: OcrLanguage) {
        if (language != currentLanguage) {
            currentRecognizer.close()
            currentRecognizer = createRecognizer(language)
            currentLanguage = language
        }
    }

    /**
     * Gets the currently selected OCR language
     */
    fun getCurrentLanguage(): OcrLanguage = currentLanguage

    private fun createRecognizer(language: OcrLanguage): TextRecognizer {
        return when (language) {
            OcrLanguage.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            OcrLanguage.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            OcrLanguage.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            OcrLanguage.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            OcrLanguage.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        }
    }

    /**
     * Extracts text from a single bitmap image
     * 
     * @param bitmap The image to process
     * @return OcrResult containing the extracted text
     */
    suspend fun extractText(bitmap: Bitmap): OcrResult {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            currentRecognizer.process(image)
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
                        success = true,
                        language = currentLanguage
                    ))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(OcrResult(
                        fullText = "",
                        blocks = emptyList(),
                        success = false,
                        error = exception.message,
                        language = currentLanguage
                    ))
                }
        }
    }

    /**
     * Batch OCR: Extracts text from multiple bitmaps
     * 
     * @param bitmaps List of images to process
     * @param onProgress Callback for progress updates (0.0 to 1.0)
     * @return BatchOcrResult containing all extracted text
     */
    suspend fun extractTextBatch(
        bitmaps: List<Bitmap>,
        onProgress: ((Float) -> Unit)? = null
    ): BatchOcrResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<OcrResult>()
        val totalPages = bitmaps.size
        
        bitmaps.forEachIndexed { index, bitmap ->
            val result = extractText(bitmap)
            results.add(result)
            onProgress?.invoke((index + 1).toFloat() / totalPages)
        }
        
        val combinedText = results
            .filter { it.success }
            .mapIndexed { index, result -> 
                "--- Page ${index + 1} ---\n${result.fullText}" 
            }
            .joinToString("\n\n")
        
        val successCount = results.count { it.success }
        
        BatchOcrResult(
            results = results,
            combinedText = combinedText,
            totalPages = totalPages,
            successfulPages = successCount,
            success = successCount > 0,
            language = currentLanguage
        )
    }

    fun close() {
        currentRecognizer.close()
    }
}

/**
 * Result of a single OCR operation
 */
data class OcrResult(
    val fullText: String,
    val blocks: List<TextBlock>,
    val success: Boolean,
    val error: String? = null,
    val language: OcrLanguage = OcrLanguage.LATIN
)

/**
 * Result of a batch OCR operation
 */
data class BatchOcrResult(
    val results: List<OcrResult>,
    val combinedText: String,
    val totalPages: Int,
    val successfulPages: Int,
    val success: Boolean,
    val language: OcrLanguage = OcrLanguage.LATIN
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
