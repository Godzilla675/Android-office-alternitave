package com.officesuite.app.automation

import android.content.Context
import android.net.Uri
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.regex.Pattern

/**
 * Document Automation and Workflow Tools for Phase 2 features
 * Includes mail merge, batch processing, document templates, and automation
 */
class DocumentAutomation(private val context: Context) {

    /**
     * Mail merge configuration
     */
    data class MailMergeConfig(
        val templateFile: File,
        val dataFile: File,
        val outputDirectory: File,
        val outputPrefix: String = "merged_",
        val placeholderFormat: PlaceholderFormat = PlaceholderFormat.CURLY_BRACES
    )

    enum class PlaceholderFormat {
        CURLY_BRACES,      // {{fieldName}}
        ANGLE_BRACKETS,    // <<fieldName>>
        SQUARE_BRACKETS,   // [[fieldName]]
        DOLLAR_SIGN        // $fieldName$
    }

    /**
     * Batch processing configuration
     */
    data class BatchProcessConfig(
        val inputFiles: List<File>,
        val outputDirectory: File,
        val operations: List<BatchOperation>
    )

    sealed class BatchOperation {
        data class ConvertToPdf(val quality: Int = 100) : BatchOperation()
        data class AddWatermark(val text: String, val opacity: Float = 0.3f) : BatchOperation()
        data class Rename(val pattern: String, val replacement: String) : BatchOperation()
        data class Compress(val compressionLevel: Int = 7) : BatchOperation()
        object ExtractText : BatchOperation()
        data class AddPrefix(val prefix: String) : BatchOperation()
        data class AddSuffix(val suffix: String) : BatchOperation()
    }

    /**
     * Template variable configuration
     */
    data class TemplateVariable(
        val name: String,
        val value: String,
        val type: VariableType = VariableType.TEXT
    )

    enum class VariableType {
        TEXT, NUMBER, DATE, IMAGE, TABLE
    }

    /**
     * Workflow step configuration
     */
    data class WorkflowStep(
        val name: String,
        val action: WorkflowAction,
        val continueOnError: Boolean = false
    )

    sealed class WorkflowAction {
        data class ConvertFormat(val targetFormat: String) : WorkflowAction()
        data class ApplyTemplate(val templateFile: File) : WorkflowAction()
        data class MergeDocuments(val outputName: String) : WorkflowAction()
        data class SendEmail(val recipient: String, val subject: String) : WorkflowAction()
        data class SaveToCloud(val provider: String, val path: String) : WorkflowAction()
        data class ExtractData(val fields: List<String>) : WorkflowAction()
        data class ValidateDocument(val rules: List<String>) : WorkflowAction()
    }

    /**
     * Perform mail merge with DOCX template and XLSX data
     */
    suspend fun performMailMerge(config: MailMergeConfig): MailMergeResult = withContext(Dispatchers.IO) {
        try {
            // Load data from spreadsheet
            val dataRecords = loadSpreadsheetData(config.dataFile)
            if (dataRecords.isEmpty()) {
                return@withContext MailMergeResult(
                    success = false,
                    generatedFiles = emptyList(),
                    errors = listOf("No data records found in spreadsheet")
                )
            }

            val generatedFiles = mutableListOf<File>()
            val errors = mutableListOf<String>()
            val placeholderPattern = getPlaceholderPattern(config.placeholderFormat)

            // Generate document for each record
            dataRecords.forEachIndexed { index, record ->
                try {
                    val outputFile = File(
                        config.outputDirectory,
                        "${config.outputPrefix}${index + 1}.docx"
                    )

                    mergeDocument(
                        templateFile = config.templateFile,
                        outputFile = outputFile,
                        data = record,
                        pattern = placeholderPattern
                    )

                    generatedFiles.add(outputFile)
                } catch (e: Exception) {
                    errors.add("Error processing record ${index + 1}: ${e.message}")
                }
            }

            MailMergeResult(
                success = errors.isEmpty(),
                generatedFiles = generatedFiles,
                errors = errors,
                totalRecords = dataRecords.size,
                successfulMerges = generatedFiles.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MailMergeResult(
                success = false,
                generatedFiles = emptyList(),
                errors = listOf("Mail merge failed: ${e.message}")
            )
        }
    }

    private fun loadSpreadsheetData(file: File): List<Map<String, String>> {
        val records = mutableListOf<Map<String, String>>()
        
        val workbook = XSSFWorkbook(FileInputStream(file))
        val sheet = workbook.getSheetAt(0)
        
        // Get headers from first row
        val headerRow = sheet.getRow(0) ?: return emptyList()
        val headers = mutableListOf<String>()
        for (i in 0 until headerRow.lastCellNum) {
            val cell = headerRow.getCell(i)
            headers.add(getCellValue(cell))
        }
        
        // Get data from remaining rows
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val record = mutableMapOf<String, String>()
            
            headers.forEachIndexed { colIndex, header ->
                val cell = row.getCell(colIndex)
                record[header] = getCellValue(cell)
            }
            
            if (record.values.any { it.isNotBlank() }) {
                records.add(record)
            }
        }
        
        workbook.close()
        return records
    }

    private fun getCellValue(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cell.dateCellValue)
                } else {
                    val num = cell.numericCellValue
                    if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                }
            }
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }

    private fun getPlaceholderPattern(format: PlaceholderFormat): Pattern {
        val regex = when (format) {
            PlaceholderFormat.CURLY_BRACES -> "\\{\\{(\\w+)\\}\\}"
            PlaceholderFormat.ANGLE_BRACKETS -> "<<(\\w+)>>"
            PlaceholderFormat.SQUARE_BRACKETS -> "\\[\\[(\\w+)\\]\\]"
            PlaceholderFormat.DOLLAR_SIGN -> "\\$(\\w+)\\$"
        }
        return Pattern.compile(regex)
    }

    private fun mergeDocument(
        templateFile: File,
        outputFile: File,
        data: Map<String, String>,
        pattern: Pattern
    ) {
        val doc = XWPFDocument(FileInputStream(templateFile))
        
        // Replace in paragraphs
        doc.paragraphs.forEach { paragraph ->
            val runs = paragraph.runs
            runs.forEach { run ->
                var text = run.text() ?: return@forEach
                val matcher = pattern.matcher(text)
                
                while (matcher.find()) {
                    val fieldName = matcher.group(1)
                    val value = data[fieldName] ?: ""
                    text = text.replace(matcher.group(), value)
                }
                
                run.setText(text, 0)
            }
        }
        
        // Replace in tables
        doc.tables.forEach { table ->
            table.rows.forEach { row ->
                row.tableCells.forEach { cell ->
                    cell.paragraphs.forEach { paragraph ->
                        paragraph.runs.forEach { run ->
                            var text = run.text() ?: return@forEach
                            val matcher = pattern.matcher(text)
                            
                            while (matcher.find()) {
                                val fieldName = matcher.group(1)
                                val value = data[fieldName] ?: ""
                                text = text.replace(matcher.group(), value)
                            }
                            
                            run.setText(text, 0)
                        }
                    }
                }
            }
        }
        
        FileOutputStream(outputFile).use { doc.write(it) }
        doc.close()
    }

    data class MailMergeResult(
        val success: Boolean,
        val generatedFiles: List<File>,
        val errors: List<String>,
        val totalRecords: Int = 0,
        val successfulMerges: Int = 0
    )

    /**
     * Perform batch processing on multiple files
     */
    suspend fun performBatchProcessing(config: BatchProcessConfig): BatchProcessResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<BatchFileResult>()
        
        config.inputFiles.map { file ->
            async {
                processSingleFile(file, config.outputDirectory, config.operations)
            }
        }.awaitAll().forEach { results.add(it) }
        
        BatchProcessResult(
            success = results.all { it.success },
            fileResults = results,
            totalProcessed = results.size,
            successCount = results.count { it.success },
            failureCount = results.count { !it.success }
        )
    }

    private suspend fun processSingleFile(
        inputFile: File,
        outputDirectory: File,
        operations: List<BatchOperation>
    ): BatchFileResult {
        var currentFile = inputFile
        val appliedOperations = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        operations.forEach { operation ->
            try {
                val result = applyOperation(currentFile, outputDirectory, operation)
                if (result.success && result.outputFile != null) {
                    currentFile = result.outputFile
                    appliedOperations.add(operation::class.simpleName ?: "Unknown")
                } else {
                    errors.add("Failed to apply ${operation::class.simpleName}: ${result.error}")
                }
            } catch (e: Exception) {
                errors.add("Error applying ${operation::class.simpleName}: ${e.message}")
            }
        }
        
        return BatchFileResult(
            inputFile = inputFile,
            outputFile = currentFile,
            success = errors.isEmpty(),
            appliedOperations = appliedOperations,
            errors = errors
        )
    }

    private suspend fun applyOperation(
        inputFile: File,
        outputDirectory: File,
        operation: BatchOperation
    ): OperationResult {
        return when (operation) {
            is BatchOperation.ConvertToPdf -> {
                // Would use document converter here
                OperationResult(true, null, inputFile)
            }
            is BatchOperation.AddWatermark -> {
                // Would use PdfAdvancedTools here
                OperationResult(true, null, inputFile)
            }
            is BatchOperation.Rename -> {
                val newName = inputFile.name.replace(
                    operation.pattern.toRegex(),
                    operation.replacement
                )
                val outputFile = File(outputDirectory, newName)
                inputFile.copyTo(outputFile, overwrite = true)
                OperationResult(true, null, outputFile)
            }
            is BatchOperation.Compress -> {
                // Would use file compression here
                OperationResult(true, null, inputFile)
            }
            is BatchOperation.ExtractText -> {
                // Would extract and save text
                OperationResult(true, null, inputFile)
            }
            is BatchOperation.AddPrefix -> {
                val newName = "${operation.prefix}${inputFile.name}"
                val outputFile = File(outputDirectory, newName)
                inputFile.copyTo(outputFile, overwrite = true)
                OperationResult(true, null, outputFile)
            }
            is BatchOperation.AddSuffix -> {
                val nameWithoutExt = inputFile.nameWithoutExtension
                val ext = inputFile.extension
                val newName = "${nameWithoutExt}${operation.suffix}.$ext"
                val outputFile = File(outputDirectory, newName)
                inputFile.copyTo(outputFile, overwrite = true)
                OperationResult(true, null, outputFile)
            }
        }
    }

    private data class OperationResult(
        val success: Boolean,
        val error: String?,
        val outputFile: File?
    )

    data class BatchProcessResult(
        val success: Boolean,
        val fileResults: List<BatchFileResult>,
        val totalProcessed: Int,
        val successCount: Int,
        val failureCount: Int
    )

    data class BatchFileResult(
        val inputFile: File,
        val outputFile: File,
        val success: Boolean,
        val appliedOperations: List<String>,
        val errors: List<String>
    )

    /**
     * Fill a template document with variables
     */
    suspend fun fillTemplate(
        templateFile: File,
        outputFile: File,
        variables: List<TemplateVariable>,
        placeholderFormat: PlaceholderFormat = PlaceholderFormat.CURLY_BRACES
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pattern = getPlaceholderPattern(placeholderFormat)
            val variableMap = variables.associate { it.name to it.value }
            
            mergeDocument(templateFile, outputFile, variableMap, pattern)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Extract placeholders from a template
     */
    suspend fun extractPlaceholders(
        templateFile: File,
        placeholderFormat: PlaceholderFormat = PlaceholderFormat.CURLY_BRACES
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            val pattern = getPlaceholderPattern(placeholderFormat)
            val placeholders = mutableSetOf<String>()
            
            val doc = XWPFDocument(FileInputStream(templateFile))
            
            // Extract from paragraphs
            doc.paragraphs.forEach { paragraph ->
                paragraph.runs.forEach { run ->
                    val text = run.text() ?: return@forEach
                    val matcher = pattern.matcher(text)
                    while (matcher.find()) {
                        placeholders.add(matcher.group(1))
                    }
                }
            }
            
            // Extract from tables
            doc.tables.forEach { table ->
                table.rows.forEach { row ->
                    row.tableCells.forEach { cell ->
                        cell.paragraphs.forEach { paragraph ->
                            paragraph.runs.forEach { run ->
                                val text = run.text() ?: return@forEach
                                val matcher = pattern.matcher(text)
                                while (matcher.find()) {
                                    placeholders.add(matcher.group(1))
                                }
                            }
                        }
                    }
                }
            }
            
            doc.close()
            placeholders.toList().sorted()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Execute a document workflow
     */
    suspend fun executeWorkflow(
        inputFiles: List<File>,
        steps: List<WorkflowStep>,
        outputDirectory: File
    ): WorkflowResult = withContext(Dispatchers.IO) {
        val stepResults = mutableListOf<WorkflowStepResult>()
        var currentFiles = inputFiles.toMutableList()
        var overallSuccess = true
        
        steps.forEach { step ->
            val stepResult = executeWorkflowStep(step, currentFiles, outputDirectory)
            stepResults.add(stepResult)
            
            if (!stepResult.success && !step.continueOnError) {
                overallSuccess = false
                return@forEach
            }
            
            // Update files for next step
            if (stepResult.outputFiles.isNotEmpty()) {
                currentFiles = stepResult.outputFiles.toMutableList()
            }
        }
        
        WorkflowResult(
            success = overallSuccess,
            stepResults = stepResults,
            finalOutputFiles = currentFiles
        )
    }

    private suspend fun executeWorkflowStep(
        step: WorkflowStep,
        inputFiles: List<File>,
        outputDirectory: File
    ): WorkflowStepResult {
        return try {
            when (step.action) {
                is WorkflowAction.ConvertFormat -> {
                    // Would convert files to target format
                    WorkflowStepResult(
                        stepName = step.name,
                        success = true,
                        outputFiles = inputFiles
                    )
                }
                is WorkflowAction.MergeDocuments -> {
                    // Would merge all documents
                    WorkflowStepResult(
                        stepName = step.name,
                        success = true,
                        outputFiles = inputFiles
                    )
                }
                else -> {
                    WorkflowStepResult(
                        stepName = step.name,
                        success = true,
                        outputFiles = inputFiles
                    )
                }
            }
        } catch (e: Exception) {
            WorkflowStepResult(
                stepName = step.name,
                success = false,
                error = e.message,
                outputFiles = inputFiles
            )
        }
    }

    data class WorkflowResult(
        val success: Boolean,
        val stepResults: List<WorkflowStepResult>,
        val finalOutputFiles: List<File>
    )

    data class WorkflowStepResult(
        val stepName: String,
        val success: Boolean,
        val error: String? = null,
        val outputFiles: List<File>
    )

    /**
     * Create a document from form data
     */
    suspend fun createDocumentFromForm(
        formData: Map<String, Any>,
        templateName: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = XWPFDocument()
            
            // Add form data as formatted content
            formData.forEach { (key, value) ->
                val paragraph = doc.createParagraph()
                
                // Add field label
                val labelRun = paragraph.createRun()
                labelRun.isBold = true
                labelRun.setText("$key: ")
                
                // Add field value
                val valueRun = paragraph.createRun()
                valueRun.setText(value.toString())
            }
            
            FileOutputStream(outputFile).use { doc.write(it) }
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Validate a document against rules
     */
    suspend fun validateDocument(
        file: File,
        rules: List<ValidationRule>
    ): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val violations = mutableListOf<ValidationViolation>()
            val doc = XWPFDocument(FileInputStream(file))
            
            val fullText = doc.paragraphs.joinToString("\n") { it.text }
            val wordCount = fullText.split("\\s+".toRegex()).size
            
            rules.forEach { rule ->
                val violation = when (rule) {
                    is ValidationRule.MinWordCount -> {
                        if (wordCount < rule.minWords) {
                            ValidationViolation(
                                rule = rule.name,
                                message = "Document has $wordCount words, minimum is ${rule.minWords}"
                            )
                        } else null
                    }
                    is ValidationRule.MaxWordCount -> {
                        if (wordCount > rule.maxWords) {
                            ValidationViolation(
                                rule = rule.name,
                                message = "Document has $wordCount words, maximum is ${rule.maxWords}"
                            )
                        } else null
                    }
                    is ValidationRule.RequiredField -> {
                        if (!fullText.contains(rule.fieldName, ignoreCase = true)) {
                            ValidationViolation(
                                rule = rule.name,
                                message = "Required field '${rule.fieldName}' not found"
                            )
                        } else null
                    }
                    is ValidationRule.ForbiddenContent -> {
                        if (fullText.contains(rule.content, ignoreCase = true)) {
                            ValidationViolation(
                                rule = rule.name,
                                message = "Forbidden content '${rule.content}' found"
                            )
                        } else null
                    }
                    is ValidationRule.DateFormat -> {
                        // Check for date format compliance
                        null
                    }
                }
                
                violation?.let { violations.add(it) }
            }
            
            doc.close()
            
            ValidationResult(
                isValid = violations.isEmpty(),
                violations = violations,
                wordCount = wordCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ValidationResult(
                isValid = false,
                violations = listOf(ValidationViolation("Error", "Failed to validate: ${e.message}")),
                wordCount = 0
            )
        }
    }

    sealed class ValidationRule(val name: String) {
        data class MinWordCount(val minWords: Int) : ValidationRule("MinWordCount")
        data class MaxWordCount(val maxWords: Int) : ValidationRule("MaxWordCount")
        data class RequiredField(val fieldName: String) : ValidationRule("RequiredField")
        data class ForbiddenContent(val content: String) : ValidationRule("ForbiddenContent")
        data class DateFormat(val format: String) : ValidationRule("DateFormat")
    }

    data class ValidationResult(
        val isValid: Boolean,
        val violations: List<ValidationViolation>,
        val wordCount: Int
    )

    data class ValidationViolation(
        val rule: String,
        val message: String
    )
}
