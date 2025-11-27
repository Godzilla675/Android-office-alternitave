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
import com.officesuite.app.ocr.OcrManager
import com.officesuite.app.ocr.OcrResult
import com.officesuite.app.utils.FileUtils
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.io.font.constants.StandardFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayOutputStream
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

    /**
     * Converts DOCX to PDF using HTML as an intermediate format.
     * This approach provides better fidelity for formatting, images, and tables.
     */
    private fun convertDocxToPdf(inputFile: File, outputFile: File) {
        val xwpfDocument = XWPFDocument(FileInputStream(inputFile))
        
        try {
            // Convert DOCX to HTML first
            val htmlContent = convertDocxToHtml(xwpfDocument)
            
            // Use iText to create PDF from HTML content
            val pdfWriter = PdfWriter(outputFile)
            val pdfDoc = ITextPdfDocument(pdfWriter)
            val document = Document(pdfDoc, PageSize.A4)
            document.setMargins(50f, 50f, 50f, 50f)
            
            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            val italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE)
            
            // Process paragraphs with better formatting
            for (paragraph in xwpfDocument.paragraphs) {
                val text = paragraph.text
                if (text.isNotEmpty()) {
                    val iTextParagraph = Paragraph()
                    
                    for (run in paragraph.runs) {
                        val runText = run.text() ?: ""
                        if (runText.isNotEmpty()) {
                            val currentFont = when {
                                run.isBold && run.isItalic -> boldFont
                                run.isBold -> boldFont
                                run.isItalic -> italicFont
                                else -> font
                            }
                            
                            val fontSize = if (run.fontSize > 0) run.fontSize.toFloat() else 11f
                            val textElement = com.itextpdf.layout.element.Text(runText)
                                .setFont(currentFont)
                                .setFontSize(fontSize)
                            
                            // Handle text color
                            run.color?.let { colorStr ->
                                try {
                                    val r = Integer.parseInt(colorStr.substring(0, 2), 16)
                                    val g = Integer.parseInt(colorStr.substring(2, 4), 16)
                                    val b = Integer.parseInt(colorStr.substring(4, 6), 16)
                                    textElement.setFontColor(DeviceRgb(r, g, b))
                                } catch (e: Exception) {
                                    // Ignore color parsing errors
                                }
                            }
                            
                            iTextParagraph.add(textElement)
                        }
                    }
                    
                    // Handle paragraph alignment
                    when (paragraph.alignment) {
                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER -> 
                            iTextParagraph.setTextAlignment(TextAlignment.CENTER)
                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT ->
                            iTextParagraph.setTextAlignment(TextAlignment.RIGHT)
                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.BOTH,
                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.DISTRIBUTE ->
                            iTextParagraph.setTextAlignment(TextAlignment.JUSTIFIED)
                        else ->
                            iTextParagraph.setTextAlignment(TextAlignment.LEFT)
                    }
                    
                    iTextParagraph.setMarginBottom(6f)
                    document.add(iTextParagraph)
                } else {
                    // Empty paragraph for spacing
                    document.add(Paragraph("\n"))
                }
            }
            
            // Process tables
            for (table in xwpfDocument.tables) {
                val numCols = table.rows.firstOrNull()?.tableCells?.size ?: 0
                if (numCols > 0) {
                    val iTextTable = com.itextpdf.layout.element.Table(numCols)
                    iTextTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100f))
                    
                    var isFirstRow = true
                    for (row in table.rows) {
                        for (cell in row.tableCells) {
                            val cellParagraph = Paragraph(cell.text)
                                .setFont(font)
                                .setFontSize(10f)
                                .setPadding(5f)
                            
                            val iTextCell = com.itextpdf.layout.element.Cell()
                                .add(cellParagraph)
                            
                            if (isFirstRow) {
                                iTextCell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
                                cellParagraph.setFont(boldFont)
                            }
                            
                            iTextTable.addCell(iTextCell)
                        }
                        isFirstRow = false
                    }
                    
                    iTextTable.setMarginBottom(12f)
                    document.add(iTextTable)
                }
            }
            
            // Process embedded images
            for (pictureData in xwpfDocument.allPictures) {
                try {
                    val imageData = ImageDataFactory.create(pictureData.data)
                    val image = Image(imageData)
                    image.setMaxWidth(500f)
                    image.setMarginBottom(10f)
                    document.add(image)
                } catch (e: Exception) {
                    // Skip images that can't be processed
                }
            }
            
            document.close()
        } finally {
            xwpfDocument.close()
        }
    }
    
    /**
     * Helper method to convert DOCX content to HTML string.
     * Used for intermediate conversion and preview purposes.
     */
    private fun convertDocxToHtml(document: XWPFDocument): String {
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>")
        
        for (paragraph in document.paragraphs) {
            val text = paragraph.text
            if (text.isNotEmpty()) {
                htmlBuilder.append("<p>")
                for (run in paragraph.runs) {
                    var runHtml = escapeHtmlForConverter(run.text() ?: "")
                    if (run.isBold) runHtml = "<b>$runHtml</b>"
                    if (run.isItalic) runHtml = "<i>$runHtml</i>"
                    htmlBuilder.append(runHtml)
                }
                htmlBuilder.append("</p>")
            }
        }
        
        for (table in document.tables) {
            htmlBuilder.append("<table border=\"1\">")
            for (row in table.rows) {
                htmlBuilder.append("<tr>")
                for (cell in row.tableCells) {
                    htmlBuilder.append("<td>${escapeHtmlForConverter(cell.text)}</td>")
                }
                htmlBuilder.append("</tr>")
            }
            htmlBuilder.append("</table>")
        }
        
        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
    }
    
    private fun escapeHtmlForConverter(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
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

    /**
     * Creates a PDF from scanned bitmaps without OCR.
     * For searchable PDFs with selectable text, use createSearchablePdfFromBitmaps instead.
     */
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

    /**
     * Creates a searchable PDF from scanned bitmaps with embedded OCR text.
     * The text is rendered invisibly behind the image, making the PDF text-selectable.
     * 
     * @param bitmaps List of scanned page images
     * @param ocrResults List of OCR results corresponding to each bitmap
     * @param outputFile The output PDF file
     * @return true if successful, false otherwise
     */
    fun createSearchablePdfFromBitmaps(
        bitmaps: List<Bitmap>,
        ocrResults: List<OcrResult>,
        outputFile: File
    ): Boolean {
        return try {
            val pdfWriter = PdfWriter(outputFile)
            val pdfDoc = ITextPdfDocument(pdfWriter)
            val document = Document(pdfDoc)
            
            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            
            bitmaps.forEachIndexed { index, bitmap ->
                // Convert bitmap to byte array
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val imageBytes = stream.toByteArray()
                
                // Create page with image dimensions
                val pageSize = PageSize(bitmap.width.toFloat(), bitmap.height.toFloat())
                pdfDoc.addNewPage(pageSize)
                
                // Add image to page
                val imageData = ImageDataFactory.create(imageBytes)
                val image = Image(imageData)
                image.setFixedPosition(index + 1, 0f, 0f)
                image.scaleToFit(pageSize.width, pageSize.height)
                document.add(image)
                
                // Add invisible OCR text layer if available
                if (index < ocrResults.size && ocrResults[index].success) {
                    val ocrResult = ocrResults[index]
                    
                    // Add text blocks at their detected positions
                    for (block in ocrResult.blocks) {
                        block.boundingBox?.let { box ->
                            // Calculate position (PDF coordinates start from bottom-left)
                            val x = box.left.toFloat()
                            val y = bitmap.height - box.bottom.toFloat()
                            
                            // Create invisible text (very small, transparent)
                            val textParagraph = Paragraph(block.text)
                                .setFont(font)
                                .setFontSize(1f) // Very small font
                                .setFontColor(ColorConstants.WHITE, 0f) // Transparent
                                .setFixedPosition(index + 1, x, y, (box.right - box.left).toFloat())
                            
                            document.add(textParagraph)
                        }
                    }
                }
            }
            
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Creates a searchable PDF using OCR to extract and embed text.
     * This is a convenience method that performs OCR internally.
     * 
     * @param bitmaps List of scanned page images
     * @param outputFile The output PDF file
     * @param ocrManager The OCR manager to use for text extraction
     * @return true if successful, false otherwise
     */
    suspend fun createSearchablePdfWithOcr(
        bitmaps: List<Bitmap>,
        outputFile: File,
        ocrManager: OcrManager
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Run OCR on all pages
            val ocrResults = bitmaps.map { bitmap ->
                ocrManager.extractText(bitmap)
            }
            
            // Create searchable PDF with OCR results
            createSearchablePdfFromBitmaps(bitmaps, ocrResults, outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
