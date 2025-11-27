package com.officesuite.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.officesuite.app.data.model.ConversionOptions
import com.officesuite.app.data.model.ConversionResult
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DocumentConverter(private val context: Context) {

    suspend fun convert(
        inputFile: File,
        options: ConversionOptions
    ): ConversionResult = withContext(Dispatchers.IO) {
        try {
            val outputFile = createOutputFile(inputFile.nameWithoutExtension, options.targetFormat)
            
            when {
                options.sourceFormat == DocumentType.PDF && options.targetFormat == DocumentType.DOCX -> {
                    convertPdfToDocx(inputFile, outputFile)
                }
                options.sourceFormat == DocumentType.PDF && options.targetFormat == DocumentType.PPTX -> {
                    convertPdfToPptx(inputFile, outputFile)
                }
                options.sourceFormat == DocumentType.DOCX && options.targetFormat == DocumentType.PDF -> {
                    convertDocxToPdf(inputFile, outputFile)
                }
                options.sourceFormat == DocumentType.PPTX && options.targetFormat == DocumentType.PDF -> {
                    convertPptxToPdf(inputFile, outputFile)
                }
                options.sourceFormat == DocumentType.MARKDOWN && options.targetFormat == DocumentType.PDF -> {
                    convertMarkdownToPdf(inputFile, outputFile)
                }
                options.sourceFormat == DocumentType.TXT && options.targetFormat == DocumentType.PDF -> {
                    convertTextToPdf(inputFile, outputFile)
                }
                else -> {
                    return@withContext ConversionResult(
                        success = false,
                        errorMessage = "Unsupported conversion: ${options.sourceFormat} to ${options.targetFormat}"
                    )
                }
            }
            
            ConversionResult(
                success = true,
                outputPath = outputFile.absolutePath
            )
        } catch (e: Exception) {
            ConversionResult(
                success = false,
                errorMessage = e.message ?: "Conversion failed"
            )
        }
    }

    private fun createOutputFile(baseName: String, targetFormat: DocumentType): File {
        val outputDir = FileUtils.getOutputDirectory(context)
        return File(outputDir, "$baseName.${targetFormat.extension}")
    }

    private fun convertPdfToDocx(inputFile: File, outputFile: File) {
        val document = XWPFDocument()
        
        val descriptor = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        
        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                
                // Add page content as paragraph (simplified - real implementation would extract text)
                val paragraph = document.createParagraph()
                val run = paragraph.createRun()
                run.setText("Page ${i + 1}")
                run.addBreak()
                
                // Create bitmap from PDF page
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                // Save bitmap as image and add to document
                val imageFile = FileUtils.createTempFile(context, "page_$i", ".png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                // Add image to document
                FileInputStream(imageFile).use { imageStream ->
                    run.addPicture(
                        imageStream,
                        XWPFDocument.PICTURE_TYPE_PNG,
                        imageFile.name,
                        page.width * 9525,
                        page.height * 9525
                    )
                }
                
                page.close()
                bitmap.recycle()
                imageFile.delete()
            }
        } finally {
            renderer.close()
            descriptor.close()
        }
        
        FileOutputStream(outputFile).use { out ->
            document.write(out)
        }
        document.close()
    }

    private fun convertPdfToPptx(inputFile: File, outputFile: File) {
        val slideShow = XMLSlideShow()
        
        val descriptor = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        
        try {
            val layout = slideShow.slideMasters[0].slideLayouts[0]
            
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                
                val slide = slideShow.createSlide(layout)
                
                // Create bitmap from PDF page
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                // Save bitmap as image
                val imageFile = FileUtils.createTempFile(context, "slide_$i", ".png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                // Add image to slide
                FileInputStream(imageFile).use { imageStream ->
                    val pictureData = slideShow.addPicture(imageStream, org.apache.poi.sl.usermodel.PictureData.PictureType.PNG)
                    slide.createPicture(pictureData)
                }
                
                page.close()
                bitmap.recycle()
                imageFile.delete()
            }
        } finally {
            renderer.close()
            descriptor.close()
        }
        
        FileOutputStream(outputFile).use { out ->
            slideShow.write(out)
        }
        slideShow.close()
    }

    private fun convertDocxToPdf(inputFile: File, outputFile: File) {
        val document = XWPFDocument(FileInputStream(inputFile))
        val pdfDocument = PdfDocument()
        
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = android.graphics.Paint().apply {
                color = Color.BLACK
                textSize = 12f
            }
            
            var y = 50f
            val lineHeight = 18f
            val pageHeight = 792f
            var pageNum = 1
            
            for (paragraph in document.paragraphs) {
                val text = paragraph.text
                if (text.isNotEmpty()) {
                    val lines = wrapText(text, paint, 500f)
                    for (line in lines) {
                        if (y > pageHeight) {
                            pdfDocument.finishPage(page)
                            pageNum++
                            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            y = 50f
                        }
                        canvas.drawText(line, 50f, y, paint)
                        y += lineHeight
                    }
                    y += lineHeight / 2
                }
            }
            
            pdfDocument.finishPage(page)
            
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
            document.close()
        }
    }

    private fun convertPptxToPdf(inputFile: File, outputFile: File) {
        val slideShow = XMLSlideShow(FileInputStream(inputFile))
        val pdfDocument = PdfDocument()
        
        try {
            // Use standard 16:9 presentation dimensions
            val slideWidth = 960
            val slideHeight = 540
            var pageNum = 1
            
            for (slide in slideShow.slides) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    slideWidth,
                    slideHeight,
                    pageNum
                ).create()
                
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                // Create bitmap for slide
                val bitmap = Bitmap.createBitmap(
                    slideWidth,
                    slideHeight,
                    Bitmap.Config.ARGB_8888
                )
                val bitmapCanvas = Canvas(bitmap)
                bitmapCanvas.drawColor(Color.WHITE)
                
                // Draw slide content (simplified)
                val paint = android.graphics.Paint().apply {
                    color = Color.BLACK
                    textSize = 24f
                }
                bitmapCanvas.drawText("Slide $pageNum", 50f, 50f, paint)
                
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                bitmap.recycle()
                
                pdfDocument.finishPage(page)
                pageNum++
            }
            
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
            slideShow.close()
        }
    }

    private fun convertMarkdownToPdf(inputFile: File, outputFile: File) {
        val content = inputFile.readText()
        convertTextToPdf(content, outputFile, "Markdown Document")
    }

    private fun convertTextToPdf(inputFile: File, outputFile: File) {
        val content = inputFile.readText()
        convertTextToPdf(content, outputFile, "Text Document")
    }

    private fun convertTextToPdf(content: String, outputFile: File, title: String) {
        val pdfDocument = PdfDocument()
        
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = android.graphics.Paint().apply {
                color = Color.BLACK
                textSize = 12f
            }
            
            var y = 50f
            val lineHeight = 18f
            val pageHeight = 792f
            var pageNum = 1
            
            val lines = content.split("\n")
            for (line in lines) {
                val wrappedLines = wrapText(line, paint, 500f)
                for (wrappedLine in wrappedLines) {
                    if (y > pageHeight) {
                        pdfDocument.finishPage(page)
                        pageNum++
                        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                        page = pdfDocument.startPage(newPageInfo)
                        canvas = page.canvas
                        y = 50f
                    }
                    canvas.drawText(wrappedLine, 50f, y, paint)
                    y += lineHeight
                }
            }
            
            pdfDocument.finishPage(page)
            
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }
    }

    private fun wrapText(text: String, paint: android.graphics.Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val width = paint.measureText(testLine)
            
            if (width <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return if (lines.isEmpty()) listOf("") else lines
    }

    fun createPdfFromBitmaps(bitmaps: List<Bitmap>, outputFile: File): Boolean {
        return try {
            val pdfDocument = PdfDocument()
            
            bitmaps.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(
                    bitmap.width,
                    bitmap.height,
                    index + 1
                ).create()
                
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
            }
            
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
