package com.officesuite.app.spreadsheet

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Spreadsheet Power Tools for Phase 2 features
 * Includes conditional formatting, filtering, sorting, charts, and more
 */
class SpreadsheetTools(private val context: Context) {

    /**
     * Conditional formatting rule configuration
     */
    data class ConditionalFormatRule(
        val type: FormatType,
        val operator: ComparisonOperator,
        val value1: String,
        val value2: String? = null,
        val backgroundColor: String = "#FFEB3B",
        val fontColor: String? = null,
        val bold: Boolean = false
    )

    enum class FormatType {
        CELL_VALUE, TEXT_CONTAINS, DUPLICATE, TOP_N, BOTTOM_N, ABOVE_AVERAGE, BELOW_AVERAGE
    }

    enum class ComparisonOperator {
        EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL, LESS_OR_EQUAL, BETWEEN
    }

    /**
     * Chart configuration
     */
    data class ChartConfig(
        val type: ChartType,
        val title: String,
        val dataRange: String,
        val categoryRange: String?,
        val position: ChartPosition = ChartPosition(0, 5, 15, 20),
        val showLegend: Boolean = true,
        val is3D: Boolean = false
    )

    data class ChartPosition(
        val startRow: Int,
        val startCol: Int,
        val endRow: Int,
        val endCol: Int
    )

    enum class ChartType {
        BAR, LINE, PIE, AREA, SCATTER, COLUMN
    }

    /**
     * Sort configuration
     */
    data class SortConfig(
        val columnIndex: Int,
        val ascending: Boolean = true,
        val caseSensitive: Boolean = false
    )

    /**
     * Filter configuration
     */
    data class FilterConfig(
        val columnIndex: Int,
        val filterType: FilterType,
        val values: List<String>
    )

    enum class FilterType {
        EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, GREATER_THAN, LESS_THAN
    }

    /**
     * Apply conditional formatting to a range
     */
    suspend fun applyConditionalFormatting(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        range: String,
        rule: ConditionalFormatRule
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex)
            val sheetCF = sheet.sheetConditionalFormatting
            
            val cfRule = when (rule.type) {
                FormatType.CELL_VALUE -> {
                    sheetCF.createConditionalFormattingRule(
                        getComparisonOperator(rule.operator),
                        rule.value1,
                        rule.value2
                    )
                }
                FormatType.TEXT_CONTAINS -> {
                    sheetCF.createConditionalFormattingRule("SEARCH(\"${rule.value1}\",A1)>0")
                }
                FormatType.DUPLICATE -> {
                    sheetCF.createConditionalFormattingRule("COUNTIF(\$A:\$A,A1)>1")
                }
                FormatType.TOP_N -> {
                    sheetCF.createConditionalFormattingRule("RANK(A1,\$A:\$A)<=${rule.value1}")
                }
                FormatType.BOTTOM_N -> {
                    sheetCF.createConditionalFormattingRule("RANK(A1,\$A:\$A,1)<=${rule.value1}")
                }
                FormatType.ABOVE_AVERAGE -> {
                    sheetCF.createConditionalFormattingRule("A1>AVERAGE(\$A:\$A)")
                }
                FormatType.BELOW_AVERAGE -> {
                    sheetCF.createConditionalFormattingRule("A1<AVERAGE(\$A:\$A)")
                }
            }
            
            // Set pattern formatting
            val patternFormatting = cfRule.createPatternFormatting()
            patternFormatting.fillBackgroundColor = parseColorIndex(rule.backgroundColor)
            patternFormatting.fillPattern = PatternFormatting.SOLID_FOREGROUND
            
            // Apply to range
            val regions = arrayOf(CellRangeAddress.valueOf(range))
            sheetCF.addConditionalFormatting(regions, cfRule)
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Comparison operator byte codes for Apache POI conditional formatting
     * These values correspond to org.apache.poi.ss.usermodel.ComparisonOperator constants
     */
    private companion object {
        const val CF_OPERATOR_BETWEEN: Byte = 1
        const val CF_OPERATOR_EQUAL: Byte = 3
        const val CF_OPERATOR_GREATER_THAN: Byte = 5
        const val CF_OPERATOR_GREATER_OR_EQUAL: Byte = 7
        const val CF_OPERATOR_LESS_THAN: Byte = 6
        const val CF_OPERATOR_LESS_OR_EQUAL: Byte = 8
        const val CF_OPERATOR_NOT_EQUAL: Byte = 4
    }

    private fun getComparisonOperator(op: ComparisonOperator): Byte {
        return when (op) {
            ComparisonOperator.EQUAL -> CF_OPERATOR_EQUAL
            ComparisonOperator.NOT_EQUAL -> CF_OPERATOR_NOT_EQUAL
            ComparisonOperator.GREATER_THAN -> CF_OPERATOR_GREATER_THAN
            ComparisonOperator.LESS_THAN -> CF_OPERATOR_LESS_THAN
            ComparisonOperator.GREATER_OR_EQUAL -> CF_OPERATOR_GREATER_OR_EQUAL
            ComparisonOperator.LESS_OR_EQUAL -> CF_OPERATOR_LESS_OR_EQUAL
            ComparisonOperator.BETWEEN -> CF_OPERATOR_BETWEEN
        }
    }

    private fun parseColorIndex(hex: String): Short {
        // Return a predefined index color for simplicity
        return when (hex.uppercase()) {
            "#FFEB3B", "#FFFF00" -> IndexedColors.YELLOW.index
            "#FF0000" -> IndexedColors.RED.index
            "#00FF00" -> IndexedColors.GREEN.index
            "#0000FF" -> IndexedColors.BLUE.index
            "#FF9800", "#FFA500" -> IndexedColors.ORANGE.index
            "#E91E63", "#FF69B4" -> IndexedColors.PINK.index
            else -> IndexedColors.YELLOW.index
        }
    }

    /**
     * Create a chart in the spreadsheet
     * Note: Chart creation is simplified due to Apache POI API limitations on Android
     */
    suspend fun createChart(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        config: ChartConfig
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex) as XSSFSheet
            
            val drawing = sheet.createDrawingPatriarch()
            val anchor = drawing.createAnchor(
                0, 0, 0, 0,
                config.position.startCol, config.position.startRow,
                config.position.endCol, config.position.endRow
            )
            
            val chart = drawing.createChart(anchor)
            chart.setTitleText(config.title)
            
            // Note: Full chart data binding requires XDDF API which has 
            // limited compatibility on Android. The chart structure is created
            // but data binding may need to be done on a full JVM environment.
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Sort data in a sheet
     */
    suspend fun sortData(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        dataRange: String,
        sortConfigs: List<SortConfig>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex)
            
            val range = CellRangeAddress.valueOf(dataRange)
            val startRow = range.firstRow
            val endRow = range.lastRow
            val startCol = range.firstColumn
            val endCol = range.lastColumn
            
            // Extract data to sort
            val rows = mutableListOf<List<Pair<Int, Cell?>>>()
            for (rowIdx in startRow..endRow) {
                val row = sheet.getRow(rowIdx) ?: sheet.createRow(rowIdx)
                val cells = mutableListOf<Pair<Int, Cell?>>()
                for (colIdx in startCol..endCol) {
                    cells.add(Pair(colIdx, row.getCell(colIdx)))
                }
                rows.add(cells)
            }
            
            // Sort by each config in reverse order (last has highest priority)
            var sortedRows = rows.toMutableList()
            sortConfigs.reversed().forEach { config ->
                sortedRows = sortedRows.sortedWith { a, b ->
                    val cellA = a.find { it.first == config.columnIndex + startCol }?.second
                    val cellB = b.find { it.first == config.columnIndex + startCol }?.second
                    val comparison = compareCells(cellA, cellB, config.caseSensitive)
                    if (config.ascending) comparison else -comparison
                }.toMutableList()
            }
            
            // Write sorted data back
            sortedRows.forEachIndexed { idx, rowCells ->
                val row = sheet.getRow(startRow + idx) ?: sheet.createRow(startRow + idx)
                rowCells.forEach { (colIdx, cell) ->
                    val newCell = row.createCell(colIdx)
                    cell?.let { copyCellValue(it, newCell) }
                }
            }
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compareCells(a: Cell?, b: Cell?, caseSensitive: Boolean): Int {
        if (a == null && b == null) return 0
        if (a == null) return -1
        if (b == null) return 1
        
        return try {
            when {
                a.cellType == CellType.NUMERIC && b.cellType == CellType.NUMERIC -> {
                    a.numericCellValue.compareTo(b.numericCellValue)
                }
                else -> {
                    val strA = getCellStringValue(a)
                    val strB = getCellStringValue(b)
                    if (caseSensitive) strA.compareTo(strB) else strA.compareTo(strB, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun getCellStringValue(cell: Cell): String {
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> cell.cellFormula
            else -> ""
        }
    }

    private fun copyCellValue(source: Cell, target: Cell) {
        when (source.cellType) {
            CellType.STRING -> target.setCellValue(source.stringCellValue)
            CellType.NUMERIC -> target.setCellValue(source.numericCellValue)
            CellType.BOOLEAN -> target.setCellValue(source.booleanCellValue)
            CellType.FORMULA -> target.setCellFormula(source.cellFormula)
            CellType.BLANK -> target.setBlank()
            else -> {}
        }
    }

    /**
     * Filter data in a sheet
     */
    suspend fun filterData(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        filterConfigs: List<FilterConfig>
    ): FilterResult = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex)
            
            var hiddenCount = 0
            var visibleCount = 0
            
            for (rowIdx in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIdx) ?: continue
                var shouldHide = false
                
                filterConfigs.forEach { config ->
                    val cell = row.getCell(config.columnIndex)
                    val cellValue = cell?.let { getCellStringValue(it) } ?: ""
                    
                    val matches = when (config.filterType) {
                        FilterType.EQUALS -> config.values.any { it.equals(cellValue, ignoreCase = true) }
                        FilterType.NOT_EQUALS -> config.values.none { it.equals(cellValue, ignoreCase = true) }
                        FilterType.CONTAINS -> config.values.any { cellValue.contains(it, ignoreCase = true) }
                        FilterType.STARTS_WITH -> config.values.any { cellValue.startsWith(it, ignoreCase = true) }
                        FilterType.ENDS_WITH -> config.values.any { cellValue.endsWith(it, ignoreCase = true) }
                        FilterType.GREATER_THAN -> {
                            val num = cellValue.toDoubleOrNull()
                            val filterNum = config.values.firstOrNull()?.toDoubleOrNull()
                            num != null && filterNum != null && num > filterNum
                        }
                        FilterType.LESS_THAN -> {
                            val num = cellValue.toDoubleOrNull()
                            val filterNum = config.values.firstOrNull()?.toDoubleOrNull()
                            num != null && filterNum != null && num < filterNum
                        }
                    }
                    
                    if (!matches) shouldHide = true
                }
                
                row.zeroHeight = shouldHide
                if (shouldHide) hiddenCount++ else visibleCount++
            }
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            
            FilterResult(
                success = true,
                totalRows = hiddenCount + visibleCount,
                visibleRows = visibleCount,
                hiddenRows = hiddenCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            FilterResult(success = false, totalRows = 0, visibleRows = 0, hiddenRows = 0)
        }
    }

    data class FilterResult(
        val success: Boolean,
        val totalRows: Int,
        val visibleRows: Int,
        val hiddenRows: Int
    )

    /**
     * Freeze panes (rows and columns)
     */
    suspend fun freezePanes(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        freezeRow: Int,
        freezeColumn: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex)
            
            sheet.createFreezePane(freezeColumn, freezeRow)
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Merge cells in a range
     */
    suspend fun mergeCells(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        range: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex)
            
            val cellRange = CellRangeAddress.valueOf(range)
            sheet.addMergedRegion(cellRange)
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Unmerge cells in a range
     */
    suspend fun unmergeCells(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        range: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex)
            
            val targetRange = CellRangeAddress.valueOf(range)
            val mergedRegions = sheet.mergedRegions
            val regionsToRemove = mutableListOf<Int>()
            
            mergedRegions.forEachIndexed { index, region ->
                if (region.intersects(targetRange)) {
                    regionsToRemove.add(index)
                }
            }
            
            // Remove in reverse order to maintain indices
            regionsToRemove.reversed().forEach { sheet.removeMergedRegion(it) }
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Add cell comment/note
     */
    suspend fun addCellComment(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        row: Int,
        col: Int,
        comment: String,
        author: String = "User"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex) as XSSFSheet
            val factory = workbook.creationHelper
            
            val sheetRow = sheet.getRow(row) ?: sheet.createRow(row)
            val cell = sheetRow.getCell(col) ?: sheetRow.createCell(col)
            
            val drawing = sheet.createDrawingPatriarch()
            val anchor = factory.createClientAnchor()
            anchor.setCol1(col)
            anchor.setCol2(col + 2)
            anchor.setRow1(row)
            anchor.setRow2(row + 3)
            
            val cellComment = drawing.createCellComment(anchor)
            cellComment.string = factory.createRichTextString(comment)
            cellComment.author = author
            cell.cellComment = cellComment
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Find and replace text in spreadsheet
     */
    suspend fun findAndReplace(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        findText: String,
        replaceText: String,
        caseSensitive: Boolean = false,
        matchWholeCell: Boolean = false
    ): FindReplaceResult = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex)
            var replacementCount = 0
            
            for (row in sheet) {
                for (cell in row) {
                    if (cell.cellType == CellType.STRING) {
                        val cellValue = cell.stringCellValue
                        
                        val matches = if (matchWholeCell) {
                            if (caseSensitive) cellValue == findText else cellValue.equals(findText, ignoreCase = true)
                        } else {
                            if (caseSensitive) cellValue.contains(findText) else cellValue.contains(findText, ignoreCase = true)
                        }
                        
                        if (matches) {
                            val newValue = if (matchWholeCell) {
                                replaceText
                            } else {
                                if (caseSensitive) {
                                    cellValue.replace(findText, replaceText)
                                } else {
                                    cellValue.replace(findText, replaceText, ignoreCase = true)
                                }
                            }
                            cell.setCellValue(newValue)
                            replacementCount++
                        }
                    }
                }
            }
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            
            FindReplaceResult(success = true, replacementCount = replacementCount)
        } catch (e: Exception) {
            e.printStackTrace()
            FindReplaceResult(success = false, replacementCount = 0)
        }
    }

    data class FindReplaceResult(
        val success: Boolean,
        val replacementCount: Int
    )

    /**
     * Apply number formatting to cells
     */
    suspend fun applyNumberFormat(
        file: File,
        outputFile: File,
        sheetIndex: Int,
        range: String,
        formatType: NumberFormatType
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook(FileInputStream(file))
            val sheet = workbook.getSheetAt(sheetIndex)
            
            val cellRange = CellRangeAddress.valueOf(range)
            val format = when (formatType) {
                NumberFormatType.CURRENCY_USD -> "$#,##0.00"
                NumberFormatType.CURRENCY_EUR -> "€#,##0.00"
                NumberFormatType.PERCENTAGE -> "0.00%"
                NumberFormatType.DATE_SHORT -> "MM/dd/yyyy"
                NumberFormatType.DATE_LONG -> "MMMM d, yyyy"
                NumberFormatType.TIME -> "h:mm AM/PM"
                NumberFormatType.DATETIME -> "MM/dd/yyyy h:mm AM/PM"
                NumberFormatType.NUMBER_COMMA -> "#,##0"
                NumberFormatType.NUMBER_DECIMAL -> "#,##0.00"
                NumberFormatType.SCIENTIFIC -> "0.00E+00"
            }
            
            val dataFormat = workbook.createDataFormat()
            val cellStyle = workbook.createCellStyle()
            cellStyle.dataFormat = dataFormat.getFormat(format)
            
            for (rowIdx in cellRange.firstRow..cellRange.lastRow) {
                val row = sheet.getRow(rowIdx) ?: continue
                for (colIdx in cellRange.firstColumn..cellRange.lastColumn) {
                    val cell = row.getCell(colIdx) ?: continue
                    cell.cellStyle = cellStyle
                }
            }
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    enum class NumberFormatType {
        CURRENCY_USD, CURRENCY_EUR, PERCENTAGE, 
        DATE_SHORT, DATE_LONG, TIME, DATETIME,
        NUMBER_COMMA, NUMBER_DECIMAL, SCIENTIFIC
    }
}
