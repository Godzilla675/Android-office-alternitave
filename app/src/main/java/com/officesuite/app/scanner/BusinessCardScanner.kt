package com.officesuite.app.scanner

import android.graphics.Bitmap
import com.officesuite.app.ocr.OcrManager
import com.officesuite.app.ocr.OcrLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Business Card Scanner that extracts contact information from scanned images
 */
class BusinessCardScanner(private val ocrManager: OcrManager) {

    /**
     * Scanned contact information from a business card
     */
    data class BusinessCardInfo(
        val name: String?,
        val title: String?,
        val company: String?,
        val emails: List<String>,
        val phones: List<String>,
        val addresses: List<String>,
        val websites: List<String>,
        val rawText: String,
        val confidence: Float
    )

    // Patterns for extracting information
    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    )
    
    private val phonePattern = Pattern.compile(
        "(?:\\+?\\d{1,3}[-.\\s]?)?(?:\\(?\\d{2,4}\\)?[-.\\s]?)?\\d{3,4}[-.\\s]?\\d{3,4}"
    )
    
    private val urlPattern = Pattern.compile(
        "(?:https?://)?(?:www\\.)?[a-zA-Z0-9][a-zA-Z0-9-]*\\.[a-zA-Z]{2,}(?:/\\S*)?"
    )

    // Common job title keywords
    private val titleKeywords = listOf(
        "CEO", "CTO", "CFO", "COO", "CMO", "CIO",
        "Director", "Manager", "President", "Vice President", "VP",
        "Engineer", "Developer", "Designer", "Architect", "Analyst",
        "Consultant", "Specialist", "Executive", "Officer", "Head",
        "Lead", "Senior", "Junior", "Principal", "Chief", "Founder",
        "Partner", "Associate", "Assistant", "Coordinator", "Administrator"
    )

    /**
     * Scans a business card image and extracts contact information
     * 
     * @param bitmap The business card image
     * @return BusinessCardInfo containing extracted contact details
     */
    suspend fun scan(bitmap: Bitmap): BusinessCardInfo = withContext(Dispatchers.Default) {
        // Ensure OCR is set to Latin for business cards
        ocrManager.setLanguage(OcrLanguage.LATIN)
        
        val ocrResult = ocrManager.extractText(bitmap)
        
        if (!ocrResult.success) {
            return@withContext BusinessCardInfo(
                name = null,
                title = null,
                company = null,
                emails = emptyList(),
                phones = emptyList(),
                addresses = emptyList(),
                websites = emptyList(),
                rawText = "",
                confidence = 0f
            )
        }

        val text = ocrResult.fullText
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        
        // Extract structured information
        val emails = extractEmails(text)
        val phones = extractPhones(text)
        val websites = extractWebsites(text)
        
        // Remove lines that are emails, phones, or websites for name/title detection
        val contentLines = lines.filter { line ->
            !emails.any { line.contains(it, ignoreCase = true) } &&
            !phones.any { line.contains(it) } &&
            !websites.any { line.contains(it, ignoreCase = true) }
        }

        // Extract name (usually the first or largest text)
        val name = extractName(contentLines, ocrResult.blocks)
        
        // Extract title
        val title = extractTitle(contentLines)
        
        // Extract company
        val company = extractCompany(contentLines, name, title)
        
        // Extract address
        val addresses = extractAddresses(lines)

        // Calculate confidence based on how much information was extracted
        val confidence = calculateConfidence(name, title, company, emails, phones)

        BusinessCardInfo(
            name = name,
            title = title,
            company = company,
            emails = emails,
            phones = phones,
            addresses = addresses,
            websites = websites,
            rawText = text,
            confidence = confidence
        )
    }

    private fun extractEmails(text: String): List<String> {
        val matcher = emailPattern.matcher(text)
        val emails = mutableListOf<String>()
        while (matcher.find()) {
            emails.add(matcher.group())
        }
        return emails.distinct()
    }

    private fun extractPhones(text: String): List<String> {
        val matcher = phonePattern.matcher(text)
        val phones = mutableListOf<String>()
        while (matcher.find()) {
            val phone = matcher.group().trim()
            // Filter out short numbers that might be false positives
            if (phone.replace(Regex("[^0-9]"), "").length >= 7) {
                phones.add(phone)
            }
        }
        return phones.distinct()
    }

    private fun extractWebsites(text: String): List<String> {
        val matcher = urlPattern.matcher(text)
        val websites = mutableListOf<String>()
        while (matcher.find()) {
            val url = matcher.group()
            // Filter out email domains
            if (!url.contains("@")) {
                websites.add(url)
            }
        }
        return websites.distinct()
    }

    private fun extractName(
        lines: List<String>, 
        blocks: List<com.officesuite.app.ocr.TextBlock>
    ): String? {
        if (lines.isEmpty()) return null
        
        // Try to find a line that looks like a name (typically at the top, 2-4 words)
        for (line in lines.take(3)) {
            val words = line.split(Regex("\\s+"))
            // A name typically has 2-4 words and no numbers
            if (words.size in 2..4 && 
                !line.contains(Regex("\\d")) &&
                !titleKeywords.any { line.contains(it, ignoreCase = true) }) {
                return line
            }
        }
        
        // Fallback: use the first line if it looks reasonable
        val firstLine = lines.firstOrNull() ?: return null
        if (firstLine.length in 3..50 && !firstLine.contains(Regex("\\d"))) {
            return firstLine
        }
        
        return null
    }

    private fun extractTitle(lines: List<String>): String? {
        for (line in lines) {
            if (titleKeywords.any { line.contains(it, ignoreCase = true) }) {
                return line
            }
        }
        return null
    }

    private fun extractCompany(lines: List<String>, name: String?, title: String?): String? {
        // Look for lines that might be company names
        // Skip name and title lines
        for (line in lines) {
            if (line == name || line == title) continue
            
            // Company names often contain these words
            val companyIndicators = listOf(
                "Inc", "Corp", "LLC", "Ltd", "Co.", "Company", "Group",
                "Technologies", "Solutions", "Services", "Consulting"
            )
            
            if (companyIndicators.any { line.contains(it, ignoreCase = true) }) {
                return line
            }
        }
        
        // Fallback: take a line that isn't the name or title and looks like a company
        for (line in lines) {
            if (line != name && line != title && 
                line.length in 3..100 &&
                !line.contains("@") &&
                !line.matches(Regex(".*\\d{5,}.*"))) {
                return line
            }
        }
        
        return null
    }

    private fun extractAddresses(lines: List<String>): List<String> {
        val addressPatterns = listOf(
            Regex("\\d+\\s+[A-Za-z]+\\s+(Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Drive|Dr|Lane|Ln|Way|Court|Ct)", RegexOption.IGNORE_CASE),
            Regex("[A-Za-z]+,\\s*[A-Za-z]{2}\\s*\\d{5}(-\\d{4})?") // City, State ZIP
        )
        
        val addresses = mutableListOf<String>()
        
        for (line in lines) {
            if (addressPatterns.any { it.containsMatchIn(line) }) {
                addresses.add(line)
            }
        }
        
        return addresses.distinct()
    }

    private fun calculateConfidence(
        name: String?,
        title: String?,
        company: String?,
        emails: List<String>,
        phones: List<String>
    ): Float {
        var score = 0f
        var total = 5f
        
        if (name != null) score += 1f
        if (title != null) score += 1f
        if (company != null) score += 1f
        if (emails.isNotEmpty()) score += 1f
        if (phones.isNotEmpty()) score += 1f
        
        return score / total
    }
}
