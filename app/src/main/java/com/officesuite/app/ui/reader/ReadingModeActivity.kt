package com.officesuite.app.ui.reader

import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebSettings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.officesuite.app.databinding.ActivityReadingModeBinding
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Full-screen reading/presentation mode for PDF, DOCX, and XLSX documents.
 * Provides a distraction-free viewing experience with timer support.
 */
class ReadingModeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadingModeBinding
    
    private var documentType: DocumentType = DocumentType.UNKNOWN
    private var timerCountDown: CountDownTimer? = null
    private var isTimerRunning = false
    private var elapsedSeconds = 0L
    private var currentPage = 0
    private var totalPages = 1

    enum class DocumentType {
        PDF, DOCX, XLSX, UNKNOWN
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingModeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        hideSystemUI()
        setupWebView()
        setupClickListeners()
        
        val fileUriString = intent.getStringExtra(EXTRA_FILE_URI)
        val docTypeString = intent.getStringExtra(EXTRA_DOCUMENT_TYPE)
        
        if (fileUriString == null) {
            Toast.makeText(this, "No file provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        documentType = try {
            DocumentType.valueOf(docTypeString ?: "UNKNOWN")
        } catch (e: Exception) {
            DocumentType.UNKNOWN
        }
        
        loadDocument(Uri.parse(fileUriString))
    }

    private fun hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            setBackgroundColor(android.graphics.Color.WHITE)
        }
    }

    private fun setupClickListeners() {
        // Navigate to previous page
        binding.btnPrevious.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                updatePageDisplay()
            }
        }
        
        // Navigate to next page
        binding.btnNext.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                updatePageDisplay()
            }
        }
        
        // Tap on content area to toggle controls visibility
        binding.webView.setOnClickListener {
            toggleControlsVisibility()
        }
        
        // Exit reading mode
        binding.btnExit.setOnClickListener {
            finish()
        }
        
        // Timer toggle
        binding.btnTimer.setOnClickListener {
            toggleTimer()
        }
        
        // Reset timer on text click
        binding.textTimer.setOnClickListener {
            resetTimer()
        }
    }

    private fun toggleControlsVisibility() {
        val visibility = if (binding.controlsContainer.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.controlsContainer.visibility = visibility
        binding.timerContainer.visibility = visibility
        binding.btnExit.visibility = visibility
    }

    private fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        isTimerRunning = true
        binding.btnTimer.text = "⏸"
        
        timerCountDown = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                updateTimerDisplay()
            }
            
            override fun onFinish() {}
        }.start()
    }

    private fun pauseTimer() {
        isTimerRunning = false
        binding.btnTimer.text = "▶"
        timerCountDown?.cancel()
    }

    private fun resetTimer() {
        pauseTimer()
        elapsedSeconds = 0L
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        
        binding.textTimer.text = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun updatePageDisplay() {
        binding.textPageNumber.text = "${currentPage + 1} / $totalPages"
        
        // Update navigation button states
        binding.btnPrevious.isEnabled = currentPage > 0
        binding.btnNext.isEnabled = currentPage < totalPages - 1
        
        binding.btnPrevious.alpha = if (currentPage > 0) 1f else 0.3f
        binding.btnNext.alpha = if (currentPage < totalPages - 1) 1f else 0.3f
    }

    private fun loadDocument(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val cachedFile = withContext(Dispatchers.IO) {
                    FileUtils.copyToCache(this@ReadingModeActivity, uri)
                }
                
                cachedFile?.let { file ->
                    val htmlContent = withContext(Dispatchers.IO) {
                        when (documentType) {
                            DocumentType.DOCX -> convertDocxToReadingHtml(file)
                            DocumentType.XLSX -> convertXlsxToReadingHtml(file)
                            DocumentType.PDF -> createPdfPlaceholderHtml(file)
                            else -> createGenericHtml("Unsupported document type")
                        }
                    }
                    
                    binding.webView.loadDataWithBaseURL(
                        null,
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    binding.progressBar.visibility = View.GONE
                    updatePageDisplay()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ReadingModeActivity, "Failed to load document: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun convertDocxToReadingHtml(file: File): String {
        val htmlBuilder = StringBuilder()
        
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * {
                        box-sizing: border-box;
                    }
                    body {
                        font-family: 'Georgia', 'Times New Roman', serif;
                        margin: 0;
                        padding: 40px 60px;
                        line-height: 1.8;
                        color: #1a1a1a;
                        background-color: #faf8f5;
                        font-size: 20px;
                        max-width: 900px;
                        margin-left: auto;
                        margin-right: auto;
                    }
                    p {
                        margin: 0 0 20px 0;
                        text-align: justify;
                    }
                    h1 { 
                        font-size: 32px; 
                        font-weight: bold; 
                        margin: 40px 0 24px 0;
                        text-align: center;
                        color: #2c3e50;
                    }
                    h2 { 
                        font-size: 26px; 
                        font-weight: bold; 
                        margin: 32px 0 20px 0;
                        color: #34495e;
                    }
                    h3 { 
                        font-size: 22px; 
                        font-weight: bold; 
                        margin: 24px 0 16px 0;
                        color: #34495e;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin: 24px 0;
                        background-color: white;
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 12px 16px;
                        text-align: left;
                    }
                    th {
                        background-color: #f5f5f5;
                        font-weight: 600;
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                        margin: 20px auto;
                        display: block;
                    }
                    .center { text-align: center; }
                    .right { text-align: right; }
                    .justify { text-align: justify; }
                    ul, ol {
                        margin: 16px 0;
                        padding-left: 32px;
                    }
                    li {
                        margin: 8px 0;
                    }
                    blockquote {
                        margin: 24px 40px;
                        padding-left: 20px;
                        border-left: 4px solid #bdc3c7;
                        color: #7f8c8d;
                        font-style: italic;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        try {
            val document = XWPFDocument(FileInputStream(file))
            
            // Process body elements in order
            val bodyElements = document.bodyElements
            for (element in bodyElements) {
                when (element) {
                    is org.apache.poi.xwpf.usermodel.XWPFParagraph -> {
                        val paragraphHtml = processParagraph(element)
                        if (paragraphHtml.isNotEmpty()) {
                            htmlBuilder.append(paragraphHtml)
                        }
                    }
                    is org.apache.poi.xwpf.usermodel.XWPFTable -> {
                        htmlBuilder.append(processTable(element))
                    }
                }
            }
            
            totalPages = 1 // DOCX is single scroll
            document.close()
        } catch (e: Exception) {
            htmlBuilder.append("<p style=\"color: red;\">Error reading document: ${escapeHtml(e.message ?: "Unknown error")}</p>")
        }
        
        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
    }

    private fun processParagraph(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph): String {
        val builder = StringBuilder()
        val paragraphText = StringBuilder()
        
        // Determine heading level based on style
        val styleName = paragraph.style ?: ""
        val headingLevel = when {
            styleName.lowercase() == "heading1" || styleName == "Title" -> 1
            styleName.lowercase() == "heading2" -> 2
            styleName.lowercase() == "heading3" -> 3
            else -> 0
        }
        
        // Get alignment class
        val alignClass = when (paragraph.alignment) {
            ParagraphAlignment.CENTER -> "center"
            ParagraphAlignment.RIGHT -> "right"
            ParagraphAlignment.BOTH, ParagraphAlignment.DISTRIBUTE -> "justify"
            else -> ""
        }
        
        // Process runs
        for (run in paragraph.runs) {
            val text = run.text() ?: ""
            if (text.isEmpty() && run.embeddedPictures.isEmpty()) continue
            
            // Handle embedded pictures in run
            for (picture in run.embeddedPictures) {
                val pictureData = picture.pictureData
                val base64 = Base64.encodeToString(pictureData.data, Base64.NO_WRAP)
                val mimeType = getMimeType(pictureData)
                builder.append("<img src=\"data:$mimeType;base64,$base64\" alt=\"embedded image\" />")
            }
            
            if (text.isNotEmpty()) {
                var styledText = escapeHtml(text)
                
                // Apply formatting
                if (run.isBold) styledText = "<strong>$styledText</strong>"
                if (run.isItalic) styledText = "<em>$styledText</em>"
                if (run.underline != UnderlinePatterns.NONE) styledText = "<u>$styledText</u>"
                if (run.isStrikeThrough) styledText = "<s>$styledText</s>"
                
                // Handle font color
                val color = run.color
                if (color != null && color != "000000") {
                    styledText = "<span style=\"color:#$color\">$styledText</span>"
                }
                
                paragraphText.append(styledText)
            }
        }
        
        val content = paragraphText.toString()
        if (content.isEmpty() && builder.isEmpty()) {
            builder.append("<p>&nbsp;</p>")
        } else if (content.isNotEmpty()) {
            val tag = when (headingLevel) {
                1 -> "h1"
                2 -> "h2"
                3 -> "h3"
                else -> "p"
            }
            val classAttr = if (alignClass.isNotEmpty()) " class=\"$alignClass\"" else ""
            builder.append("<$tag$classAttr>$content</$tag>")
        }
        
        return builder.toString()
    }

    private fun processTable(table: org.apache.poi.xwpf.usermodel.XWPFTable): String {
        val builder = StringBuilder()
        builder.append("<table>")
        
        var isFirstRow = true
        for (row in table.rows) {
            builder.append("<tr>")
            for (cell in row.tableCells) {
                val tag = if (isFirstRow) "th" else "td"
                val cellText = cell.text ?: ""
                builder.append("<$tag>${escapeHtml(cellText)}</$tag>")
            }
            builder.append("</tr>")
            isFirstRow = false
        }
        
        builder.append("</table>")
        return builder.toString()
    }

    private fun convertXlsxToReadingHtml(file: File): String {
        val htmlBuilder = StringBuilder()
        
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * {
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 24px;
                        background-color: #f0f4f8;
                        font-size: 16px;
                    }
                    .sheet-container {
                        margin-bottom: 32px;
                        background-color: white;
                        border-radius: 12px;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .sheet-name {
                        font-size: 20px;
                        font-weight: bold;
                        color: #1976D2;
                        padding: 16px 20px;
                        background-color: #e3f2fd;
                        border-bottom: 1px solid #bbdefb;
                    }
                    .table-wrapper {
                        overflow-x: auto;
                        -webkit-overflow-scrolling: touch;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        min-width: 600px;
                    }
                    th, td {
                        border: 1px solid #e0e0e0;
                        padding: 12px 16px;
                        text-align: left;
                        min-width: 80px;
                        max-width: 250px;
                        word-wrap: break-word;
                    }
                    th {
                        background-color: #4CAF50;
                        color: white;
                        font-weight: 600;
                        position: sticky;
                        top: 0;
                        font-size: 14px;
                    }
                    tr:nth-child(even) {
                        background-color: #f8f9fa;
                    }
                    tr:hover {
                        background-color: #e8f5e9;
                    }
                    .row-number {
                        background-color: #f5f5f5;
                        color: #757575;
                        font-size: 13px;
                        text-align: center;
                        min-width: 50px;
                        max-width: 50px;
                    }
                    .number-cell {
                        text-align: right;
                        font-family: 'Roboto Mono', monospace;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            totalPages = workbook.numberOfSheets
            
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                val sheetName = sheet.sheetName
                
                htmlBuilder.append("<div class=\"sheet-container\">")
                htmlBuilder.append("<div class=\"sheet-name\">$sheetName</div>")
                htmlBuilder.append("<div class=\"table-wrapper\">")
                htmlBuilder.append("<table>")
                
                // Determine the maximum number of columns
                var maxColumns = 0
                for (row in sheet) {
                    if (row.lastCellNum > maxColumns) {
                        maxColumns = row.lastCellNum.toInt()
                    }
                }
                
                // Add header row (column letters)
                if (maxColumns > 0) {
                    htmlBuilder.append("<tr>")
                    htmlBuilder.append("<th class=\"row-number\">#</th>")
                    for (col in 0 until maxColumns) {
                        val colLetter = getColumnLetter(col)
                        htmlBuilder.append("<th>$colLetter</th>")
                    }
                    htmlBuilder.append("</tr>")
                }
                
                // Add data rows
                var rowNum = 1
                for (row in sheet) {
                    htmlBuilder.append("<tr>")
                    htmlBuilder.append("<td class=\"row-number\">$rowNum</td>")
                    
                    for (col in 0 until maxColumns) {
                        val cell = row.getCell(col)
                        val cellValue = getCellValueAsString(cell, dateFormat)
                        val cellClass = when {
                            cell?.cellType == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell) -> "number-cell"
                            else -> ""
                        }
                        val displayValue = if (cellValue.isEmpty()) "&nbsp;" else escapeHtml(cellValue)
                        htmlBuilder.append("<td class=\"$cellClass\">$displayValue</td>")
                    }
                    
                    htmlBuilder.append("</tr>")
                    rowNum++
                }
                
                htmlBuilder.append("</table>")
                htmlBuilder.append("</div>") // close table-wrapper
                htmlBuilder.append("</div>") // close sheet-container
            }
            
            workbook.close()
        } catch (e: Exception) {
            htmlBuilder.append("<p style=\"color: red;\">Error reading spreadsheet: ${escapeHtml(e.message ?: "Unknown error")}</p>")
        }
        
        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
    }

    private fun createPdfPlaceholderHtml(file: File): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                        background-color: #f5f5f5;
                    }
                    .container {
                        text-align: center;
                        padding: 40px;
                        background-color: white;
                        border-radius: 16px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                    }
                    .icon {
                        font-size: 64px;
                        margin-bottom: 16px;
                    }
                    h2 {
                        color: #333;
                        margin-bottom: 8px;
                    }
                    p {
                        color: #666;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">📄</div>
                    <h2>${escapeHtml(file.name)}</h2>
                    <p>PDF Reading Mode</p>
                    <p>Use the PDF viewer for full reading experience</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun createGenericHtml(message: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                        background-color: #f5f5f5;
                    }
                    .container {
                        text-align: center;
                        padding: 40px;
                    }
                    p {
                        color: #666;
                        font-size: 18px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <p>${escapeHtml(message)}</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getColumnLetter(colIndex: Int): String {
        val result = StringBuilder()
        var col = colIndex
        while (col >= 0) {
            result.insert(0, ('A' + (col % 26)))
            col = col / 26 - 1
        }
        return result.toString()
    }

    private fun getCellValueAsString(cell: org.apache.poi.ss.usermodel.Cell?, dateFormat: SimpleDateFormat): String {
        if (cell == null) return ""
        
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        dateFormat.format(cell.dateCellValue)
                    } else {
                        val numValue = cell.numericCellValue
                        if (numValue == numValue.toLong().toDouble()) {
                            numValue.toLong().toString()
                        } else {
                            String.format("%.2f", numValue)
                        }
                    }
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        cell.stringCellValue
                    } catch (e: Exception) {
                        try {
                            cell.numericCellValue.toString()
                        } catch (e2: Exception) {
                            cell.cellFormula
                        }
                    }
                }
                CellType.BLANK -> ""
                CellType.ERROR -> "#ERROR"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun getMimeType(pictureData: XWPFPictureData): String {
        return when (pictureData.pictureType) {
            org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG -> "image/png"
            org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG -> "image/jpeg"
            org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_GIF -> "image/gif"
            org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_BMP -> "image/bmp"
            else -> "image/png"
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    override fun onDestroy() {
        super.onDestroy()
        timerCountDown?.cancel()
    }

    companion object {
        const val EXTRA_FILE_URI = "file_uri"
        const val EXTRA_DOCUMENT_TYPE = "document_type"
    }
}
