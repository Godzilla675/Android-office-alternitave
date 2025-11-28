package com.officesuite.app.automation

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for DocumentAutomation class data classes and enums.
 */
class DocumentAutomationTest {

    @Test
    fun `PlaceholderFormat enum has all expected values`() {
        val formats = DocumentAutomation.PlaceholderFormat.values()
        
        assertEquals(4, formats.size)
        assertTrue(formats.contains(DocumentAutomation.PlaceholderFormat.CURLY_BRACES))
        assertTrue(formats.contains(DocumentAutomation.PlaceholderFormat.ANGLE_BRACKETS))
        assertTrue(formats.contains(DocumentAutomation.PlaceholderFormat.SQUARE_BRACKETS))
        assertTrue(formats.contains(DocumentAutomation.PlaceholderFormat.DOLLAR_SIGN))
    }

    @Test
    fun `VariableType enum has all expected values`() {
        val types = DocumentAutomation.VariableType.values()
        
        assertEquals(5, types.size)
        assertTrue(types.contains(DocumentAutomation.VariableType.TEXT))
        assertTrue(types.contains(DocumentAutomation.VariableType.NUMBER))
        assertTrue(types.contains(DocumentAutomation.VariableType.DATE))
        assertTrue(types.contains(DocumentAutomation.VariableType.IMAGE))
        assertTrue(types.contains(DocumentAutomation.VariableType.TABLE))
    }

    @Test
    fun `MailMergeConfig data class holds values correctly`() {
        val templateFile = File("/tmp/template.docx")
        val dataFile = File("/tmp/data.xlsx")
        val outputDir = File("/tmp/output")
        
        val config = DocumentAutomation.MailMergeConfig(
            templateFile = templateFile,
            dataFile = dataFile,
            outputDirectory = outputDir,
            outputPrefix = "merged_",
            placeholderFormat = DocumentAutomation.PlaceholderFormat.CURLY_BRACES
        )
        
        assertEquals(templateFile, config.templateFile)
        assertEquals(dataFile, config.dataFile)
        assertEquals(outputDir, config.outputDirectory)
        assertEquals("merged_", config.outputPrefix)
        assertEquals(DocumentAutomation.PlaceholderFormat.CURLY_BRACES, config.placeholderFormat)
    }

    @Test
    fun `MailMergeConfig default values are correct`() {
        val config = DocumentAutomation.MailMergeConfig(
            templateFile = File("/tmp/template.docx"),
            dataFile = File("/tmp/data.xlsx"),
            outputDirectory = File("/tmp/output")
        )
        
        assertEquals("merged_", config.outputPrefix)
        assertEquals(DocumentAutomation.PlaceholderFormat.CURLY_BRACES, config.placeholderFormat)
    }

    @Test
    fun `BatchOperation ConvertToPdf has default quality of 100`() {
        val operation = DocumentAutomation.BatchOperation.ConvertToPdf()
        
        assertEquals(100, operation.quality)
    }

    @Test
    fun `BatchOperation ConvertToPdf with custom quality`() {
        val operation = DocumentAutomation.BatchOperation.ConvertToPdf(quality = 75)
        
        assertEquals(75, operation.quality)
    }

    @Test
    fun `BatchOperation AddWatermark has default opacity of 0_3f`() {
        val operation = DocumentAutomation.BatchOperation.AddWatermark(text = "DRAFT")
        
        assertEquals("DRAFT", operation.text)
        assertEquals(0.3f, operation.opacity, 0.001f)
    }

    @Test
    fun `BatchOperation Rename holds pattern and replacement`() {
        val operation = DocumentAutomation.BatchOperation.Rename(
            pattern = "old_",
            replacement = "new_"
        )
        
        assertEquals("old_", operation.pattern)
        assertEquals("new_", operation.replacement)
    }

    @Test
    fun `BatchOperation Compress has default compression level of 7`() {
        val operation = DocumentAutomation.BatchOperation.Compress()
        
        assertEquals(7, operation.compressionLevel)
    }

    @Test
    fun `BatchOperation AddPrefix holds prefix value`() {
        val operation = DocumentAutomation.BatchOperation.AddPrefix(prefix = "final_")
        
        assertEquals("final_", operation.prefix)
    }

    @Test
    fun `BatchOperation AddSuffix holds suffix value`() {
        val operation = DocumentAutomation.BatchOperation.AddSuffix(suffix = "_v2")
        
        assertEquals("_v2", operation.suffix)
    }

    @Test
    fun `BatchOperation ExtractText is singleton object`() {
        val operation1 = DocumentAutomation.BatchOperation.ExtractText
        val operation2 = DocumentAutomation.BatchOperation.ExtractText
        
        assertSame(operation1, operation2)
    }

    @Test
    fun `TemplateVariable data class holds values correctly`() {
        val variable = DocumentAutomation.TemplateVariable(
            name = "userName",
            value = "John Doe",
            type = DocumentAutomation.VariableType.TEXT
        )
        
        assertEquals("userName", variable.name)
        assertEquals("John Doe", variable.value)
        assertEquals(DocumentAutomation.VariableType.TEXT, variable.type)
    }

    @Test
    fun `TemplateVariable default type is TEXT`() {
        val variable = DocumentAutomation.TemplateVariable(
            name = "field",
            value = "value"
        )
        
        assertEquals(DocumentAutomation.VariableType.TEXT, variable.type)
    }

    @Test
    fun `WorkflowStep data class holds values correctly`() {
        val action = DocumentAutomation.WorkflowAction.ConvertFormat(targetFormat = "pdf")
        val step = DocumentAutomation.WorkflowStep(
            name = "Convert to PDF",
            action = action,
            continueOnError = true
        )
        
        assertEquals("Convert to PDF", step.name)
        assertTrue(step.action is DocumentAutomation.WorkflowAction.ConvertFormat)
        assertTrue(step.continueOnError)
    }

    @Test
    fun `WorkflowStep default continueOnError is false`() {
        val step = DocumentAutomation.WorkflowStep(
            name = "Test Step",
            action = DocumentAutomation.WorkflowAction.ConvertFormat("pdf")
        )
        
        assertFalse(step.continueOnError)
    }

    @Test
    fun `WorkflowAction ConvertFormat holds target format`() {
        val action = DocumentAutomation.WorkflowAction.ConvertFormat(targetFormat = "docx")
        
        assertEquals("docx", action.targetFormat)
    }

    @Test
    fun `WorkflowAction ApplyTemplate holds template file`() {
        val templateFile = File("/tmp/template.docx")
        val action = DocumentAutomation.WorkflowAction.ApplyTemplate(templateFile = templateFile)
        
        assertEquals(templateFile, action.templateFile)
    }

    @Test
    fun `WorkflowAction MergeDocuments holds output name`() {
        val action = DocumentAutomation.WorkflowAction.MergeDocuments(outputName = "merged.pdf")
        
        assertEquals("merged.pdf", action.outputName)
    }

    @Test
    fun `WorkflowAction SendEmail holds recipient and subject`() {
        val action = DocumentAutomation.WorkflowAction.SendEmail(
            recipient = "test@example.com",
            subject = "Test Subject"
        )
        
        assertEquals("test@example.com", action.recipient)
        assertEquals("Test Subject", action.subject)
    }

    @Test
    fun `WorkflowAction SaveToCloud holds provider and path`() {
        val action = DocumentAutomation.WorkflowAction.SaveToCloud(
            provider = "GoogleDrive",
            path = "/documents/output"
        )
        
        assertEquals("GoogleDrive", action.provider)
        assertEquals("/documents/output", action.path)
    }

    @Test
    fun `WorkflowAction ExtractData holds field list`() {
        val action = DocumentAutomation.WorkflowAction.ExtractData(
            fields = listOf("name", "email", "phone")
        )
        
        assertEquals(3, action.fields.size)
        assertTrue(action.fields.contains("name"))
    }

    @Test
    fun `WorkflowAction ValidateDocument holds rules list`() {
        val action = DocumentAutomation.WorkflowAction.ValidateDocument(
            rules = listOf("minWords:100", "maxWords:1000")
        )
        
        assertEquals(2, action.rules.size)
    }

    @Test
    fun `MailMergeResult success state`() {
        val result = DocumentAutomation.MailMergeResult(
            success = true,
            generatedFiles = listOf(File("/tmp/merged_1.docx")),
            errors = emptyList(),
            totalRecords = 5,
            successfulMerges = 5
        )
        
        assertTrue(result.success)
        assertEquals(1, result.generatedFiles.size)
        assertTrue(result.errors.isEmpty())
        assertEquals(5, result.totalRecords)
    }

    @Test
    fun `MailMergeResult failure state`() {
        val result = DocumentAutomation.MailMergeResult(
            success = false,
            generatedFiles = emptyList(),
            errors = listOf("Template not found"),
            totalRecords = 0,
            successfulMerges = 0
        )
        
        assertFalse(result.success)
        assertTrue(result.generatedFiles.isEmpty())
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `BatchProcessConfig holds values correctly`() {
        val inputFiles = listOf(File("/tmp/file1.pdf"), File("/tmp/file2.pdf"))
        val outputDir = File("/tmp/output")
        val operations = listOf(
            DocumentAutomation.BatchOperation.ConvertToPdf(),
            DocumentAutomation.BatchOperation.AddWatermark(text = "DRAFT")
        )
        
        val config = DocumentAutomation.BatchProcessConfig(
            inputFiles = inputFiles,
            outputDirectory = outputDir,
            operations = operations
        )
        
        assertEquals(2, config.inputFiles.size)
        assertEquals(outputDir, config.outputDirectory)
        assertEquals(2, config.operations.size)
    }

    @Test
    fun `BatchProcessResult holds values correctly`() {
        val result = DocumentAutomation.BatchProcessResult(
            success = true,
            fileResults = emptyList(),
            totalProcessed = 5,
            successCount = 4,
            failureCount = 1
        )
        
        assertTrue(result.success)
        assertEquals(5, result.totalProcessed)
        assertEquals(4, result.successCount)
        assertEquals(1, result.failureCount)
    }

    @Test
    fun `BatchFileResult holds values correctly`() {
        val inputFile = File("/tmp/input.pdf")
        val outputFile = File("/tmp/output.pdf")
        
        val result = DocumentAutomation.BatchFileResult(
            inputFile = inputFile,
            outputFile = outputFile,
            success = true,
            appliedOperations = listOf("ConvertToPdf", "AddWatermark"),
            errors = emptyList()
        )
        
        assertEquals(inputFile, result.inputFile)
        assertEquals(outputFile, result.outputFile)
        assertTrue(result.success)
        assertEquals(2, result.appliedOperations.size)
    }

    @Test
    fun `WorkflowResult holds values correctly`() {
        val result = DocumentAutomation.WorkflowResult(
            success = true,
            stepResults = emptyList(),
            finalOutputFiles = listOf(File("/tmp/final.pdf"))
        )
        
        assertTrue(result.success)
        assertEquals(1, result.finalOutputFiles.size)
    }

    @Test
    fun `WorkflowStepResult holds values correctly`() {
        val result = DocumentAutomation.WorkflowStepResult(
            stepName = "Convert Step",
            success = true,
            error = null,
            outputFiles = listOf(File("/tmp/output.pdf"))
        )
        
        assertEquals("Convert Step", result.stepName)
        assertTrue(result.success)
        assertNull(result.error)
    }

    @Test
    fun `WorkflowStepResult with error`() {
        val result = DocumentAutomation.WorkflowStepResult(
            stepName = "Failed Step",
            success = false,
            error = "File not found",
            outputFiles = emptyList()
        )
        
        assertFalse(result.success)
        assertEquals("File not found", result.error)
    }

    @Test
    fun `ValidationRule MinWordCount holds minWords`() {
        val rule = DocumentAutomation.ValidationRule.MinWordCount(minWords = 100)
        
        assertEquals(100, rule.minWords)
        assertEquals("MinWordCount", rule.name)
    }

    @Test
    fun `ValidationRule MaxWordCount holds maxWords`() {
        val rule = DocumentAutomation.ValidationRule.MaxWordCount(maxWords = 1000)
        
        assertEquals(1000, rule.maxWords)
        assertEquals("MaxWordCount", rule.name)
    }

    @Test
    fun `ValidationRule RequiredField holds fieldName`() {
        val rule = DocumentAutomation.ValidationRule.RequiredField(fieldName = "Date")
        
        assertEquals("Date", rule.fieldName)
        assertEquals("RequiredField", rule.name)
    }

    @Test
    fun `ValidationRule ForbiddenContent holds content`() {
        val rule = DocumentAutomation.ValidationRule.ForbiddenContent(content = "CONFIDENTIAL")
        
        assertEquals("CONFIDENTIAL", rule.content)
        assertEquals("ForbiddenContent", rule.name)
    }

    @Test
    fun `ValidationRule DateFormat holds format`() {
        val rule = DocumentAutomation.ValidationRule.DateFormat(format = "MM/dd/yyyy")
        
        assertEquals("MM/dd/yyyy", rule.format)
        assertEquals("DateFormat", rule.name)
    }

    @Test
    fun `ValidationResult isValid when no violations`() {
        val result = DocumentAutomation.ValidationResult(
            isValid = true,
            violations = emptyList(),
            wordCount = 500
        )
        
        assertTrue(result.isValid)
        assertTrue(result.violations.isEmpty())
        assertEquals(500, result.wordCount)
    }

    @Test
    fun `ValidationResult invalid when violations exist`() {
        val result = DocumentAutomation.ValidationResult(
            isValid = false,
            violations = listOf(
                DocumentAutomation.ValidationViolation(
                    rule = "MinWordCount",
                    message = "Document has 50 words, minimum is 100"
                )
            ),
            wordCount = 50
        )
        
        assertFalse(result.isValid)
        assertEquals(1, result.violations.size)
    }

    @Test
    fun `ValidationViolation holds rule and message`() {
        val violation = DocumentAutomation.ValidationViolation(
            rule = "RequiredField",
            message = "Required field 'Signature' not found"
        )
        
        assertEquals("RequiredField", violation.rule)
        assertEquals("Required field 'Signature' not found", violation.message)
    }
}
