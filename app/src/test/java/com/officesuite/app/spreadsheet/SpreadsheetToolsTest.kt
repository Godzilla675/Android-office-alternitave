package com.officesuite.app.spreadsheet

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SpreadsheetTools class data classes and enums.
 */
class SpreadsheetToolsTest {

    @Test
    fun `FormatType enum has all expected values`() {
        val types = SpreadsheetTools.FormatType.values()
        
        assertEquals(7, types.size)
        assertTrue(types.contains(SpreadsheetTools.FormatType.CELL_VALUE))
        assertTrue(types.contains(SpreadsheetTools.FormatType.TEXT_CONTAINS))
        assertTrue(types.contains(SpreadsheetTools.FormatType.DUPLICATE))
        assertTrue(types.contains(SpreadsheetTools.FormatType.TOP_N))
        assertTrue(types.contains(SpreadsheetTools.FormatType.BOTTOM_N))
        assertTrue(types.contains(SpreadsheetTools.FormatType.ABOVE_AVERAGE))
        assertTrue(types.contains(SpreadsheetTools.FormatType.BELOW_AVERAGE))
    }

    @Test
    fun `ComparisonOperator enum has all expected values`() {
        val operators = SpreadsheetTools.ComparisonOperator.values()
        
        assertEquals(7, operators.size)
        assertTrue(operators.contains(SpreadsheetTools.ComparisonOperator.EQUAL))
        assertTrue(operators.contains(SpreadsheetTools.ComparisonOperator.NOT_EQUAL))
        assertTrue(operators.contains(SpreadsheetTools.ComparisonOperator.GREATER_THAN))
        assertTrue(operators.contains(SpreadsheetTools.ComparisonOperator.LESS_THAN))
        assertTrue(operators.contains(SpreadsheetTools.ComparisonOperator.GREATER_OR_EQUAL))
        assertTrue(operators.contains(SpreadsheetTools.ComparisonOperator.LESS_OR_EQUAL))
        assertTrue(operators.contains(SpreadsheetTools.ComparisonOperator.BETWEEN))
    }

    @Test
    fun `ChartType enum has all expected values`() {
        val types = SpreadsheetTools.ChartType.values()
        
        assertEquals(6, types.size)
        assertTrue(types.contains(SpreadsheetTools.ChartType.BAR))
        assertTrue(types.contains(SpreadsheetTools.ChartType.LINE))
        assertTrue(types.contains(SpreadsheetTools.ChartType.PIE))
        assertTrue(types.contains(SpreadsheetTools.ChartType.AREA))
        assertTrue(types.contains(SpreadsheetTools.ChartType.SCATTER))
        assertTrue(types.contains(SpreadsheetTools.ChartType.COLUMN))
    }

    @Test
    fun `FilterType enum has all expected values`() {
        val types = SpreadsheetTools.FilterType.values()
        
        assertEquals(7, types.size)
        assertTrue(types.contains(SpreadsheetTools.FilterType.EQUALS))
        assertTrue(types.contains(SpreadsheetTools.FilterType.NOT_EQUALS))
        assertTrue(types.contains(SpreadsheetTools.FilterType.CONTAINS))
        assertTrue(types.contains(SpreadsheetTools.FilterType.STARTS_WITH))
        assertTrue(types.contains(SpreadsheetTools.FilterType.ENDS_WITH))
        assertTrue(types.contains(SpreadsheetTools.FilterType.GREATER_THAN))
        assertTrue(types.contains(SpreadsheetTools.FilterType.LESS_THAN))
    }

    @Test
    fun `NumberFormatType enum has all expected values`() {
        val types = SpreadsheetTools.NumberFormatType.values()
        
        assertEquals(10, types.size)
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.CURRENCY_USD))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.CURRENCY_EUR))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.PERCENTAGE))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.DATE_SHORT))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.DATE_LONG))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.TIME))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.DATETIME))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.NUMBER_COMMA))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.NUMBER_DECIMAL))
        assertTrue(types.contains(SpreadsheetTools.NumberFormatType.SCIENTIFIC))
    }

    @Test
    fun `ConditionalFormatRule holds values correctly`() {
        val rule = SpreadsheetTools.ConditionalFormatRule(
            type = SpreadsheetTools.FormatType.CELL_VALUE,
            operator = SpreadsheetTools.ComparisonOperator.GREATER_THAN,
            value1 = "100",
            value2 = null,
            backgroundColor = "#FF0000",
            fontColor = "#FFFFFF",
            bold = true
        )
        
        assertEquals(SpreadsheetTools.FormatType.CELL_VALUE, rule.type)
        assertEquals(SpreadsheetTools.ComparisonOperator.GREATER_THAN, rule.operator)
        assertEquals("100", rule.value1)
        assertNull(rule.value2)
        assertEquals("#FF0000", rule.backgroundColor)
        assertEquals("#FFFFFF", rule.fontColor)
        assertTrue(rule.bold)
    }

    @Test
    fun `ConditionalFormatRule default values`() {
        val rule = SpreadsheetTools.ConditionalFormatRule(
            type = SpreadsheetTools.FormatType.DUPLICATE,
            operator = SpreadsheetTools.ComparisonOperator.EQUAL,
            value1 = ""
        )
        
        assertEquals("#FFEB3B", rule.backgroundColor)
        assertNull(rule.fontColor)
        assertFalse(rule.bold)
    }

    @Test
    fun `ConditionalFormatRule with BETWEEN operator has two values`() {
        val rule = SpreadsheetTools.ConditionalFormatRule(
            type = SpreadsheetTools.FormatType.CELL_VALUE,
            operator = SpreadsheetTools.ComparisonOperator.BETWEEN,
            value1 = "10",
            value2 = "100"
        )
        
        assertEquals("10", rule.value1)
        assertEquals("100", rule.value2)
    }

    @Test
    fun `ChartConfig holds values correctly`() {
        val config = SpreadsheetTools.ChartConfig(
            type = SpreadsheetTools.ChartType.BAR,
            title = "Sales Chart",
            dataRange = "B2:B10",
            categoryRange = "A2:A10",
            position = SpreadsheetTools.ChartPosition(0, 5, 15, 20),
            showLegend = true,
            is3D = false
        )
        
        assertEquals(SpreadsheetTools.ChartType.BAR, config.type)
        assertEquals("Sales Chart", config.title)
        assertEquals("B2:B10", config.dataRange)
        assertEquals("A2:A10", config.categoryRange)
        assertTrue(config.showLegend)
        assertFalse(config.is3D)
    }

    @Test
    fun `ChartConfig default values`() {
        val config = SpreadsheetTools.ChartConfig(
            type = SpreadsheetTools.ChartType.LINE,
            title = "Test Chart",
            dataRange = "A1:A10",
            categoryRange = null
        )
        
        assertEquals(SpreadsheetTools.ChartPosition(0, 5, 15, 20), config.position)
        assertTrue(config.showLegend)
        assertFalse(config.is3D)
    }

    @Test
    fun `ChartPosition holds values correctly`() {
        val position = SpreadsheetTools.ChartPosition(
            startRow = 5,
            startCol = 10,
            endRow = 20,
            endCol = 25
        )
        
        assertEquals(5, position.startRow)
        assertEquals(10, position.startCol)
        assertEquals(20, position.endRow)
        assertEquals(25, position.endCol)
    }

    @Test
    fun `SortConfig holds values correctly`() {
        val config = SpreadsheetTools.SortConfig(
            columnIndex = 2,
            ascending = false,
            caseSensitive = true
        )
        
        assertEquals(2, config.columnIndex)
        assertFalse(config.ascending)
        assertTrue(config.caseSensitive)
    }

    @Test
    fun `SortConfig default values`() {
        val config = SpreadsheetTools.SortConfig(columnIndex = 0)
        
        assertTrue(config.ascending)
        assertFalse(config.caseSensitive)
    }

    @Test
    fun `FilterConfig holds values correctly`() {
        val config = SpreadsheetTools.FilterConfig(
            columnIndex = 1,
            filterType = SpreadsheetTools.FilterType.CONTAINS,
            values = listOf("Apple", "Banana")
        )
        
        assertEquals(1, config.columnIndex)
        assertEquals(SpreadsheetTools.FilterType.CONTAINS, config.filterType)
        assertEquals(2, config.values.size)
        assertTrue(config.values.contains("Apple"))
    }

    @Test
    fun `FilterResult success state`() {
        val result = SpreadsheetTools.FilterResult(
            success = true,
            totalRows = 100,
            visibleRows = 75,
            hiddenRows = 25
        )
        
        assertTrue(result.success)
        assertEquals(100, result.totalRows)
        assertEquals(75, result.visibleRows)
        assertEquals(25, result.hiddenRows)
    }

    @Test
    fun `FilterResult failure state`() {
        val result = SpreadsheetTools.FilterResult(
            success = false,
            totalRows = 0,
            visibleRows = 0,
            hiddenRows = 0
        )
        
        assertFalse(result.success)
    }

    @Test
    fun `FindReplaceResult success with replacements`() {
        val result = SpreadsheetTools.FindReplaceResult(
            success = true,
            replacementCount = 15
        )
        
        assertTrue(result.success)
        assertEquals(15, result.replacementCount)
    }

    @Test
    fun `FindReplaceResult success with no replacements`() {
        val result = SpreadsheetTools.FindReplaceResult(
            success = true,
            replacementCount = 0
        )
        
        assertTrue(result.success)
        assertEquals(0, result.replacementCount)
    }

    @Test
    fun `FindReplaceResult failure`() {
        val result = SpreadsheetTools.FindReplaceResult(
            success = false,
            replacementCount = 0
        )
        
        assertFalse(result.success)
    }

    @Test
    fun `ChartConfig with 3D enabled`() {
        val config = SpreadsheetTools.ChartConfig(
            type = SpreadsheetTools.ChartType.PIE,
            title = "3D Pie Chart",
            dataRange = "A1:A10",
            categoryRange = "B1:B10",
            is3D = true
        )
        
        assertTrue(config.is3D)
        assertEquals(SpreadsheetTools.ChartType.PIE, config.type)
    }

    @Test
    fun `ChartConfig without legend`() {
        val config = SpreadsheetTools.ChartConfig(
            type = SpreadsheetTools.ChartType.SCATTER,
            title = "Scatter Plot",
            dataRange = "C1:C50",
            categoryRange = "D1:D50",
            showLegend = false
        )
        
        assertFalse(config.showLegend)
    }

    @Test
    fun `Multiple SortConfig for multi-column sort`() {
        val configs = listOf(
            SpreadsheetTools.SortConfig(columnIndex = 0, ascending = true),
            SpreadsheetTools.SortConfig(columnIndex = 1, ascending = false)
        )
        
        assertEquals(2, configs.size)
        assertTrue(configs[0].ascending)
        assertFalse(configs[1].ascending)
    }

    @Test
    fun `FilterConfig with single value`() {
        val config = SpreadsheetTools.FilterConfig(
            columnIndex = 0,
            filterType = SpreadsheetTools.FilterType.EQUALS,
            values = listOf("Active")
        )
        
        assertEquals(1, config.values.size)
        assertEquals("Active", config.values[0])
    }

    @Test
    fun `FilterConfig with numeric comparison`() {
        val config = SpreadsheetTools.FilterConfig(
            columnIndex = 2,
            filterType = SpreadsheetTools.FilterType.GREATER_THAN,
            values = listOf("100")
        )
        
        assertEquals(SpreadsheetTools.FilterType.GREATER_THAN, config.filterType)
        assertEquals("100", config.values[0])
    }
}
