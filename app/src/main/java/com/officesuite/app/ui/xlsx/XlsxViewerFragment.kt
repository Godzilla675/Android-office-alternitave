package com.officesuite.app.ui.xlsx

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentXlsxViewerBinding
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Locale

class XlsxViewerFragment : Fragment() {

    private var _binding: FragmentXlsxViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var cachedFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentXlsxViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupWebView()
        setupToolbar()
        
        arguments?.getString("file_uri")?.let { uriString ->
            fileUri = Uri.parse(uriString)
            loadDocument()
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
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            inflateMenu(R.menu.menu_xlsx_viewer)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_share -> {
                        shareDocument()
                        true
                    }
                    R.id.action_convert -> {
                        convertToPdf()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun loadDocument() {
        fileUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                try {
                    cachedFile = withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }
                    
                    cachedFile?.let { file ->
                        val htmlContent = withContext(Dispatchers.IO) {
                            convertXlsxToHtml(file)
                        }
                        
                        binding.toolbar.title = file.name
                        binding.webView.loadDataWithBaseURL(
                            null,
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                        binding.progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load spreadsheet: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun convertXlsxToHtml(file: File): String {
        val htmlBuilder = StringBuilder()
        
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 8px;
                        background-color: #f5f5f5;
                    }
                    .sheet-container {
                        margin-bottom: 24px;
                    }
                    .sheet-name {
                        font-size: 16px;
                        font-weight: bold;
                        color: #1976D2;
                        margin-bottom: 8px;
                        padding: 8px;
                        background-color: #e3f2fd;
                        border-radius: 4px;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        background-color: white;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.12);
                        overflow-x: auto;
                        display: block;
                    }
                    th, td {
                        border: 1px solid #e0e0e0;
                        padding: 8px 12px;
                        text-align: left;
                        min-width: 60px;
                        max-width: 200px;
                        word-wrap: break-word;
                    }
                    th {
                        background-color: #4CAF50;
                        color: white;
                        font-weight: 600;
                        position: sticky;
                        top: 0;
                    }
                    tr:nth-child(even) {
                        background-color: #fafafa;
                    }
                    tr:hover {
                        background-color: #e8f5e9;
                    }
                    .row-number {
                        background-color: #f5f5f5;
                        color: #757575;
                        font-size: 12px;
                        text-align: center;
                        min-width: 40px;
                        max-width: 40px;
                    }
                    .empty-cell {
                        color: #bdbdbd;
                    }
                    .number-cell {
                        text-align: right;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                val sheetName = sheet.sheetName
                
                htmlBuilder.append("<div class=\"sheet-container\">")
                htmlBuilder.append("<div class=\"sheet-name\">$sheetName</div>")
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
                            cellValue.isEmpty() -> "empty-cell"
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
                htmlBuilder.append("</div>")
            }
            
            workbook.close()
        } catch (e: Exception) {
            htmlBuilder.append("<p style=\"color: red;\">Error reading spreadsheet: ${escapeHtml(e.message ?: "Unknown error")}</p>")
        }
        
        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
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

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun shareDocument() {
        cachedFile?.let { file ->
            ShareUtils.shareFile(
                requireContext(),
                file,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        }
    }

    private fun convertToPdf() {
        Toast.makeText(context, "Converting to PDF...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
