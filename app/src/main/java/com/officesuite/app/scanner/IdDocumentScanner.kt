package com.officesuite.app.scanner

import android.graphics.Bitmap
import com.officesuite.app.ocr.OcrManager
import com.officesuite.app.ocr.OcrLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * ID Document types that can be scanned
 */
enum class IdDocumentType {
    PASSPORT,
    DRIVERS_LICENSE,
    ID_CARD,
    UNKNOWN
}

/**
 * Scanned ID document information
 */
data class IdDocumentInfo(
    val documentType: IdDocumentType,
    val firstName: String?,
    val lastName: String?,
    val fullName: String?,
    val dateOfBirth: Date?,
    val dateOfBirthString: String?,
    val expirationDate: Date?,
    val expirationDateString: String?,
    val documentNumber: String?,
    val nationality: String?,
    val sex: String?,
    val address: String?,
    val rawText: String,
    val confidence: Float
)

/**
 * ID Document Scanner that extracts information from passports, driver's licenses, and ID cards
 */
class IdDocumentScanner(private val ocrManager: OcrManager) {

    // Common date formats found in ID documents
    private val dateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("ddMMyyyy", Locale.US)
    )

    // Patterns for extracting information
    private val datePattern = Pattern.compile(
        "\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}|\\d{1,2}\\s+[A-Za-z]{3,}\\s+\\d{4}"
    )
    
    private val documentNumberPattern = Pattern.compile(
        "[A-Z]{1,3}\\d{6,9}|\\d{9,12}|[A-Z]\\d{7}[A-Z]"
    )

    // Keywords that indicate different document types
    private val passportKeywords = listOf("PASSPORT", "PASSEPORT", "PASSAPORTE", "MRZ")
    private val driverLicenseKeywords = listOf("DRIVER", "LICENSE", "LICENCE", "DL", "DRIVING")
    private val idCardKeywords = listOf("IDENTITY", "ID CARD", "IDENTIFICATION", "NATIONAL ID")

    // Field labels commonly found on IDs
    private val namePrefixes = listOf("NAME", "SURNAME", "GIVEN NAME", "FIRST NAME", "LAST NAME", "NOM", "PRENOM")
    private val dobPrefixes = listOf("DATE OF BIRTH", "DOB", "BIRTH DATE", "BORN", "DN", "DATE NAISSANCE")
    private val expPrefixes = listOf("EXPIRY", "EXPIRES", "EXP", "EXPIRATION", "VALID UNTIL", "DATE EXP")
    private val docNumPrefixes = listOf("DOCUMENT NO", "DOC NO", "ID NO", "LICENSE NO", "PASSPORT NO", "NUMBER")

    /**
     * Scans an ID document and extracts information
     * 
     * @param bitmap The ID document image
     * @return IdDocumentInfo containing extracted information
     */
    suspend fun scan(bitmap: Bitmap): IdDocumentInfo = withContext(Dispatchers.Default) {
        // Use Latin OCR for ID documents
        ocrManager.setLanguage(OcrLanguage.LATIN)
        
        val ocrResult = ocrManager.extractText(bitmap)
        
        if (!ocrResult.success) {
            return@withContext IdDocumentInfo(
                documentType = IdDocumentType.UNKNOWN,
                firstName = null,
                lastName = null,
                fullName = null,
                dateOfBirth = null,
                dateOfBirthString = null,
                expirationDate = null,
                expirationDateString = null,
                documentNumber = null,
                nationality = null,
                sex = null,
                address = null,
                rawText = "",
                confidence = 0f
            )
        }

        val text = ocrResult.fullText
        val upperText = text.uppercase()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        // Determine document type
        val documentType = determineDocumentType(upperText)
        
        // Extract fields
        val fullName = extractName(lines, upperText)
        val (firstName, lastName) = splitName(fullName)
        val dobString = extractDateWithLabel(lines, upperText, dobPrefixes)
        val dob = parseDate(dobString)
        val expString = extractDateWithLabel(lines, upperText, expPrefixes)
        val exp = parseDate(expString)
        val documentNumber = extractDocumentNumber(upperText)
        val nationality = extractNationality(lines, upperText)
        val sex = extractSex(upperText)
        val address = extractAddress(lines)

        // Calculate confidence
        val confidence = calculateConfidence(fullName, dob, documentNumber, documentType)

        IdDocumentInfo(
            documentType = documentType,
            firstName = firstName,
            lastName = lastName,
            fullName = fullName,
            dateOfBirth = dob,
            dateOfBirthString = dobString,
            expirationDate = exp,
            expirationDateString = expString,
            documentNumber = documentNumber,
            nationality = nationality,
            sex = sex,
            address = address,
            rawText = text,
            confidence = confidence
        )
    }

    private fun determineDocumentType(text: String): IdDocumentType {
        return when {
            passportKeywords.any { text.contains(it) } -> IdDocumentType.PASSPORT
            driverLicenseKeywords.any { text.contains(it) } -> IdDocumentType.DRIVERS_LICENSE
            idCardKeywords.any { text.contains(it) } -> IdDocumentType.ID_CARD
            else -> IdDocumentType.UNKNOWN
        }
    }

    private fun extractName(lines: List<String>, upperText: String): String? {
        // Look for name after common labels
        for (line in lines) {
            val upperLine = line.uppercase()
            for (prefix in namePrefixes) {
                if (upperLine.contains(prefix)) {
                    // Try to extract the value after the label
                    val colonIndex = line.indexOf(':')
                    if (colonIndex != -1 && colonIndex < line.length - 1) {
                        return line.substring(colonIndex + 1).trim()
                    }
                    // Or the text after the prefix
                    val prefixIndex = upperLine.indexOf(prefix)
                    if (prefixIndex != -1) {
                        val afterPrefix = line.substring(prefixIndex + prefix.length).trim()
                        if (afterPrefix.isNotBlank() && !afterPrefix.startsWith(":")) {
                            return afterPrefix.trimStart(':').trim()
                        }
                    }
                }
            }
        }
        
        // Fallback: look for a line that looks like a name (all caps, 2-4 words, no numbers)
        for (line in lines) {
            val words = line.split(Regex("\\s+")).filter { it.length > 1 }
            if (words.size in 2..4 && 
                !line.contains(Regex("\\d")) &&
                line.all { it.isLetter() || it.isWhitespace() || it == '-' }) {
                return line
            }
        }
        
        return null
    }

    private fun splitName(fullName: String?): Pair<String?, String?> {
        if (fullName == null) return Pair(null, null)
        
        val parts = fullName.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> Pair(parts.first(), parts.drop(1).joinToString(" "))
            parts.size == 1 -> Pair(parts.first(), null)
            else -> Pair(null, null)
        }
    }

    private fun extractDateWithLabel(lines: List<String>, upperText: String, prefixes: List<String>): String? {
        for (line in lines) {
            val upperLine = line.uppercase()
            for (prefix in prefixes) {
                if (upperLine.contains(prefix)) {
                    // Find date in this line or nearby
                    val matcher = datePattern.matcher(line)
                    if (matcher.find()) {
                        return matcher.group()
                    }
                }
            }
        }
        
        // Fallback: extract all dates and return based on position
        val allDates = mutableListOf<String>()
        val matcher = datePattern.matcher(upperText)
        while (matcher.find()) {
            allDates.add(matcher.group())
        }
        
        return allDates.firstOrNull()
    }

    private fun parseDate(dateString: String?): Date? {
        if (dateString == null) return null
        
        for (format in dateFormats) {
            try {
                format.isLenient = false
                return format.parse(dateString)
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun extractDocumentNumber(text: String): String? {
        val matcher = documentNumberPattern.matcher(text)
        while (matcher.find()) {
            val match = matcher.group()
            // Validate it's not a date
            if (!match.contains("/") && !match.contains("-") && match.length >= 6) {
                return match
            }
        }
        return null
    }

    private fun extractNationality(lines: List<String>, upperText: String): String? {
        val nationalityPrefixes = listOf("NATIONALITY", "CITIZEN", "COUNTRY")
        
        for (line in lines) {
            val upperLine = line.uppercase()
            for (prefix in nationalityPrefixes) {
                if (upperLine.contains(prefix)) {
                    val colonIndex = line.indexOf(':')
                    if (colonIndex != -1 && colonIndex < line.length - 1) {
                        return line.substring(colonIndex + 1).trim()
                    }
                }
            }
        }
        
        // Look for 3-letter country codes
        val countryCodePattern = Pattern.compile("\\b[A-Z]{3}\\b")
        val matcher = countryCodePattern.matcher(upperText)
        while (matcher.find()) {
            val code = matcher.group()
            if (isValidCountryCode(code)) {
                return code
            }
        }
        
        return null
    }

    private fun isValidCountryCode(code: String): Boolean {
        val commonCodes = setOf("USA", "GBR", "CAN", "AUS", "DEU", "FRA", "ITA", "ESP", "JPN", "CHN", "IND", "BRA", "MEX")
        return commonCodes.contains(code)
    }

    private fun extractSex(text: String): String? {
        return when {
            text.contains(Regex("\\bM\\b|\\bMALE\\b")) && !text.contains(Regex("\\bFEMALE\\b")) -> "M"
            text.contains(Regex("\\bF\\b|\\bFEMALE\\b")) -> "F"
            else -> null
        }
    }

    private fun extractAddress(lines: List<String>): String? {
        val addressPrefixes = listOf("ADDRESS", "RESIDENCE", "ADDR")
        val addressPatterns = listOf(
            Regex("\\d+\\s+[A-Za-z]+\\s+(Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd)", RegexOption.IGNORE_CASE),
            Regex("[A-Za-z]+,\\s*[A-Za-z]{2}\\s*\\d{5}")
        )
        
        // Look for address after label
        for (i in lines.indices) {
            val line = lines[i]
            if (addressPrefixes.any { line.uppercase().contains(it) }) {
                val colonIndex = line.indexOf(':')
                if (colonIndex != -1 && colonIndex < line.length - 1) {
                    return line.substring(colonIndex + 1).trim()
                }
                // Check next line
                if (i + 1 < lines.size) {
                    return lines[i + 1]
                }
            }
        }
        
        // Look for lines matching address patterns
        for (line in lines) {
            if (addressPatterns.any { it.containsMatchIn(line) }) {
                return line
            }
        }
        
        return null
    }

    private fun calculateConfidence(
        name: String?,
        dob: Date?,
        documentNumber: String?,
        documentType: IdDocumentType
    ): Float {
        var score = 0f
        
        if (name != null) score += 0.3f
        if (dob != null) score += 0.25f
        if (documentNumber != null) score += 0.25f
        if (documentType != IdDocumentType.UNKNOWN) score += 0.2f
        
        return score
    }
}
