package com.officesuite.app.pdf

import android.graphics.Color
import android.graphics.RectF
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.annot.PdfTextMarkupAnnotation
import com.itextpdf.kernel.pdf.annot.PdfFreeTextAnnotation
import com.itextpdf.kernel.pdf.annot.PdfInkAnnotation
import com.itextpdf.kernel.pdf.PdfArray
import com.itextpdf.kernel.pdf.PdfNumber
import com.itextpdf.kernel.pdf.PdfString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manager for PDF annotation features including highlighting, text boxes,
 * sticky notes, and freehand drawing.
 */
class PdfAnnotationManager {

    /**
     * Result class for annotation operations
     */
    data class AnnotationResult(
        val success: Boolean,
        val outputPath: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Annotation types
     */
    enum class AnnotationType {
        HIGHLIGHT,
        UNDERLINE,
        STRIKETHROUGH,
        TEXT_BOX,
        STICKY_NOTE,
        FREEHAND
    }

    /**
     * Highlight color options
     */
    enum class HighlightColor(val r: Int, val g: Int, val b: Int) {
        YELLOW(255, 255, 0),
        GREEN(0, 255, 0),
        BLUE(0, 200, 255),
        PINK(255, 150, 200),
        ORANGE(255, 165, 0)
    }

    /**
     * Represents a highlight annotation
     */
    data class HighlightAnnotation(
        val pageNumber: Int,
        val rect: RectF,
        val color: HighlightColor = HighlightColor.YELLOW,
        val opacity: Float = 0.5f
    )

    /**
     * Represents a text box annotation
     */
    data class TextBoxAnnotation(
        val pageNumber: Int,
        val rect: RectF,
        val text: String,
        val fontSize: Float = 12f,
        val textColor: Int = Color.BLACK,
        val backgroundColor: Int = Color.WHITE
    )

    /**
     * Represents a freehand drawing annotation
     */
    data class FreehandAnnotation(
        val pageNumber: Int,
        val points: List<Pair<Float, Float>>,
        val strokeColor: Int = Color.BLACK,
        val strokeWidth: Float = 2f
    )

    /**
     * Add a highlight annotation to a PDF.
     */
    suspend fun addHighlight(
        inputFile: File,
        outputFile: File,
        annotation: HighlightAnnotation
    ): AnnotationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputFile)
            val writer = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(reader, writer)
            
            val page = pdfDoc.getPage(annotation.pageNumber)
            val pageHeight = page.pageSize.height
            
            // Convert Android coordinates to PDF coordinates (PDF origin is bottom-left)
            val rect = Rectangle(
                annotation.rect.left,
                pageHeight - annotation.rect.bottom,
                annotation.rect.width(),
                annotation.rect.height()
            )
            
            val quadPoints = floatArrayOf(
                rect.left, rect.top,
                rect.right, rect.top,
                rect.left, rect.bottom,
                rect.right, rect.bottom
            )
            
            val highlightAnnotation = PdfTextMarkupAnnotation.createHighLight(
                rect, quadPoints
            )
            
            highlightAnnotation.setColor(DeviceRgb(
                annotation.color.r,
                annotation.color.g,
                annotation.color.b
            ))
            highlightAnnotation.setOpacity(PdfNumber(annotation.opacity.toDouble()))
            
            page.addAnnotation(highlightAnnotation)
            pdfDoc.close()
            
            AnnotationResult(success = true, outputPath = outputFile.absolutePath)
        } catch (e: Exception) {
            AnnotationResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * Add an underline annotation to a PDF.
     */
    suspend fun addUnderline(
        inputFile: File,
        outputFile: File,
        pageNumber: Int,
        rect: RectF,
        color: HighlightColor = HighlightColor.BLUE
    ): AnnotationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputFile)
            val writer = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(reader, writer)
            
            val page = pdfDoc.getPage(pageNumber)
            val pageHeight = page.pageSize.height
            
            val pdfRect = Rectangle(
                rect.left,
                pageHeight - rect.bottom,
                rect.width(),
                rect.height()
            )
            
            val quadPoints = floatArrayOf(
                pdfRect.left, pdfRect.top,
                pdfRect.right, pdfRect.top,
                pdfRect.left, pdfRect.bottom,
                pdfRect.right, pdfRect.bottom
            )
            
            val underlineAnnotation = PdfTextMarkupAnnotation.createUnderline(
                pdfRect, quadPoints
            )
            
            underlineAnnotation.setColor(DeviceRgb(color.r, color.g, color.b))
            
            page.addAnnotation(underlineAnnotation)
            pdfDoc.close()
            
            AnnotationResult(success = true, outputPath = outputFile.absolutePath)
        } catch (e: Exception) {
            AnnotationResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * Add a strikethrough annotation to a PDF.
     */
    suspend fun addStrikethrough(
        inputFile: File,
        outputFile: File,
        pageNumber: Int,
        rect: RectF,
        color: HighlightColor = HighlightColor.PINK
    ): AnnotationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputFile)
            val writer = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(reader, writer)
            
            val page = pdfDoc.getPage(pageNumber)
            val pageHeight = page.pageSize.height
            
            val pdfRect = Rectangle(
                rect.left,
                pageHeight - rect.bottom,
                rect.width(),
                rect.height()
            )
            
            val quadPoints = floatArrayOf(
                pdfRect.left, pdfRect.top,
                pdfRect.right, pdfRect.top,
                pdfRect.left, pdfRect.bottom,
                pdfRect.right, pdfRect.bottom
            )
            
            val strikeAnnotation = PdfTextMarkupAnnotation.createStrikeout(
                pdfRect, quadPoints
            )
            
            strikeAnnotation.setColor(DeviceRgb(color.r, color.g, color.b))
            
            page.addAnnotation(strikeAnnotation)
            pdfDoc.close()
            
            AnnotationResult(success = true, outputPath = outputFile.absolutePath)
        } catch (e: Exception) {
            AnnotationResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * Add a text box annotation to a PDF.
     */
    suspend fun addTextBox(
        inputFile: File,
        outputFile: File,
        annotation: TextBoxAnnotation
    ): AnnotationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputFile)
            val writer = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(reader, writer)
            
            val page = pdfDoc.getPage(annotation.pageNumber)
            val pageHeight = page.pageSize.height
            
            val pdfRect = Rectangle(
                annotation.rect.left,
                pageHeight - annotation.rect.bottom,
                annotation.rect.width(),
                annotation.rect.height()
            )
            
            val freeTextAnnotation = PdfFreeTextAnnotation(pdfRect, PdfString(annotation.text))
            freeTextAnnotation.setContents(annotation.text)
            
            page.addAnnotation(freeTextAnnotation)
            pdfDoc.close()
            
            AnnotationResult(success = true, outputPath = outputFile.absolutePath)
        } catch (e: Exception) {
            AnnotationResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * Add a freehand drawing (ink) annotation to a PDF.
     */
    suspend fun addFreehandDrawing(
        inputFile: File,
        outputFile: File,
        annotation: FreehandAnnotation
    ): AnnotationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputFile)
            val writer = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(reader, writer)
            
            val page = pdfDoc.getPage(annotation.pageNumber)
            val pageHeight = page.pageSize.height
            
            // Convert points to ink list format
            val inkList = PdfArray()
            val pointArray = PdfArray()
            
            annotation.points.forEach { (x, y) ->
                pointArray.add(PdfNumber(x.toDouble()))
                pointArray.add(PdfNumber((pageHeight - y).toDouble()))
            }
            inkList.add(pointArray)
            
            // Calculate bounding rectangle
            val minX = annotation.points.minOfOrNull { it.first } ?: 0f
            val maxX = annotation.points.maxOfOrNull { it.first } ?: 0f
            val minY = annotation.points.minOfOrNull { it.second } ?: 0f
            val maxY = annotation.points.maxOfOrNull { it.second } ?: 0f
            
            val pdfRect = Rectangle(
                minX - 5,
                pageHeight - maxY - 5,
                maxX - minX + 10,
                maxY - minY + 10
            )
            
            val inkAnnotation = PdfInkAnnotation(pdfRect, inkList)
            
            // Set color from Android color int
            val r = Color.red(annotation.strokeColor)
            val g = Color.green(annotation.strokeColor)
            val b = Color.blue(annotation.strokeColor)
            inkAnnotation.setColor(DeviceRgb(r, g, b))
            
            page.addAnnotation(inkAnnotation)
            pdfDoc.close()
            
            AnnotationResult(success = true, outputPath = outputFile.absolutePath)
        } catch (e: Exception) {
            AnnotationResult(success = false, errorMessage = e.message)
        }
    }

    /**
     * Add multiple annotations to a PDF in one operation.
     */
    suspend fun addAnnotations(
        inputFile: File,
        outputFile: File,
        highlights: List<HighlightAnnotation> = emptyList(),
        textBoxes: List<TextBoxAnnotation> = emptyList(),
        freehandDrawings: List<FreehandAnnotation> = emptyList()
    ): AnnotationResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputFile)
            val writer = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(reader, writer)
            
            highlights.forEach { annotation ->
                val page = pdfDoc.getPage(annotation.pageNumber)
                val pageHeight = page.pageSize.height
                
                val rect = Rectangle(
                    annotation.rect.left,
                    pageHeight - annotation.rect.bottom,
                    annotation.rect.width(),
                    annotation.rect.height()
                )
                
                val quadPoints = floatArrayOf(
                    rect.left, rect.top,
                    rect.right, rect.top,
                    rect.left, rect.bottom,
                    rect.right, rect.bottom
                )
                
                val highlightAnnotation = PdfTextMarkupAnnotation.createHighLight(rect, quadPoints)
                highlightAnnotation.setColor(DeviceRgb(
                    annotation.color.r,
                    annotation.color.g,
                    annotation.color.b
                ))
                highlightAnnotation.setOpacity(PdfNumber(annotation.opacity.toDouble()))
                page.addAnnotation(highlightAnnotation)
            }
            
            textBoxes.forEach { annotation ->
                val page = pdfDoc.getPage(annotation.pageNumber)
                val pageHeight = page.pageSize.height
                
                val pdfRect = Rectangle(
                    annotation.rect.left,
                    pageHeight - annotation.rect.bottom,
                    annotation.rect.width(),
                    annotation.rect.height()
                )
                
                val freeTextAnnotation = PdfFreeTextAnnotation(pdfRect, PdfString(annotation.text))
                freeTextAnnotation.setContents(annotation.text)
                page.addAnnotation(freeTextAnnotation)
            }
            
            freehandDrawings.forEach { annotation ->
                val page = pdfDoc.getPage(annotation.pageNumber)
                val pageHeight = page.pageSize.height
                
                val inkList = PdfArray()
                val pointArray = PdfArray()
                
                annotation.points.forEach { (x, y) ->
                    pointArray.add(PdfNumber(x.toDouble()))
                    pointArray.add(PdfNumber((pageHeight - y).toDouble()))
                }
                inkList.add(pointArray)
                
                val minX = annotation.points.minOfOrNull { it.first } ?: 0f
                val maxX = annotation.points.maxOfOrNull { it.first } ?: 0f
                val minY = annotation.points.minOfOrNull { it.second } ?: 0f
                val maxY = annotation.points.maxOfOrNull { it.second } ?: 0f
                
                val pdfRect = Rectangle(
                    minX - 5,
                    pageHeight - maxY - 5,
                    maxX - minX + 10,
                    maxY - minY + 10
                )
                
                val inkAnnotation = PdfInkAnnotation(pdfRect, inkList)
                val r = Color.red(annotation.strokeColor)
                val g = Color.green(annotation.strokeColor)
                val b = Color.blue(annotation.strokeColor)
                inkAnnotation.setColor(DeviceRgb(r, g, b))
                page.addAnnotation(inkAnnotation)
            }
            
            pdfDoc.close()
            
            AnnotationResult(success = true, outputPath = outputFile.absolutePath)
        } catch (e: Exception) {
            AnnotationResult(success = false, errorMessage = e.message)
        }
    }
}
