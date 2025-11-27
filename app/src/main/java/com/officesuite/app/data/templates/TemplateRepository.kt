package com.officesuite.app.data.templates

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing document templates
 */
class TemplateRepository(private val context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "templates_prefs", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CUSTOM_TEMPLATES = "custom_templates"
        private const val KEY_TEMPLATE_USAGE = "template_usage"
        private const val PLACEHOLDER_START = "{{"
        private const val PLACEHOLDER_END = "}}"
    }

    /**
     * Get all available templates (built-in + custom)
     */
    suspend fun getAllTemplates(): List<DocumentTemplate> = withContext(Dispatchers.IO) {
        val builtIn = getBuiltInTemplates()
        val custom = getCustomTemplates()
        builtIn + custom
    }

    /**
     * Get templates by category
     */
    suspend fun getTemplatesByCategory(category: TemplateCategory): List<DocumentTemplate> {
        return getAllTemplates().filter { it.category == category }
    }

    /**
     * Get templates by type
     */
    suspend fun getTemplatesByType(type: TemplateType): List<DocumentTemplate> {
        return getAllTemplates().filter { it.type == type }
    }

    /**
     * Get a specific template by ID
     */
    suspend fun getTemplateById(id: String): DocumentTemplate? {
        return getAllTemplates().find { it.id == id }
    }

    /**
     * Save a custom template
     */
    suspend fun saveCustomTemplate(template: DocumentTemplate): Boolean = withContext(Dispatchers.IO) {
        try {
            val templates = getCustomTemplates().toMutableList()
            val existingIndex = templates.indexOfFirst { it.id == template.id }
            
            if (existingIndex >= 0) {
                templates[existingIndex] = template
            } else {
                templates.add(template.copy(isBuiltIn = false))
            }
            
            val json = gson.toJson(templates)
            prefs.edit().putString(KEY_CUSTOM_TEMPLATES, json).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a custom template
     */
    suspend fun deleteCustomTemplate(templateId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val templates = getCustomTemplates().toMutableList()
            templates.removeIf { it.id == templateId }
            
            val json = gson.toJson(templates)
            prefs.edit().putString(KEY_CUSTOM_TEMPLATES, json).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Record template usage
     */
    suspend fun recordTemplateUsage(templateId: String) = withContext(Dispatchers.IO) {
        val usageJson = prefs.getString(KEY_TEMPLATE_USAGE, "{}") ?: "{}"
        val usageType = object : TypeToken<MutableMap<String, Int>>() {}.type
        val usage: MutableMap<String, Int> = gson.fromJson(usageJson, usageType) ?: mutableMapOf()
        
        usage[templateId] = (usage[templateId] ?: 0) + 1
        
        prefs.edit().putString(KEY_TEMPLATE_USAGE, gson.toJson(usage)).apply()
    }

    /**
     * Get recently used templates
     */
    suspend fun getRecentlyUsedTemplates(limit: Int = 5): List<DocumentTemplate> = withContext(Dispatchers.IO) {
        val usageJson = prefs.getString(KEY_TEMPLATE_USAGE, "{}") ?: "{}"
        val usageType = object : TypeToken<Map<String, Int>>() {}.type
        val usage: Map<String, Int> = gson.fromJson(usageJson, usageType) ?: emptyMap()
        
        val allTemplates = getAllTemplates()
        allTemplates
            .filter { usage.containsKey(it.id) }
            .sortedByDescending { usage[it.id] ?: 0 }
            .take(limit)
    }

    /**
     * Apply template placeholders with user values
     */
    fun applyPlaceholders(template: DocumentTemplate, values: Map<String, String>): String {
        var content = template.content
        for (placeholder in template.placeholders) {
            val value = values[placeholder.key] ?: placeholder.defaultValue
            content = content.replace("$PLACEHOLDER_START${placeholder.key}$PLACEHOLDER_END", value)
        }
        return content
    }

    private fun getCustomTemplates(): List<DocumentTemplate> {
        val json = prefs.getString(KEY_CUSTOM_TEMPLATES, "[]") ?: "[]"
        val type = object : TypeToken<List<DocumentTemplate>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun placeholder(name: String): String = "$PLACEHOLDER_START$name$PLACEHOLDER_END"

    /**
     * Built-in templates
     */
    private fun getBuiltInTemplates(): List<DocumentTemplate> {
        return listOf(
            createResumeTemplate(),
            createModernResumeTemplate(),
            createFormalLetterTemplate(),
            createCoverLetterTemplate(),
            createInvoiceTemplate(),
            createMeetingNotesTemplate(),
            createProjectReportTemplate(),
            createBusinessPresentationTemplate()
        )
    }

    // Resume Templates
    private fun createResumeTemplate(): DocumentTemplate {
        val content = buildString {
            appendLine("# ${placeholder("full_name")}")
            appendLine("${placeholder("email")} | ${placeholder("phone")} | ${placeholder("location")}")
            appendLine()
            appendLine("## Professional Summary")
            appendLine(placeholder("summary"))
            appendLine()
            appendLine("## Experience")
            appendLine()
            appendLine("### ${placeholder("job_title_1")} | ${placeholder("company_1")}")
            appendLine("*${placeholder("dates_1")}*")
            appendLine()
            appendLine(placeholder("responsibilities_1"))
            appendLine()
            appendLine("### ${placeholder("job_title_2")} | ${placeholder("company_2")}")
            appendLine("*${placeholder("dates_2")}*")
            appendLine()
            appendLine(placeholder("responsibilities_2"))
            appendLine()
            appendLine("## Education")
            appendLine()
            appendLine("### ${placeholder("degree")} | ${placeholder("school")}")
            appendLine("*${placeholder("graduation_date")}*")
            appendLine()
            appendLine("## Skills")
            appendLine(placeholder("skills"))
        }
        
        return DocumentTemplate(
            id = "builtin_resume_classic",
            name = "Classic Resume",
            description = "A clean, professional resume template",
            category = TemplateCategory.RESUME,
            type = TemplateType.MARKDOWN,
            isBuiltIn = true,
            content = content,
            placeholders = listOf(
                TemplatePlaceholder("full_name", "Full Name", required = true),
                TemplatePlaceholder("email", "Email", type = PlaceholderType.EMAIL, required = true),
                TemplatePlaceholder("phone", "Phone", type = PlaceholderType.PHONE),
                TemplatePlaceholder("location", "Location", hint = "City, State"),
                TemplatePlaceholder("summary", "Professional Summary", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("job_title_1", "Job Title"),
                TemplatePlaceholder("company_1", "Company Name"),
                TemplatePlaceholder("dates_1", "Employment Dates", hint = "Jan 2020 - Present"),
                TemplatePlaceholder("responsibilities_1", "Key Responsibilities", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("job_title_2", "Previous Job Title"),
                TemplatePlaceholder("company_2", "Previous Company"),
                TemplatePlaceholder("dates_2", "Employment Dates"),
                TemplatePlaceholder("responsibilities_2", "Key Responsibilities", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("degree", "Degree"),
                TemplatePlaceholder("school", "School/University"),
                TemplatePlaceholder("graduation_date", "Graduation Date"),
                TemplatePlaceholder("skills", "Skills", hint = "Comma-separated list")
            )
        )
    }

    private fun createModernResumeTemplate(): DocumentTemplate {
        val content = buildString {
            appendLine("# ${placeholder("full_name")}")
            appendLine("**${placeholder("job_title")}**")
            appendLine()
            appendLine("📧 ${placeholder("email")} | 📱 ${placeholder("phone")} | 🔗 ${placeholder("linkedin")}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## About Me")
            appendLine(placeholder("about"))
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Experience")
            appendLine()
            appendLine("### ${placeholder("current_role")} @ ${placeholder("current_company")}")
            appendLine("📅 ${placeholder("current_dates")}")
            appendLine()
            appendLine(placeholder("current_achievements"))
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Skills")
            appendLine(placeholder("skills"))
        }
        
        return DocumentTemplate(
            id = "builtin_resume_modern",
            name = "Modern Resume",
            description = "A contemporary resume with modern styling",
            category = TemplateCategory.RESUME,
            type = TemplateType.MARKDOWN,
            isBuiltIn = true,
            content = content,
            placeholders = listOf(
                TemplatePlaceholder("full_name", "Full Name", required = true),
                TemplatePlaceholder("job_title", "Professional Title"),
                TemplatePlaceholder("email", "Email", type = PlaceholderType.EMAIL),
                TemplatePlaceholder("phone", "Phone", type = PlaceholderType.PHONE),
                TemplatePlaceholder("linkedin", "LinkedIn URL"),
                TemplatePlaceholder("about", "About Me", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("current_role", "Current Role"),
                TemplatePlaceholder("current_company", "Current Company"),
                TemplatePlaceholder("current_dates", "Dates"),
                TemplatePlaceholder("current_achievements", "Key Achievements", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("skills", "Skills")
            )
        )
    }

    private fun createFormalLetterTemplate(): DocumentTemplate {
        val content = buildString {
            appendLine(placeholder("your_name"))
            appendLine(placeholder("your_address"))
            appendLine(placeholder("your_email"))
            appendLine()
            appendLine(placeholder("date"))
            appendLine()
            appendLine(placeholder("recipient_name"))
            appendLine(placeholder("recipient_title"))
            appendLine(placeholder("company_name"))
            appendLine(placeholder("company_address"))
            appendLine()
            appendLine("Dear ${placeholder("salutation")},")
            appendLine()
            appendLine(placeholder("opening_paragraph"))
            appendLine()
            appendLine(placeholder("body_paragraph"))
            appendLine()
            appendLine(placeholder("closing_paragraph"))
            appendLine()
            appendLine("Sincerely,")
            appendLine()
            appendLine(placeholder("your_name"))
        }
        
        return DocumentTemplate(
            id = "builtin_letter_formal",
            name = "Formal Letter",
            description = "Professional business letter format",
            category = TemplateCategory.LETTER,
            type = TemplateType.MARKDOWN,
            isBuiltIn = true,
            content = content,
            placeholders = listOf(
                TemplatePlaceholder("your_name", "Your Name", required = true),
                TemplatePlaceholder("your_address", "Your Address"),
                TemplatePlaceholder("your_email", "Your Email", type = PlaceholderType.EMAIL),
                TemplatePlaceholder("date", "Date", type = PlaceholderType.DATE),
                TemplatePlaceholder("recipient_name", "Recipient Name"),
                TemplatePlaceholder("recipient_title", "Recipient Title"),
                TemplatePlaceholder("company_name", "Company Name"),
                TemplatePlaceholder("company_address", "Company Address"),
                TemplatePlaceholder("salutation", "Salutation", defaultValue = "Mr./Ms. Last Name"),
                TemplatePlaceholder("opening_paragraph", "Opening Paragraph", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("body_paragraph", "Body", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("closing_paragraph", "Closing Paragraph", type = PlaceholderType.MULTILINE_TEXT)
            )
        )
    }

    private fun createCoverLetterTemplate(): DocumentTemplate {
        val content = buildString {
            appendLine("# Cover Letter")
            appendLine()
            appendLine("**${placeholder("your_name")}**")
            appendLine("${placeholder("your_email")} | ${placeholder("your_phone")}")
            appendLine()
            appendLine(placeholder("date"))
            appendLine()
            appendLine("**RE: ${placeholder("job_title")} Position**")
            appendLine()
            appendLine("Dear ${placeholder("hiring_manager")},")
            appendLine()
            appendLine(placeholder("intro_paragraph"))
            appendLine()
            appendLine(placeholder("experience_paragraph"))
            appendLine()
            appendLine(placeholder("closing_paragraph"))
            appendLine()
            appendLine("Thank you for considering my application.")
            appendLine()
            appendLine("Sincerely,")
            appendLine(placeholder("your_name"))
        }
        
        return DocumentTemplate(
            id = "builtin_cover_letter",
            name = "Cover Letter",
            description = "Job application cover letter",
            category = TemplateCategory.LETTER,
            type = TemplateType.MARKDOWN,
            isBuiltIn = true,
            content = content,
            placeholders = listOf(
                TemplatePlaceholder("your_name", "Your Name", required = true),
                TemplatePlaceholder("your_email", "Email", type = PlaceholderType.EMAIL),
                TemplatePlaceholder("your_phone", "Phone", type = PlaceholderType.PHONE),
                TemplatePlaceholder("date", "Date", type = PlaceholderType.DATE),
                TemplatePlaceholder("job_title", "Job Title"),
                TemplatePlaceholder("hiring_manager", "Hiring Manager Name", defaultValue = "Hiring Manager"),
                TemplatePlaceholder("intro_paragraph", "Introduction", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("experience_paragraph", "Relevant Experience", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("closing_paragraph", "Closing", type = PlaceholderType.MULTILINE_TEXT)
            )
        )
    }

    private fun createInvoiceTemplate(): DocumentTemplate {
        val dollar = "$"
        val content = buildString {
            appendLine("# INVOICE")
            appendLine()
            appendLine("**Invoice #:** ${placeholder("invoice_number")}")
            appendLine("**Date:** ${placeholder("invoice_date")}")
            appendLine("**Due Date:** ${placeholder("due_date")}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## From:")
            appendLine("**${placeholder("your_company")}**")
            appendLine(placeholder("your_address"))
            appendLine(placeholder("your_email"))
            appendLine()
            appendLine("## Bill To:")
            appendLine("**${placeholder("client_name")}**")
            appendLine(placeholder("client_company"))
            appendLine(placeholder("client_address"))
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Items")
            appendLine()
            appendLine("| Description | Quantity | Unit Price | Total |")
            appendLine("|-------------|----------|------------|-------|")
            appendLine("| ${placeholder("item_1")} | ${placeholder("qty_1")} | ${dollar}${placeholder("price_1")} | ${dollar}${placeholder("total_1")} |")
            appendLine("| ${placeholder("item_2")} | ${placeholder("qty_2")} | ${dollar}${placeholder("price_2")} | ${dollar}${placeholder("total_2")} |")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("| **Subtotal** | ${dollar}${placeholder("subtotal")} |")
            appendLine("| Tax | ${dollar}${placeholder("tax_amount")} |")
            appendLine("| **Total** | **${dollar}${placeholder("grand_total")}** |")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("**Payment Terms:** ${placeholder("payment_terms")}")
            appendLine()
            appendLine("Thank you for your business!")
        }
        
        return DocumentTemplate(
            id = "builtin_invoice",
            name = "Invoice",
            description = "Professional invoice template",
            category = TemplateCategory.INVOICE,
            type = TemplateType.MARKDOWN,
            isBuiltIn = true,
            content = content,
            placeholders = listOf(
                TemplatePlaceholder("invoice_number", "Invoice Number", required = true),
                TemplatePlaceholder("invoice_date", "Invoice Date", type = PlaceholderType.DATE),
                TemplatePlaceholder("due_date", "Due Date", type = PlaceholderType.DATE),
                TemplatePlaceholder("your_company", "Your Company Name"),
                TemplatePlaceholder("your_address", "Your Address"),
                TemplatePlaceholder("your_email", "Your Email", type = PlaceholderType.EMAIL),
                TemplatePlaceholder("client_name", "Client Name"),
                TemplatePlaceholder("client_company", "Client Company"),
                TemplatePlaceholder("client_address", "Client Address"),
                TemplatePlaceholder("item_1", "Item 1 Description"),
                TemplatePlaceholder("qty_1", "Quantity", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("price_1", "Unit Price", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("total_1", "Line Total", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("item_2", "Item 2 Description"),
                TemplatePlaceholder("qty_2", "Quantity", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("price_2", "Unit Price", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("total_2", "Line Total", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("subtotal", "Subtotal", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("tax_amount", "Tax Amount", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("grand_total", "Grand Total", type = PlaceholderType.NUMBER),
                TemplatePlaceholder("payment_terms", "Payment Terms", defaultValue = "Net 30")
            )
        )
    }

    private fun createMeetingNotesTemplate(): DocumentTemplate {
        val content = buildString {
            appendLine("# Meeting Notes")
            appendLine()
            appendLine("**Date:** ${placeholder("meeting_date")}")
            appendLine("**Time:** ${placeholder("meeting_time")}")
            appendLine("**Location:** ${placeholder("location")}")
            appendLine()
            appendLine("## Attendees")
            appendLine(placeholder("attendees"))
            appendLine()
            appendLine("## Agenda")
            appendLine(placeholder("agenda"))
            appendLine()
            appendLine("## Discussion Points")
            appendLine(placeholder("discussion"))
            appendLine()
            appendLine("## Decisions Made")
            appendLine(placeholder("decisions"))
            appendLine()
            appendLine("## Action Items")
            appendLine("| Task | Owner | Due Date |")
            appendLine("|------|-------|----------|")
            appendLine("| ${placeholder("action_1")} | ${placeholder("owner_1")} | ${placeholder("due_1")} |")
            appendLine("| ${placeholder("action_2")} | ${placeholder("owner_2")} | ${placeholder("due_2")} |")
            appendLine()
            appendLine("## Next Meeting")
            appendLine("**Date:** ${placeholder("next_meeting_date")}")
            appendLine()
            appendLine("---")
            appendLine("*Notes taken by: ${placeholder("note_taker")}*")
        }
        
        return DocumentTemplate(
            id = "builtin_meeting_notes",
            name = "Meeting Notes",
            description = "Template for documenting meeting discussions",
            category = TemplateCategory.REPORT,
            type = TemplateType.MARKDOWN,
            isBuiltIn = true,
            content = content,
            placeholders = listOf(
                TemplatePlaceholder("meeting_date", "Meeting Date", type = PlaceholderType.DATE),
                TemplatePlaceholder("meeting_time", "Meeting Time"),
                TemplatePlaceholder("location", "Location/Platform"),
                TemplatePlaceholder("attendees", "Attendees", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("agenda", "Agenda Items", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("discussion", "Discussion Points", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("decisions", "Decisions Made", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("action_1", "Action Item 1"),
                TemplatePlaceholder("owner_1", "Owner"),
                TemplatePlaceholder("due_1", "Due Date"),
                TemplatePlaceholder("action_2", "Action Item 2"),
                TemplatePlaceholder("owner_2", "Owner"),
                TemplatePlaceholder("due_2", "Due Date"),
                TemplatePlaceholder("next_meeting_date", "Next Meeting Date"),
                TemplatePlaceholder("note_taker", "Note Taker")
            )
        )
    }

    private fun createProjectReportTemplate(): DocumentTemplate {
        val content = buildString {
            appendLine("# Project Status Report")
            appendLine()
            appendLine("**Project:** ${placeholder("project_name")}")
            appendLine("**Report Date:** ${placeholder("report_date")}")
            appendLine("**Project Manager:** ${placeholder("project_manager")}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Executive Summary")
            appendLine(placeholder("executive_summary"))
            appendLine()
            appendLine("## Overall Status: ${placeholder("status")}")
            appendLine()
            appendLine("## Key Accomplishments")
            appendLine(placeholder("accomplishments"))
            appendLine()
            appendLine("## Upcoming Milestones")
            appendLine("| Milestone | Due Date | Status |")
            appendLine("|-----------|----------|--------|")
            appendLine("| ${placeholder("milestone_1")} | ${placeholder("due_1")} | ${placeholder("status_1")} |")
            appendLine("| ${placeholder("milestone_2")} | ${placeholder("due_2")} | ${placeholder("status_2")} |")
            appendLine()
            appendLine("## Risks and Issues")
            appendLine(placeholder("risks"))
            appendLine()
            appendLine("## Next Steps")
            appendLine(placeholder("next_steps"))
            appendLine()
            appendLine("---")
            appendLine("*Report prepared by: ${placeholder("prepared_by")}*")
        }
        
        return DocumentTemplate(
            id = "builtin_project_report",
            name = "Project Status Report",
            description = "Weekly/monthly project status update",
            category = TemplateCategory.REPORT,
            type = TemplateType.MARKDOWN,
            isBuiltIn = true,
            content = content,
            placeholders = listOf(
                TemplatePlaceholder("project_name", "Project Name", required = true),
                TemplatePlaceholder("report_date", "Report Date", type = PlaceholderType.DATE),
                TemplatePlaceholder("project_manager", "Project Manager"),
                TemplatePlaceholder("executive_summary", "Executive Summary", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("status", "Status", defaultValue = "On Track"),
                TemplatePlaceholder("accomplishments", "Accomplishments", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("milestone_1", "Milestone 1"),
                TemplatePlaceholder("due_1", "Due Date"),
                TemplatePlaceholder("status_1", "Status"),
                TemplatePlaceholder("milestone_2", "Milestone 2"),
                TemplatePlaceholder("due_2", "Due Date"),
                TemplatePlaceholder("status_2", "Status"),
                TemplatePlaceholder("risks", "Risks and Issues", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("next_steps", "Next Steps", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("prepared_by", "Prepared By")
            )
        )
    }

    private fun createBusinessPresentationTemplate(): DocumentTemplate {
        val content = buildString {
            appendLine("# ${placeholder("presentation_title")}")
            appendLine()
            appendLine("## ${placeholder("company_name")}")
            appendLine("**${placeholder("presenter_name")}** | ${placeholder("date")}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("# Agenda")
            appendLine()
            appendLine("1. ${placeholder("agenda_1")}")
            appendLine("2. ${placeholder("agenda_2")}")
            appendLine("3. ${placeholder("agenda_3")}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("# Executive Summary")
            appendLine()
            appendLine(placeholder("executive_summary"))
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("# Our Solution")
            appendLine()
            appendLine(placeholder("solution"))
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("# Key Benefits")
            appendLine()
            appendLine("- ${placeholder("benefit_1")}")
            appendLine("- ${placeholder("benefit_2")}")
            appendLine("- ${placeholder("benefit_3")}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("# Next Steps")
            appendLine()
            appendLine(placeholder("next_steps"))
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("# Thank You")
            appendLine()
            appendLine("**Questions?**")
            appendLine()
            appendLine(placeholder("contact_info"))
        }
        
        return DocumentTemplate(
            id = "builtin_presentation_business",
            name = "Business Presentation",
            description = "Professional business presentation outline",
            category = TemplateCategory.PRESENTATION,
            type = TemplateType.MARKDOWN,
            isBuiltIn = true,
            content = content,
            placeholders = listOf(
                TemplatePlaceholder("presentation_title", "Presentation Title", required = true),
                TemplatePlaceholder("company_name", "Company Name"),
                TemplatePlaceholder("presenter_name", "Presenter Name"),
                TemplatePlaceholder("date", "Date", type = PlaceholderType.DATE),
                TemplatePlaceholder("agenda_1", "Agenda Item 1"),
                TemplatePlaceholder("agenda_2", "Agenda Item 2"),
                TemplatePlaceholder("agenda_3", "Agenda Item 3"),
                TemplatePlaceholder("executive_summary", "Executive Summary", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("solution", "Proposed Solution", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("benefit_1", "Key Benefit 1"),
                TemplatePlaceholder("benefit_2", "Key Benefit 2"),
                TemplatePlaceholder("benefit_3", "Key Benefit 3"),
                TemplatePlaceholder("next_steps", "Next Steps", type = PlaceholderType.MULTILINE_TEXT),
                TemplatePlaceholder("contact_info", "Contact Information")
            )
        )
    }
}
